(ns ring.middleware.access-log.parser-test
  (:require [ring.middleware.access-log.parser :refer :all]
            [clojure.test :refer :all]))

(deftest parser-single-pattern-test
  (let [[format-pattern [tag]] (parse "%a")]
    (is (= "%s" format-pattern))
    (is (= {:id \a} tag))))

(deftest parse-with-string-test
  (testing ""
    (let [[format-pattern [tag]] (parse "---%a")]
      (is (= "---%s" format-pattern))
      (is (= {:id \a} tag))))
  (testing ""
    (let [[format-pattern [tag]] (parse "%a---")]
      (is (= "%s---" format-pattern))
      (is (= {:id \a} tag)))))

(deftest parse-with-no-pattern-test
  (let [[format-pattern [tag]] (parse "---")]
    (is (= "---" format-pattern))
    (is (nil? tag))))

(deftest parse-with-param-test
  (testing ""
    (let [[format-pattern tags] (parse "%{Content-Type}i")]
      (is (= "%s" format-pattern))
      (is (= [{:id \i :param "Content-Type"}] tags))))
  (testing ""
    (let [[format-pattern tags] (parse "- %{Content-Type}i -")]
      (is (= "- %s -" format-pattern))
      (is (= [{:id \i :param "Content-Type"}] tags)))))

(deftest parse-multi-pattern-test
  (testing ""
    (let [[format-pattern tags] (parse "%a %m")]
      (is (= "%s %s" format-pattern))
      (is (= [{:id \a} {:id \m}] tags))))
  (testing ""
    (let [[format-pattern tags] (parse "%a%m")]
      (is (= "%s%s" format-pattern))
      (is (= [{:id \a} {:id \m}] tags))))
  (testing ""
    (let [[format-pattern tags] (parse "- %a - %m -")]
      (is (= "- %s - %s -" format-pattern))
      (is (= [{:id \a} {:id \m}] tags)))))

(deftest parse-multi-param-pattern-test
  (testing ""
    (let [[format-pattern tags] (parse "%a %m")]
      (is (= "%s %s" format-pattern))
      (is (= [{:id \a} {:id \m}] tags))))
  (testing ""
    (let [[format-pattern tags] (parse "- %{Content-Type}i - %{Host}i -")]
      (is (= "- %s - %s -" format-pattern))
      (is (= [{:id \i :param "Content-Type"}
              {:id \i :param "Host"}]
             tags)))))

(deftest parse-escape-percent-test
  (let [[format-pattern tags] (parse "%%%a%%")]
    (is (= "%%%s%%" format-pattern))
    (is (= [{:id \a}] tags))))

(deftest invalid-pattern-test
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%"))))
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%{"))))
  (testing ""
    (is (thrown-with-msg? Exception #"Invalid format" (parse "%x")))))

(deftest formtter-test
  (testing ""
    (let [log ((formatter (parse "%a"))
               {:remote-addr "localhost"} {} {})]
      (is (= "localhost" log))))
  (testing ""
    (let [log ((formatter (parse "%a - %s"))
               {:remote-addr "localhost"} {:status 200} {})]
      (is (= "localhost - 200" log)))))

(deftest formtter-with-param-test
  (let [log ((formatter (parse "%{Content-Type}i"))
             {:headers {"content-type" "text/html"}} {} {})]
    (is (= "text/html" log))))
