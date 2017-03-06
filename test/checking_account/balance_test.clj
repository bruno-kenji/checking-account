(ns checking-account.balance-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        checking-account.support
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]
            [checking-account.utils :refer [do-at
                                            current-date
                                            humanize-brazilian-money]]))

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

        ; start declaring fact
        (:status response) => 200

        "returned balance should be the calculated balance from current date"
        (:data body) => expected-response))))
