(ns ring.middleware.access-log.encoder)

(defn update-last [v f]
  (let [last-idx (dec (count v))]
    (if (neg? last-idx)
      v
      (update v last-idx #(f %)))))

(defn pattern-id? [c resolvers]
  ((set (keys resolvers)) c))

(defn parse [pattern resolvers]
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
                (if (pattern-id? pattern-id resolvers)
                  (recur (nthrest pattern (+ 2 close-pos))
                         (str format-pattern "%s")
                         (conj tags {:id pattern-id
                                     :param (apply str (subvec (vec pattern) 2 close-pos))}))
                  (throw (ex-info "Invalid format" {:pattern pattern :pos (str c1 c2)})))))

            (pattern-id? c2 resolvers)
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

(defn formatter [[format-pattern tags] resolvers]
  (fn [request response options]
    (let [kss (map #(assoc % :resolver (get resolvers (:id %))) tags)]
      (apply format
             format-pattern
             (map (fn [{:keys [resolver param]}]
                    (or (resolver request response param) "-"))
                  kss)))))

(defn create [pattern resolvers]
  (formatter (parse pattern resolvers) resolvers))
