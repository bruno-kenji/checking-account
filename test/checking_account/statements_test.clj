(ns checking-account.statements-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        checking-account.support
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]))

(facts "about `/:account/statements`"
  (fact "GET returns account statements from period"
    (let [acc-num 456
          from "2017-01-01"
          to "2017-02-28"
          expected-response {:statements (period-statements acc-num from to),
                             :period {:from from, :to to}}
          response (app (request :get (str "/" acc-num "/statements") {:from from :to to}))
          body (parse-string (:body response) true)]

      ; start declaring fact
      (:status response) => 200

      "returned statements should be the period statements from date interval"
      (:data body) => expected-response)))
