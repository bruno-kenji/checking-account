(ns checking-account.utils
  (:use compojure.core
        ring.middleware.json
        ring.middleware.params
        ring.middleware.keyword-params
        clojure.walk
        helpers)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.coerce :as clj-time-coerce]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [checking-account.mock :refer [accounts]]))

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
                    (reduce #(round-decimals (+ %1 %2) 2))))]
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
      (prn "BLABLABLABALLAB")
      (prn account-number)
      (prn acc)
      (prn @accounts)
      (prn operation)
      (prn (.indexOf @accounts acc))
      (dosync
        (alter accounts delete-element-from-vector (.indexOf @accounts acc))
        (alter accounts conj updated-acc)
        (prn @accounts)
        true))
    (catch Exception e
      (prn "new-operation Exception: " e)
      false)))
