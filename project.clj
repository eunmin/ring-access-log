(defproject ring-access-log "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clj-time "0.14.4"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.2"]
                                  [metosin/ring-http-response "0.9.0"]]}})
