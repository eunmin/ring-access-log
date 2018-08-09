(ns ring.middleware.access-log.encoder-test
  (:require [clojure.test :refer :all]
            [ring.middleware.access-log.encoder :refer :all]))

(defn test-resolver [data pattern-param]
  )

(deftest parser-single-pattern-test
  (let [[format-pattern [tag]] (parse "%a" {\a test-resolver})]
    (is (= "%s" format-pattern))
    (is (= \a (:id tag)))))

(deftest parse-with-string-test
  (testing ""
    (let [[format-pattern [tag]] (parse "---%a" {\a test-resolver})]
      (is (= "---%s" format-pattern))
      (is (= \a (:id tag)))))
  (testing ""
    (let [[format-pattern [tag]] (parse "%a---" {\a test-resolver})]
      (is (= "%s---" format-pattern))
      (is (= \a (:id tag))))))

(deftest parse-with-no-pattern-test
  (let [[format-pattern [tag]] (parse "---" {})]
    (is (= "---" format-pattern))
    (is (nil? tag))))

(deftest parse-with-param-test
  (testing ""
    (let [[format-pattern [tag]] (parse "%{Content-Type}i" {\i test-resolver})]
      (is (= "%s" format-pattern))
      (is (= \i (:id tag)))
      (is (= "Content-Type" (:pattern-param tag)))))

  (testing ""
    (let [[format-pattern [tag]] (parse "- %{Content-Type}i -" {\i test-resolver})]
      (is (= "- %s -" format-pattern))
      (is (= \i (:id tag)))
      (is (= "Content-Type" (:pattern-param tag))))))

(deftest parse-multi-pattern-test
  (testing ""
    (let [[format-pattern [tag1 tag2]] (parse "%a %m" {\a test-resolver \m test-resolver})]
      (is (= "%s %s" format-pattern))
      (is (= \a (:id tag1)))
      (is (= \m (:id tag2)))))
  (testing ""
    (let [[format-pattern [tag1 tag2]] (parse "%a%m" {\a test-resolver \m test-resolver})]
      (is (= "%s%s" format-pattern))
      (is (= \a (:id tag1)))
      (is (= \m (:id tag2)))))
  (testing ""
    (let [[format-pattern [tag1 tag2]] (parse "- %a - %m -" {\a test-resolver \m test-resolver})]
      (is (= "- %s - %s -" format-pattern))
      (is (= \a (:id tag1)))
      (is (= \m (:id tag2))))))

(deftest parse-multi-param-pattern-test
  (let [[format-pattern [tag1 tag2]] (parse "- %{Content-Type}i - %{Host}i -"
                                            {\i test-resolver})]
    (is (= "- %s - %s -" format-pattern))
    (is (= \i (:id tag1)))
    (is (= "Content-Type" (:pattern-param tag1)))
    (is (= \i (:id tag2)))
    (is (= "Host" (:pattern-param tag2)))))

(deftest parse-escape-percent-test
  (let [[format-pattern [tag]] (parse "%%%a%%" {\a test-resolver})]
    (is (= "%%%s%%" format-pattern))
    (is (= \a (:id tag)))))

(deftest invalid-pattern-test
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%" {}))))
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%{" {}))))
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%x" {})))))
