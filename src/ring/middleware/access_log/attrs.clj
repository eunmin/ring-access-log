(ns ring.middleware.access-log.attrs)

(defn wrap-record-request-start-time
  "Record request start time to `:request-start-time` in `:attrs`."
  [handler]
  (fn [request]
    (handler (assoc-in request [:attrs :request-start-time] (System/currentTimeMillis)))))
