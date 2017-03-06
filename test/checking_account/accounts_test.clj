(ns checking-account.accounts-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        checking-account.support
        cheshire.core)
  (:require [checking-account.handler-test :refer :all]))

(facts "about `/accounts`"
  (fact "GET returns all the accounts"
    (let [response (app (request :get "/accounts"))]
      (:status response) => 200
      (let [body (parse-string (:body response) true)]
        (:data body) => @accounts)))

  (fact "POST creates an account"
    (let [accCount (count @accounts)
          params {:account-number 789, :operations []}
          request (-> (request :post "/accounts")
                      (body (generate-string params))
                      (content-type "application/json; charset=utf-8"))
          response (app request)
          body (parse-string (:body response) true)]

      ; start declaring fact
      (:status response) => 200

      "@accounts count is increased by 1"
      (count @accounts) => (+ accCount 1)

      "account from params is appended to @accounts"
      (last @accounts) => params

      "response body is the account from params"
      (:data body) => params)))
