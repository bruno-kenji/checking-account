(ns checking-account.balance-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]))

(do-at "2017-03-01"
  (facts "about `/:account/balance`"
    (fact "GET returns current account balance"
      (let [acc-num 456
            date current-date
            balance (calculate-balance (get-account acc-num) date)
            expected-response {:description (humanize-brazilian-money balance),
                               :balance balance,
                               :date date}
            response (app (request :get "/456/balance"))
            body (parse-string (:body response) true)]
        (prn date)
        (prn date)
        (prn date)
        (prn date)
        (prn date)

        ; start declaring fact
        (:status response) => 200

        "returned balance should be the calculated balance from current date"
        (:data body) => expected-response))))
