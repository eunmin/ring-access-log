(ns ring.middleware.access-log.parser
  (:require [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.local :as l]))

(let [fmt (f/formatter "d/M/Y:H:M:S Z")]
  (f/unparse fmt (l/local-now)))

(def patterns {;; Remote IP address
               \a (fn [req _ _]
                    (get req :remote-addr "-"))
               ;; Local IP address
               \A (constantly nil)
               ;; Bytes sent, excluding HTTP headers, or '-' if zero
               \b (constantly nil)
               ;; Bytes sent, excluding HTTP headers
               \B (constantly nil)
               ;; Remote host name (or IP address if enableLookups for the connector is false)
               \h (constantly nil)
               ;; Request protocol
               \H (fn [req _ _]
                    (get req :protocol "-"))
               ;; Remote logical username from identd (always returns '-')
               \l (constantly nil)
               ;; Request method
               \m (fn [req _ _]
                    (get req :request-method "-"))
               ;; Local port on which this request was received. See also %{xxx}p below.
               \p (fn [req _ _]
                    (get req :server-port "-"))
               ;; Query string (prepended with a '?' if it exists)
               \q (fn [req _ _ ]
                    (get req :query-string "-"))
               ;; First line of the request (method and request URI)
               \r (constantly nil)
               ;; HTTP status code of the response
               \s (fn [_ resp _]
                    (get resp :status "-"))
               ;; Date and time, in Common Log Format
               \t (fn [_ _ _]
                    (when-let [dt (l/to-local-date-time (l/local-now))]
                      (f/unparse (f/formatter "dd/MM/Y:HH:MM:SS Z") dt)))
               ;; Remote user that was authenticated (if any), else '-'
               \u (constantly nil)
               ;; Requested URL path
               \U (fn [_ resp _]
                    (get resp :uri "-"))
               ;; Local server name
               \v (constantly nil)
               ;; Time taken to process the request, in millis
               \D (constantly nil)
               ;; Time taken to process the request, in seconds
               \T (constantly nil)
               ;; Current request thread name (can compare later with stacktraces)
               \I (constantly nil)
               ;; write value of incoming header with name xxx
               \i (fn [req _ param]
                    (get-in req [:headers param] "-"))
               ;; write value of outgoing header with name xxx
               \o (fn [_ resp param]
                    (get-in resp [:headers param] "-"))
               ;; write value of cookie with name xxx
               \c (constantly nil)
               ;; \r [] ;; write value of ServletRequest attribute with name xxx
               ;; \s [] ;; write value of HttpSession attribute with name xxx
               })

(defn update-last [v f]
  (let [last-idx (dec (count v))]
    (if (neg? last-idx)
      v
      (update v last-idx #(f %)))))

(defn pattern-id? [c]
  ((set (keys patterns)) c))

(defn parse [pattern]
  (loop [pattern (seq pattern)
         format-pattern ""
         tags []]
    (if (seq pattern)
      (let [[c1 c2] pattern]
        (if (= c1 \%)
          (cond
            (= c2 \{)
            (let [close-pos (.indexOf pattern \})
                  pattern-id (nth pattern (inc close-pos))]
              (if (neg? close-pos)
                (throw (ex-info "Invalid format" {:pattern pattern :pos (str c1 c2)}))
                (if (pattern-id? pattern-id)
                  (recur (nthrest pattern (+ 2 close-pos))
                         (str format-pattern "%s")
                         (conj tags {:id pattern-id
                                     :param (apply str (subvec (vec pattern) 2 close-pos))}))
                  (throw (ex-info "Invalid format" {:pattern pattern :pos (str c1 c2)})))))

            (pattern-id? c2)
            (recur (nthrest pattern 2)
                   (str format-pattern "%s")
                   (conj tags {:id c2}))

            (= c2 \%)
            (recur (nthrest pattern 2)
                   (str format-pattern "%%")
                   tags)

            :else (throw (ex-info "Invalid format" {:pattern pattern :pos (str c1 c2)})))
          (recur (rest pattern) (str format-pattern c1) tags)))
      [format-pattern tags])))

(defn formatter [[format-pattern tags]]
  (fn [request response options]
    (let [kss (map #(assoc % :resolver (get patterns (:id %))) tags)]
      (apply format
             format-pattern
             (map (fn [{:keys [resolver param]}]
                    (resolver request response param))
                  kss)))))
