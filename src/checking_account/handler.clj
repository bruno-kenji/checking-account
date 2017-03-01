(ns checking-account.handler
  (:use compojure.core
        ring.middleware.json
        ring.middleware.params
        ring.middleware.keyword-params
        clojure.walk)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]))

(def accounts
  (ref [{:account-number 123,
         :balance 0,
         :operations []},
        {:account-number 456,
         :balance 118.08,
         :operations [{:amount 118.08,
                       :date "2017-02-22",
                       :description "Deposit R$ 118.00 at 22/02/2017",
                       :id 1}]}]))

(defn make-account [params]
  (let [acc {:account-number (get params :account-number),
             :balance (get params :balance),
             :operations (get params :operations)}]
    (dosync
      (alter accounts conj acc))))

(defn get-account [account-number]
  (if (integer? account-number)
    (loop [index 0 size (count @accounts)]
      (let [acc (nth @accounts index)]
        (cond
          (= (get acc :account-number) account-number) acc
          (< index (dec size)) (recur (inc index) (dec size))
          :else
          (hash-map))))
    (let [acc-num (Integer/parseInt account-number)]
      (recur acc-num))))

(defn new-operation [account-number params]
  (try
    (let [operation {:amount (get params :amount),
                     :date (get params :date),
                     :description (get params :description),
                     :id (get params :operation-id)}
          acc (get-account account-number)
          operations {:operations (conj (get acc :operations) operation)}
          updated-acc (conj acc (sort-by :date operations))]
      (dosync
        (alter accounts delete-element (.indexOf @accounts acc))
        (alter accounts conj updated-acc)
        (prn @accounts)
        true))
    (catch Exception e
      (prn "new-operation Exception: " e)
      false)))

(defn- post-accounts [body]
  {:data (make-account body)})

(defn- get-accounts []
  {:data @accounts})

(defn- post-credits [account-number body]
  (prn "post-credits says hello")
  "post-credits says hello")

(defn- post-debits [account-number body]
  (prn "post-debits says hello")
  "post-debits says hello")

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
