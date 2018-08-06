(ns ring.middleware.access-log.parser-test
  (:require [ring.middleware.access-log.parser :refer :all]
            [clojure.test :refer :all]))

(deftest parser-test
  (testing ""
    (let [{:keys [format-string tags] :as r} (parse "%a")]
      (is (= "%s" format-string)))))
