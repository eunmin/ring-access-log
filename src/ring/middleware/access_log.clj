(ns ring.middleware.access-log
  (:require [clojure.tools.logging :as log]
            [ring.middleware.access-log.encoder :as encoder]
            [ring.middleware.access-log.resolver :refer :all]))

(def common "%h %l %u %t \"%r\" %s %b")

(def combined "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\"")

(def pattern-resolver-map {\a remote-ip-address
                           \b bytes-sent
                           \h remote-host-name
                           \H request-protocol
                           \l remote-logical-username
                           \m request-method
                           \p local-port
                           \q query-string
                           \r request-first-line
                           \s response-status-code
                           \t date-and-time
                           \u remote-user
                           \U request-url-path
                           \D ptime-millis
                           \T ptime-sec
                           \I current-thread-name
                           \i incoming-header
                           \o outgoing-header
                           \c cookie-value})

(defn log [request response {:keys [encoder]}]
  (log/info (encoder {:request request :response response})))

(defn wrap-access-log
  ([handler]
   (wrap-access-log handler {}))
  ([handler {:keys [pattern resolvers]
             :or {pattern combined}
             :as options}]
   (let [resolvers (merge pattern-resolver-map resolvers)
         encoder (encoder/create pattern resolvers)]
     (fn [request]
       (let [response (handler request)]
         (log request response (assoc options :encoder encoder))
         response)))))
