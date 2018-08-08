(ns ring.middleware.access-log.parser
  (:require [clojure.string :refer [upper-case]]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.local :as l]
            [ring.middleware.access-log.resolver :refer :all])
  (:import [java.net InetSocketAddress]))

(def patterns {\a remote-ip-address
               \b bytes-sent
               \h remote-host-name
               \H request-protocol
               \l remote-logical-username
               \m request-method
               \p local-port
               \q query-string
               \r request-first-line
               \s response-status-code
               \t date-and-time
               \u remote-user
               \U request-url-path
               \D ptime-millis
               \T ptime-sec
               \I current-thread-name
               \i incoming-header
               \o outgoing-header
               \c cookie-value})

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
                    (or (resolver request response param) "-"))
                  kss)))))
