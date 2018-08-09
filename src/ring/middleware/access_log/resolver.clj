(ns ring.middleware.access-log.resolver
  (:require [clojure.string :refer [upper-case lower-case]]
            [clj-time.format :refer [with-locale with-zone] :as f]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.local :as l])
  (:import java.util.Locale))

(defn remote-ip-address [{:keys [request]} _]
  (get request :remote-addr "-"))

(defn bytes-sent [{:keys [response]} _]
  (if (string? (:body response))
    (let [cnt (count (:body response))]
      (if (pos? cnt)
        (str cnt)
        "-"))
    "-"))

(defn remote-host-name [{:keys [request]} _]
  (get request :remote-addr "-"))

(defn request-protocol [{:keys [request]} _]
  (:protocol request))

(defn remote-logical-username [_ _]
  "-")

(defn request-method [{:keys [request]} _]
  (upper-case (name (:request-method request))))

(defn local-port [{:keys [request]} _]
  (:server-port request))

(defn query-string [{:keys [request]} _]
  (:query-string request))

(defn request-first-line [{{:keys [request-method uri query-string protocol]} :request} _]
  (format "%s %s%s %s"
          (upper-case (name request-method))
          uri
          (if query-string (str "?" query-string) "")
          protocol))

(defn response-status-code [{:keys [response]} _]
  (:status response))

(defn date-and-time [_ pattern-param]
  (let [fmtr (-> (or pattern-param "dd/MMM/Y:HH:MM:SS Z")
                 f/formatter
                 (with-locale Locale/US)
                 (with-zone (t/default-time-zone)))]
    (format "[%s]" (f/unparse fmtr (l/local-now)))))

(defn remote-user [_ _]
  "-")

(defn request-url-path [{:keys [request]} _]
  (:uri request))

(defn ptime-millis [{:keys [request]} _]
  (when-let [request-start-time (-> request :attrs :request-start-time)]
    (- (System/currentTimeMillis) request-start-time)))

(defn ptime-sec [{:keys [request]} _]
  (int (/ (ptime-millis request nil nil) 1000)))

(defn current-thread-name [_ _]
  (.getName (Thread/currentThread)))

(defn incoming-header [{:keys [request]} pattern-param]
  (get-in request [:headers (lower-case pattern-param)]))

(defn outgoing-header [{:keys [response]} pattern-param]
  (get-in response [:headers (lower-case pattern-param)]))

(defn cookie-value [{:keys [request]} pattern-param]
  (get-in request [:cookies pattern-param :value]))
