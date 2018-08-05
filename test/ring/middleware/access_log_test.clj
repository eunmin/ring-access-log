(ns ring.middleware.access-log-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging.impl :as impl]
            [ring.middleware.access-log :refer :all]
            [ring.util.http-response :refer :all]
            [ring.mock.request :as mock]))

;;
;; https://github.com/clojure/tools.logging/blob/master/src/test/clojure/clojure/tools/test_logging.clj
;;

(defn test-factory [enabled-set entries-atom]
  (let [main-thread (Thread/currentThread)]
    (reify impl/LoggerFactory
      (name [_] "test factory")
      (get-logger [_ log-ns]
        (reify impl/Logger
          (enabled? [_ level]
            (contains? enabled-set level))
          (write! [_ _ _ msg]
            (reset! entries-atom msg)))))))

(defmacro with-test-logging [[enabled-level-set log-entry-sym] & body]
  (let [enabled-level-set (or enabled-level-set #{:trace :debug :info :warn :error :fatal})
        log-entry-sym (or log-entry-sym 'log-entry-sym)]
    `(let [~log-entry-sym (atom nil)]
       (binding [*logger-factory* (test-factory ~enabled-level-set ~log-entry-sym)]
         ~@body))))

(deftest wrap-access-log-remote-ip-address-test
  (let [handler (wrap-access-log (fn [_] (ok))
                                 {:format "%a"})
        request (mock/request :get "/")]
    (with-test-logging [#{:info} log-entry]
      (handler request)
      (is (= "localhost" @log-entry)))))

(deftest wrap-access-log-request-method-test
  (let [handler (wrap-access-log (fn [_] (ok))
                                 {:format "%m"})
        request (mock/request :get "/")]
    (with-test-logging [#{:info} log-entry]
      (handler request)
      (is (= ":get" @log-entry)))))
