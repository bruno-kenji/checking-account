(ns checking-account.mock)

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
