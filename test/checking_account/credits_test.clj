(ns checking-account.credits-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]))

(facts "about `/:account/credits`"
  (fact "POST creates a credit operation to the account"
    (let [date "2017-02-27"
          ; old operations data
          acc-num 456
          acc (get-account acc-num)
          operations (:operations acc)
          balance (calculate-balance acc date)

          ; response data
          params {:amount 198.00, :type "deposit", :date date}
          request (-> (request :post (str "/" acc-num "/credits"))
                      (body (generate-string params))
                      (content-type "application/json; charset=utf-8"))
          response (app request)
          body (parse-string (:body response) true)

          ; new operations data
          operation-id (generate-operation-id acc)
          description (generate-description (:type params) (:amount params) (:date params)),
          operation {:amount (:amount params),
                     :date (:date params),
                     :description description,
                     :id operation-id}
          updated-acc (get-account acc-num)
          updated-operations (:operations updated-acc)
          updated-balance (calculate-balance updated-acc date)]

      ; start declaring fact
      (:status response) => 200

      "operations count is increased by 1"
      (count updated-operations) => (+ (count operations) 1)

      "response body is the operation from params"
      (:data body) => operation

      "account balance is increased by operation amount"
      (round-decimals (+ (:amount operation) balance) 2) => updated-balance)))
