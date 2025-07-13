;;; # Utility Functions for Portfolio Processing and Analysis

(ns cim_portfolio.util
  (:require [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [clj-time.format :as tf]
            [clojure.data.csv :as csv]
            [java-time :as jt]))

;; FOR DATE PROCESSING
(def multi-parser (tf/formatter (t/default-time-zone) "YYYY-MM-dd" "YYYY/MM/dd" "YYYYMMdd"))

(defn parse-date [date]
  (tf/unparse multi-parser (tf/parse multi-parser date)))

(defn number-of-days-between [start-date end-date]
  (let [end 	(coerce/from-string end-date)
        start 	(coerce/from-string start-date)
        days  	(t/in-days (t/interval start end))]
    days))

(defn read-csv [file]
  (with-open [reader (clojure.java.io/reader file)]
    (doall (csv/read-csv reader))))

(defn read-multiple-csv [input-files]
  (let [combined-data (->> input-files
                           (map-indexed (fn [idx file]
                                          (let [csv-data (csv/read-csv (clojure.java.io/reader file))]
                                            (if (zero? idx)
                                              csv-data
                                              (rest csv-data)))))
                           (apply concat))]
    combined-data
))


(number-of-days-between "2023-10-13" "2024-04-27")



(def example-input-files ["examples/testPortfolio.csv" "examples/testPortfolio1.csv"])

(read-multiple-csv example-input-files)

(parse-date "2023/12/25")

;; HELPER FUNCTIONS FOR ORDERING A MAP BY THE DATE
(defn jt-parse-date [date-str]
  (jt/local-date "yyyy-MM-dd" date-str))

(defn jt-compare-dates [date-str1 date-str2]
  (let [date1 (jt-parse-date date-str1)
        date2 (jt-parse-date date-str2)]
    (jt/before? date1 date2)))

(defn sort-map-by-date [map]
  (into (sorted-map-by jt-compare-dates) map))

;; Helper functions for computing cumulative returns
(defn sum-up-to-key [key data]
  (
   + (get data key 0)		;; Adds data entry of key itself as well
     (->> (keys data)	;; calculates the sum of entries till key
       (take-while #(not= % key))
       (map #(get data % 0))
       (apply +))
   )
  
)


(sum-up-to-key "c" {"a" 1 "b" 2 "c" 3 "d" 4 "e" 5}) ; Output: 6

(defn mean [a] (/ (reduce + a) (count a)))

(defn square [n]
  (* n n))

(defn squares [avg prices] (map #(square (- % avg)) prices))

(defn std-dev [coll]
  (let [avg (mean coll)
        squares (squares avg coll)
        total (count coll)]
    (Math/sqrt (/ (reduce + squares) (- total 1)))))

