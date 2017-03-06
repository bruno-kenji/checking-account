(ns checking-account.mock
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

(def accounts*
  (ref [{:account-number 123,
         :operations []},
        {:account-number 456,
         :operations [{:amount 118.08,
                       :date "2017-02-22",
                       :description "Deposit R$ 118.00 at 22/02/2017",
                       :id 1}]}]))
