(defproject restful-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/bruno-kenji/checking-account"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.1"]
                 [compojure "1.5.2"]
                 [ring/ring-jetty-adapter "1.5.1"]
                 [ring/ring-json "0.4.0"]
                 [clj-time "0.13.0"]
                 [midje "1.6.0" :exclusions [org.clojure/clojure]]
                 [cheshire "5.7.0"]]
  :plugins [[lein-ring "0.11.0"]
            [lein-midje "3.2.1"]]
  :ring {:handler checking-account.handler/app
         :nrepl {:start? true
                 :port 9998}}
  :profiles
    {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                          [ring-mock "0.1.5"]]}})
