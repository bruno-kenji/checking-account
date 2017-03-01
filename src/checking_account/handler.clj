(ns checking-account.handler
  (:use compojure.core
        ring.middleware.json
        ring.middleware.params
        ring.middleware.keyword-params
        clojure.walk
        helpers
        checking-account.utils)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.coerce :as clj-time-coerce]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [checking-account.mock :refer [accounts]]))

(defn- post-accounts [body]
  {:data (make-account body)})

(defn- get-accounts []
  {:data @accounts})

(defn- post-credits [account-number body]
  (let [amount (get body :amount)
        date (get-date (get body :date))
        type (get body :type)
        description (generate-description type amount date)
        operation-id (generate-operation-id (get-account account-number))
        params {:amount amount,
                :date date,
                :description description,
                :operation-id operation-id}]
    (if (new-operation account-number params)
      {:data {:description description,
              :amount amount,
              :account-number account-number,
              :date date,
              :id operation-id}}
      {:error {:message "Unable to credit amount.", :code 500}})))

(defn- post-debits [account-number body]
  (let [amount (negativate-number (get body :amount))
        date (get-date (get body :date))
        type (get body :type)
        other-party (get body :other-party)
        description (generate-description type (get body :amount) date other-party)
        operation-id (generate-operation-id (get-account account-number))
        params {:amount amount,
                :date date,
                :description description,
                :operation-id operation-id}]
    (if (new-operation account-number params)
      {:data {:description description,
              :amount amount,
              :account-number account-number,
              :date date,
              :id operation-id}}
      {:error {:message "Unable to debit amount.", :code 500}})))

(defn- get-balance [account-number]
  (try
    (let [date current-date
          balance (calculate-balance (get-account account-number) date)]
      {:data {:description (humanize-brazilian-money balance),
              :balance balance,
              :date date}})
    (catch Exception e
      {:error {:message "Unable to retrieve account balance.", :code 500}})))

(defn- get-statements [account-number params]
  (try
    (let [from (get params :from)
          to (get params :to)]
      {:data {:statements (period-statements account-number from to),
              :period {:from from, :to to}}})
    (catch Exception e
      {:error {:message "Unable to retrieve account statements.", :code 500}})))

(defn- get-debts [account-number]
  (prn "get-debts says hello")
  "get-debts says hello")

(defroutes app-routes
  (GET "/accounts" [] (response (get-accounts)))
  (POST "/accounts" {body :body} (response (post-accounts body)))
  (context "/:account" [account]
    (POST "/credits" {body :body} (response (post-credits account body)))
    (POST "/debits" {body :body} (response (post-debits account body)))
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
