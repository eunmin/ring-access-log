(ns ring.middleware.access-log.resolver-test
  (:require [ring.middleware.access-log.resolver :refer :all]
            [clojure.test :refer :all]))

(deftest bytes-sent-test
  (is (= "-" (bytes-sent nil nil)))
  (is (= "-" (bytes-sent {:response {:body ""}} nil)))
  (is (= "3" (bytes-sent {:response {:body "abc"}} nil))))
