(ns ring.middleware.access-log
  (:require [clojure.tools.logging :as log]
            [ring.middleware.access-log.parser :as parser]))

(defn log [request response {:keys [formatter] :as options}]
  (log/info (formatter request response options)))

(defn wrap-access-log
  ([handler]
   (wrap-access-log handler {}))
  ([handler {:keys [pattern] :as options}]
   (let [formatter (parser/formatter (parse pattern))]
     (fn
       ([request]
        (let [response (handler request)]
          (log request response (assoc options :formatter formatter))
          response))
       ([request respond raise]
        (handler request respond raise))))))
