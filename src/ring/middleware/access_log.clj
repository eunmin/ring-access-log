(ns ring.middleware.access-log
  (:require [clojure.tools.logging :as log]))

(def patterns {"%a" :remote-addr
               "%m" :request-method})

(defn log [request response {log-format :format}]
  (log/info ((get patterns log-format) request)))

(defn wrap-access-log
  ([handler]
   (wrap-access-log handler {}))
  ([handler options]
   (fn
     ([request]
      (let [response (handler request)]
        (log request response options)
        response))
     ([request respond raise]
      (handler request respond raise)))))
