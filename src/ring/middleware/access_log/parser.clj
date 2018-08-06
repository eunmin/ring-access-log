(ns ring.middleware.access-log.parser)

(def patterns {"a" [:request :remote-addr]
               "m" [:request :request-method]
               "H" [:request :protocol]})

(defn start-tag? [c]
  (= c \%))

(defn end-tag? [c]
  (= c \space))

(defn start-tag [r]
  (update r :tag-started? (constantly true)))

(defn end-tag [r]
  (update r :tag-started? (constantly false)))

(defn add-character [r c]
  (update r :result #(str % c)))

(defn tag->ks [tag]
  (get patterns tag))

(defn tags->kss [tags]
  (map tag->ks tags))

(defn append-to-tag-name [r c]
  (let [last-idx (dec (count (:tags r)))]
    (if (neg? last-idx)
      (update r :tags (fn [_] (vector (str c))))
      (update-in r [:tags last-idx] #(str % c)))))

(defn init-tag [r]
  (update r :tags #(conj % "")))

(defn mark-tag-pos [r]
  (update r :result #(str % \% \s)))

(defn parse [pattern]
  (-> (reduce (fn [{:keys [tag-started?] :as r} c]
                (cond
                  (start-tag? c) (-> r init-tag start-tag)
                  (and tag-started? (end-tag? c)) (-> r mark-tag-pos (add-character c) end-tag)
                  tag-started? (append-to-tag-name r c)
                  :else (add-character r c)))
              {:tag-started? false
               :format-string ""
               :tags []}
              pattern)
      (update :tags tags->kss)))

(defn formatter [{:keys [format-string kss]}]
  (fn [request response options]
    (let [m {:request request
             :response response}]
      (apply format format-string (map #(get-in m % "-") kss)))))
