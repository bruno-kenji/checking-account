(ns checking-account.utils
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]
            [clj-time.coerce :as clj-time-coerce]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

(defn to-int [param]
  "Always spits param as integer"
  (if-not (integer? param)
    (Integer/parseInt param)
    param))

(defn is-included-in? [submap m]
  "Checks if submap is included in map"
  (clojure.set/subset? (set submap)
                       (set m)))

(defn delete-element-from-vector [vector index]
  "Returns vector without given index"
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn negativate-number [number]
  (- (max number (- number))))

(defn round-decimals [number decimal-places]
  "Round a double/float to the given number of decimal-places"
  (read-string (pprint/cl-format nil (str "~," decimal-places "f") number)))

(defn parse-date [date]
  "yyyy-MM-dd to DateTime"
  (clj-time-format/parse
    (clj-time-format/formatter :year-month-day)
    date))

(defn to-miliseconds [date]
  "yyyy-MM-dd to miliseconds since Unix Epoch"
  (clj-time-coerce/to-long
    (parse-date date)))

(defn humanize-brazilian-date [date]
  "Converts yyyy-MM-dd to dd-MM-yyyy"
  (clj-time-format/unparse
    (clj-time-format/formatter "dd/MM/yyyy")
    (parse-date date)))

(defn humanize-brazilian-money [amount]
  "i.e. Converts 1000.0 to R$ 1000,00"
  (let [replaced-amount (str/replace (str amount) "." ",")
        splitted-amount (str/split replaced-amount #",")
        amount-decimals (last splitted-amount)]
    (if (< (count amount-decimals) 2)
      (loop [decimals (str amount-decimals "0")]
        (if (< (count decimals) 2)
          (recur (str decimals "0"))
          (str "R$ " (str (first splitted-amount) "," decimals))))
      (str "R$ " replaced-amount))))

(def current-date
  (clj-time-format/unparse
    (clj-time-format/formatter :year-month-day)
    (clj-time/now)))

(defn get-date [date]
  (if (empty? date)
    current-date
    date))

(defn do-at* [date func]
  (org.joda.time.DateTimeUtils/setCurrentMillisFixed (to-miliseconds date))
  (try
    (func)
    (finally (org.joda.time.DateTimeUtils/setCurrentMillisSystem))))

(defmacro do-at [date & body]
  "Evalautes the expression at the given time"
  `(do-at* ~date
    (fn [] ~@body)))
