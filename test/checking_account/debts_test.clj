(ns checking-account.debts-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        checking-account.support
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]))

(facts "about `/:account/debts`"
  (fact "GET returns lifetime account debt periods"
    (make-account! {:account-number 1234,
                    :operations [{:amount 100,
                                  :date "2017-02-22",
                                  :description "Deposit R$ 100.00 at 22/02/2017"},
                                 {:amount -150,
                                  :date "2017-02-23",
                                  :description "Purchase R$ 150.00 at 23/02/2017 on Amazon"},
                                 {:amount 200,
                                  :date "2017-02-28",
                                  :description "Deposit R$ 200.00 at 28/02/2017"},
                                 {:amount -500,
                                  :date "2017-03-02",
                                  :description "Withdrawal R$ 500.00 at 02/03/2017"}]})

    (let [acc-num 1234
          expected-response {:debts (debt-periods acc-num)}
          response (app (request :get (str "/" acc-num "/debts")))
          body (parse-string (:body response) true)]

      ; start declaring fact
      (:status response) => 200

      "returned data should be the debt periods until current date"
      (:data body) => expected-response)))
