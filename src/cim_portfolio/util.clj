;; gorilla-repl.fileformat = 1

;; **
;;; # Utility Functions for Portfolio Processing and Analysis
;; **

;; @@
(ns cim_portfolio.util
  (:require [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.data.csv :as csv]
            [java-time :as jt]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
;; FOR DATE PROCESSING
(def multi-parser (tf/formatter (t/default-time-zone) "YYYY-MM-dd" "YYYY/MM/dd" "YYYYMMdd"))

(defn parse-date [date]
  (tf/unparse multi-parser (tf/parse multi-parser date)))

(defn read-csv [file]
  (with-open [reader (clojure.java.io/reader file)]
    (doall (csv/read-csv reader))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.util/read-csv</span>","value":"#'cim_portfolio.util/read-csv"}
;; <=

;; @@
(parse-date "2023/12/25")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;2023-12-25&quot;</span>","value":"\"2023-12-25\""}
;; <=

;; @@
;; HELPER FUNCTIONS FOR ORDERING A MAP BY THE DATE
(defn jt-parse-date [date-str]
  (jt/local-date "yyyy-MM-dd" date-str))

(defn jt-compare-dates [date-str1 date-str2]
  (let [date1 (jt-parse-date date-str1)
        date2 (jt-parse-date date-str2)]
    (jt/before? date1 date2)))

(defn sort-map-by-date [map]
  (into (sorted-map-by jt-compare-dates) map))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.util/sort-map-by-date</span>","value":"#'cim_portfolio.util/sort-map-by-date"}
;; <=

;; @@
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
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>6</span>","value":"6"}
;; <=
