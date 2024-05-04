;; gorilla-repl.fileformat = 1

;; **
;;; # Utility Functions for Portfolio Processing and Analysis
;; **

;; @@
(ns cim_portfolio.util
  (:require [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
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
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.util/read-multiple-csv</span>","value":"#'cim_portfolio.util/read-multiple-csv"}
;; <=

;; @@
(number-of-days-between "2023-10-13" "2024-04-27")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>197</span>","value":"197"}
;; <=

;; @@
(def example-input-files ["examples/testPortfolio.csv" "examples/testPortfolio1.csv"])

(read-multiple-csv example-input-files)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;Date of trade submitted&quot;</span>","value":"\"Date of trade submitted\""},{"type":"html","content":"<span class='clj-string'>&quot;Action&quot;</span>","value":"\"Action\""},{"type":"html","content":"<span class='clj-string'>&quot;Amount bought&quot;</span>","value":"\"Amount bought\""},{"type":"html","content":"<span class='clj-string'>&quot;Ticker&quot;</span>","value":"\"Ticker\""}],"value":"[\"Date of trade submitted\" \"Action\" \"Amount bought\" \"Ticker\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-10-13&quot;</span>","value":"\"2023-10-13\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;100&quot;</span>","value":"\"100\""},{"type":"html","content":"<span class='clj-string'>&quot;IEF&quot;</span>","value":"\"IEF\""}],"value":"[\"2023-10-13\" \"buy\" \"100\" \"IEF\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-10-13&quot;</span>","value":"\"2023-10-13\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;20&quot;</span>","value":"\"20\""},{"type":"html","content":"<span class='clj-string'>&quot;SHY&quot;</span>","value":"\"SHY\""}],"value":"[\"2023-10-13\" \"buy\" \"20\" \"SHY\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-10-13&quot;</span>","value":"\"2023-10-13\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;35&quot;</span>","value":"\"35\""},{"type":"html","content":"<span class='clj-string'>&quot;RSP&quot;</span>","value":"\"RSP\""}],"value":"[\"2023-10-13\" \"buy\" \"35\" \"RSP\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-10-13&quot;</span>","value":"\"2023-10-13\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;100&quot;</span>","value":"\"100\""},{"type":"html","content":"<span class='clj-string'>&quot;SPY&quot;</span>","value":"\"SPY\""}],"value":"[\"2023-10-13\" \"buy\" \"100\" \"SPY\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-11-3&quot;</span>","value":"\"2023-11-3\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;50&quot;</span>","value":"\"50\""},{"type":"html","content":"<span class='clj-string'>&quot;SPY&quot;</span>","value":"\"SPY\""}],"value":"[\"2023-11-3\" \"buy\" \"50\" \"SPY\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-11-3&quot;</span>","value":"\"2023-11-3\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;250&quot;</span>","value":"\"250\""},{"type":"html","content":"<span class='clj-string'>&quot;GSG&quot;</span>","value":"\"GSG\""}],"value":"[\"2023-11-3\" \"buy\" \"250\" \"GSG\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-11-3&quot;</span>","value":"\"2023-11-3\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;150&quot;</span>","value":"\"150\""},{"type":"html","content":"<span class='clj-string'>&quot;SHY&quot;</span>","value":"\"SHY\""}],"value":"[\"2023-11-3\" \"buy\" \"150\" \"SHY\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-12-1&quot;</span>","value":"\"2023-12-1\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;300&quot;</span>","value":"\"300\""},{"type":"html","content":"<span class='clj-string'>&quot;AGG&quot;</span>","value":"\"AGG\""}],"value":"[\"2023-12-1\" \"buy\" \"300\" \"AGG\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-15&quot;</span>","value":"\"2024-02-15\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;100&quot;</span>","value":"\"100\""},{"type":"html","content":"<span class='clj-string'>&quot;NVDA&quot;</span>","value":"\"NVDA\""}],"value":"[\"2024-02-15\" \"buy\" \"100\" \"NVDA\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-25&quot;</span>","value":"\"2024-02-25\""},{"type":"html","content":"<span class='clj-string'>&quot;buy&quot;</span>","value":"\"buy\""},{"type":"html","content":"<span class='clj-string'>&quot;50&quot;</span>","value":"\"50\""},{"type":"html","content":"<span class='clj-string'>&quot;GOOG&quot;</span>","value":"\"GOOG\""}],"value":"[\"2024-02-25\" \"buy\" \"50\" \"GOOG\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-29&quot;</span>","value":"\"2024-02-29\""},{"type":"html","content":"<span class='clj-string'>&quot;sell&quot;</span>","value":"\"sell\""},{"type":"html","content":"<span class='clj-string'>&quot;30&quot;</span>","value":"\"30\""},{"type":"html","content":"<span class='clj-string'>&quot;TSLA&quot;</span>","value":"\"TSLA\""}],"value":"[\"2024-02-29\" \"sell\" \"30\" \"TSLA\"]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-08&quot;</span>","value":"\"2024-03-08\""},{"type":"html","content":"<span class='clj-string'>&quot;sell&quot;</span>","value":"\"sell\""},{"type":"html","content":"<span class='clj-string'>&quot;30&quot;</span>","value":"\"30\""},{"type":"html","content":"<span class='clj-string'>&quot;NVDA&quot;</span>","value":"\"NVDA\""}],"value":"[\"2024-03-08\" \"sell\" \"30\" \"NVDA\"]"}],"value":"([\"Date of trade submitted\" \"Action\" \"Amount bought\" \"Ticker\"] [\"2023-10-13\" \"buy\" \"100\" \"IEF\"] [\"2023-10-13\" \"buy\" \"20\" \"SHY\"] [\"2023-10-13\" \"buy\" \"35\" \"RSP\"] [\"2023-10-13\" \"buy\" \"100\" \"SPY\"] [\"2023-11-3\" \"buy\" \"50\" \"SPY\"] [\"2023-11-3\" \"buy\" \"250\" \"GSG\"] [\"2023-11-3\" \"buy\" \"150\" \"SHY\"] [\"2023-12-1\" \"buy\" \"300\" \"AGG\"] [\"2024-02-15\" \"buy\" \"100\" \"NVDA\"] [\"2024-02-25\" \"buy\" \"50\" \"GOOG\"] [\"2024-02-29\" \"sell\" \"30\" \"TSLA\"] [\"2024-03-08\" \"sell\" \"30\" \"NVDA\"])"}
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

;; @@
(defn mean [a] (/ (reduce + a) (count a)))

(defn square [n]
  (* n n))

(defn squares [avg prices] (map #(square (- % avg)) prices))

(defn std-dev [coll]
  (let [avg (mean coll)
        squares (squares avg coll)
        total (count coll)]
    (Math/sqrt (/ (reduce + squares) (- total 1)))))

;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.util/std-dev</span>","value":"#'cim_portfolio.util/std-dev"}
;; <=
