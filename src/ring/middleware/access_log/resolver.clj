(ns ring.middleware.access-log.resolver
  (:require [clojure.string :refer [upper-case lower-case]]
            [clj-time.format :refer [with-locale with-zone] :as f]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.local :as l])
  (:import java.util.Locale))

(defn remote-ip-address [req _ _]
  (get req :remote-addr "-"))

(defn bytes-sent [_ resp _]
  (if (string? (:body resp))
    (let [cnt (count (:body resp))]
      (if (pos? cnt)
        (str cnt)
        "-"))
    "-"))

(defn remote-host-name [req _ _]
  (get req :remote-addr "-"))

(defn request-protocol [req _ _]
  (:protocol req))

(defn remote-logical-username [_ _ _]
  "-")

(defn request-method [req _ _]
  (upper-case (name (:request-method req))))

(defn local-port [req _ _]
  (:server-port req))

(defn query-string [req _ _]
  (:query-string req))

(defn request-first-line [{:keys [request-method uri query-string protocol]} _ _]
  (format "%s %s%s %s"
          (upper-case (name request-method))
          uri
          (if query-string (str "?" query-string) "")
          protocol))

(defn response-status-code [_ resp _]
  (:status resp))

(defn date-and-time [_ _ param]
  (let [fmtr (-> (or param "dd/MMM/Y:HH:MM:SS Z")
                 f/formatter
                 (with-locale Locale/US)
                 (with-zone (t/default-time-zone)))]
    (format "[%s]" (f/unparse fmtr (l/local-now)))))

(defn remote-user [_ _ _]
  "-")

(defn request-url-path [req _ _]
  (:uri req))

(defn ptime-millis [req _ _]
  (when-let [request-start-time (-> req :attrs :request-start-time)]
    (- (System/currentTimeMillis) request-start-time)))

(defn ptime-sec [req _ _]
  (int (/ (ptime-millis req nil nil) 1000)))

(defn current-thread-name [_ _ _]
  (.getName (Thread/currentThread)))

(defn incoming-header [req _ param]
  (get-in req [:headers (lower-case param)]))

(defn outgoing-header [_ resp param]
  (get-in resp [:headers (lower-case param)]))

(defn cookie-value [req _ param]
  (get-in req [:cookies param :value]))
