(ns checking-account.handler
  (:use compojure.core
        ring.middleware.json
        ring.middleware.params
        ring.middleware.keyword-params
        clojure.walk)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.coerce :as clj-time-coerce]
            [clojure.string :as str]))

(defn delete-element-from-vector [vector index]
  "Returns vector without given index"
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn negativate-number [number]
  (- (max number (- number))))

(defn two-decimals [number]
  (float (/ (Math/round (* number 100)) 100)))

(defn parse-date [date]
  "yyyy-MM-dd to DateTime"
  (clj-time-format/parse
    (clj-time-format/formatter :year-month-day)
    date))

(defn to-miliseconds [date]
  "yyyy-MM-dd to miliseconds since Unix Epoch"
  (clj-time-coerce/to-long
    (parse-date date)))

(defn humanize-brazilian-date [date]
  "Converts yyyy-MM-dd to dd-MM-yyyy"
  (clj-time-format/unparse
    (clj-time-format/formatter "dd/MM/yyyy")
    (parse-date date)))

(defn humanize-brazilian-money [amount]
  "i.e. Converts 1000.0 to R$ 1000,00"
  (let [replaced-amount (str/replace (str amount) "." ",")
        splitted-amount (str/split replaced-amount #",")
        amount-decimals (last splitted-amount)]
    (if (< (count amount-decimals) 2)
      (loop [decimals (str amount-decimals "0")]
        (if (< (count decimals) 2)
          (recur (str decimals "0"))
          (str "R$ " (str (first splitted-amount) "," decimals))))
      (str "R$ " replaced-amount))))

(def current-date
  (clj-time-format/unparse
    (clj-time-format/formatter :year-month-day)
    (clj-time/now)))

(defn get-date [date]
  (if (empty? date)
    current-date
    date))

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

(defn generate-operation-id [acc]
  (inc (count (get acc :operations))))

(defn generate-description [type amount date & [other-party]]
  (let [humanized-date (humanize-brazilian-date date)
        humanized-money (humanize-brazilian-money amount)]
    (cond
      ; Credit
      (= type "deposit")
      (str "Deposit " humanized-money " at " humanized-date)
      (= type "credit")
      (str "Credit " humanized-money " at " humanized-date)
      (= type "salary")
      (str "Salary " humanized-money " at " humanized-date)
      ; Debit
      (= type "debit")
      (str "Debit " humanized-money " at " humanized-date)
      (= type "withdrawal")
      (str "Withdrawal " humanized-money " at " humanized-date)
      (= type "purchase")
      (if (empty? other-party)
        (str "Purchase " humanized-money " at " humanized-date)
        (str "Purchase " humanized-money " at " humanized-date " on " other-party))
      :else
      "No description")))

(defn description-without-date [description]
  (str/replace description #" at [^.]{10}" ""))

(defn operations-by-date [operations date]
  (filter #(= date (get % :date)) operations))

(defn statement-descriptions [operations date]
  (vec (map #(description-without-date (get % :description))
             (operations-by-date operations date))))

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

(defn calculate-balance [acc date]
  (let [operations (filter #(<=
                             (to-miliseconds (get % :date))
                             (to-miliseconds date))
                     (get acc :operations))
        balance (if (empty? operations)
                  (int 0)
                  (->> operations
                    (map #(get % :amount))
                    (reduce #(two-decimals (+ %1 %2)))))]
    balance))

(defn period-statements [account-number from to]
  (let [acc (get-account account-number)
        filters [#(>=
                    (to-miliseconds (get % :date))
                    (to-miliseconds from))
                 #(<=
                    (to-miliseconds (get % :date))
                    (to-miliseconds to))]
        operations (filter (apply every-pred filters) (get acc :operations))
        dates (distinct (vec (map #(% :date) operations)))
        statements (vec (map #(hash-map
                                :date %,
                                :balance (calculate-balance acc %),
                                :descriptions (statement-descriptions operations %)) dates))]
    (sort-by :date statements)))

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
        (alter accounts delete-element-from-vector (.indexOf @accounts acc))
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
