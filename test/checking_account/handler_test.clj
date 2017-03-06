(ns checking-account.handler-test
  (:use midje.sweet
        ring.mock.request
        checking-account.handler
        checking-account.support
        cheshire.core))

(background
  (before :facts
    (dosync
      (ref-set accounts [{:account-number 123,
                          :balance 0,
                          :operations [],}
                         {:account-number 456,
                          :balance 118.08,
                          :operations [{:amount 118.08,
                                        :date "2017-02-22",
                                        :description "Deposit R$ 118.00 at 22/02/2017",
                                        :id 1}]}]))))

(fact "unexisting routes should return route not found"
  (let [response (app (request :get "/bogus-route"))]
    (:status response) => 404))
