(ns restful-clojure.handler
  (:use compojure.core
        ring.middleware.json
        ring.middleware.params
        ring.middleware.keyword-params
        clojure.walk)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]))

(defn- put-credit [account-number body]
  (prn "put-credit says hello")
  "put-credit says hello")

(defn- put-debit [account-number body]
  (prn "put-debit says hello")
  "put-debit says hello")

(defn- get-balance [account-number]
  (prn "get-balance says hello")
  "get-balance says hello")

(defn- get-statements [account-number params]
  (prn "get-statements says hello")
  "get-statements says hello")

(defn- get-debts [account-number]
  (prn "get-debts says hello")
  "get-debts says hello")

(defroutes app-routes
  (context "/:account" [account]
    (PUT "/credit" {body :body} (response (put-credit account body)))
    (PUT "/debit" {body :body} (response (put-debit account body)))
    (GET "/balance" [] (response (get-balance account)))
    (GET "/statements" {params :query-params} (response (get-statements account (keywordize-keys params))))
    (GET "/debts" [] (response (get-debts account))))
  (route/not-found (response {:message "Page not found", :status 404})))

(defn wrap-log-request [handler]
  (fn [req] ; return handler function
    (println req) ; perform logging
    (handler req))) ; pass the request through to the inner handler

(def app
  (-> app-routes
    (wrap-log-request)
    (wrap-json-body {:keywords? true})
    (wrap-params)
    (wrap-json-response)))
