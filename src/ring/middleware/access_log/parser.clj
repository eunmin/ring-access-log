(ns ring.middleware.access-log.parser
  (:require [clojure.string :refer [index-of]]))

(def patterns {"a" [:request :remote-addr]
               "m" [:request :request-method]
               "H" [:request :protocol]})

(defn start-tag? [c]
  (= c \%))

(defn end-tag? [c]
  (= c \space))

(defn tag->ks [tag]
  (get patterns tag))

(defn tags->kss [tags]
  (map tag->ks tags))

(defn update-last [v f]
  (let [last-idx (dec (count v))]
    (if (neg? last-idx)
      v
      (update v last-idx #(f %)))))

(defn format-string? [c]
  (#{\a \m \H} c))

(defn parse [pattern]
  (loop [pattern pattern
         format-pattern ""
         tags []]
    (if (seq pattern)
      (let [[c1 c2] pattern]
        (cond
          (and (= c1 \%) (= c2 \{))
          (let [close-pos (index-of patterns "}")
                format-string (get patterns (inc close-pos))]
            (if (format-string? format-string)
              (recur (nthrest pattern (+ 2 close-pos)) (str format-pattern "%s") (conj tags [(subs pattern 1 close-pos) (str format-string)]))))

          (and (= c1 \%) (format-string? c2))
          (recur (nthrest pattern 2) (str format-pattern "%s") (conj tags (str c2)))

          :else
          (recur (rest pattern) (str format-pattern c1) tags)))
      [format-pattern tags])))

(defn formatter [{:keys [format-string kss]}]
  (fn [request response options]
    (let [m {:request request
             :response response}]
      (apply format format-string (map #(get-in m % "-") kss)))))
