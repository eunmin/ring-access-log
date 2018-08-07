(ns ring.middleware.access-log.parser
  (:require [clojure.string :refer [index-of]]))

(def patterns {\a [:request :remote-addr]
               \m [:request :request-method]
               \H [:request :protocol]
               \i [:request :headers]
               \o [:response :headers]
               \s [:response :status]})

(defn tag->ks [tag]
  (get patterns tag))

(defn tags->kss [tags]
  (map tag->ks tags))

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
    (let [kss (map (fn [{:keys [id param]}]
                     (let [ks (get patterns id)]
                       (if param
                         (conj ks param)
                         ks)))
                   tags)]
      (apply format
             format-pattern
             (map #(get-in {:request request :response response} % "-") kss)))))
