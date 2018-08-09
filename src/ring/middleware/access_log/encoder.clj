(ns ring.middleware.access-log.encoder)

(defn update-last [v f]
  (let [last-idx (dec (count v))]
    (if (neg? last-idx)
      v
      (update v last-idx #(f %)))))

(defn pattern-id? [c resolvers]
  ((set (keys resolvers)) c))

(defn map-get [m k]
  (if-let [v (get m k)]
    v
    (throw (ex-info (str "Not found key:" k) m))))

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
                (throw (ex-info "Invalid format (not closed pattern param)" {:pattern pattern :pos (str c1 c2)}))
                (if (pattern-id? pattern-id resolvers)
                  (recur (nthrest pattern (+ 2 close-pos))
                         (str format-pattern "%s")
                         (conj tags {:id pattern-id
                                     :pattern-param (apply str (subvec (vec pattern) 2 close-pos))
                                     :resolver (map-get resolvers pattern-id)}))
                  (throw (ex-info "Invalid format" {:pattern pattern :pos (str c1 c2)})))))

            (pattern-id? c2 resolvers)
            (recur (nthrest pattern 2)
                   (str format-pattern "%s")
                   (conj tags {:id c2
                               :resolver (map-get resolvers c2)}))

            (= c2 \%)
            (recur (nthrest pattern 2)
                   (str format-pattern "%%")
                   tags)

            :else (throw (ex-info "Invalid format (missing pattern id)" {:pattern pattern :pos (str c1 c2)})))
          (recur (rest pattern) (str format-pattern c1) tags)))
      [format-pattern tags])))

(defn create [pattern resolvers]
  (let [[format-pattern tags] (parse pattern resolvers)]
    (println format-pattern tags)
    (fn apply-encoder [data]
      (apply format
             format-pattern
             (map (fn [{:keys [resolver pattern-param]}]
                    (or (str (resolver data pattern-param)) "-"))
                  tags)))))
