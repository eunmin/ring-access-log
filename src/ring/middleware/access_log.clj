(ns ring.middleware.access-log
  (:require [clojure.tools.logging :as log]
            [ring.middleware.access-log.parser :as parser]))

(def common "%h %l %u %t \"%r\" %s %b")

(def combined  "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\"")

(defn log [request response {:keys [formatter] :as options}]
  (log/info (formatter request response options)))

(defn wrap-access-log
  ([handler]
   (wrap-access-log handler {}))
  ([handler {:keys [pattern] :as options}]
   (let [formatter (parser/formatter (parser/parse pattern))]
     (fn
       ([request]
        (let [response (handler request)]
          (log request response (assoc options :formatter formatter))
          response))
       ([request respond raise]
        (handler request respond raise))))))
