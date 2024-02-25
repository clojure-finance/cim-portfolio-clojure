;; gorilla-repl.fileformat = 1

;; **
;;; # CIM Portfolio
;; **

;; **
;;; ### Required packages
;; **

;; @@
(ns portfolio
  (:require [clojure.data.csv :as csv]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as str]
            [java-time :as jt]
            ))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Helper Functions
;; **

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
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/read-csv</span>","value":"#'portfolio/read-csv"}
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
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/sort-map-by-date</span>","value":"#'portfolio/sort-map-by-date"}
;; <=

;; @@
;; Helper functions for computing cumulative returns
(defn sum-up-to-key [key data]
  
   ;+ (get data key 0)		;; Adds data entry of key itself as well
     (->> (keys data)	;; calculates the sum of entries till key
       (take-while #(not= % key))
       (map #(get data %))
       (apply +))
  
)


(sum-up-to-key "c" {"a" 1 "b" 2 "c" 3 "d" 4 "e" 5}) ; Output: 6
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>3</span>","value":"3"}
;; <=

;; @@
;; Computes arithmetic, log, and cumulative return for a given list of prices
(defn calculate-returns [prices]
  (let [price-changes (map #(double (/ (second %) (first %))) (partition 2 1 prices))
        arithmetic-returns (mapv #(- % 1.0) price-changes)
        log-returns (mapv #(Math/log %) price-changes)
        cumulative-log-return (reduce + log-returns)]
    {
     :cumulative-return cumulative-log-return
     :arithmetic-returns arithmetic-returns
     :log-returns log-returns
     }))

;; Similar to function above - associates the returns with their corresponding date
(defn calculate-returns-with-corresponding-date [prices dates]
  (let [price-changes (map #(double (/ (second %) (first %))) (partition 2 1 prices))
        arithmetic-returns (mapv #(- % 1.0) price-changes)
        log-returns (mapv #(Math/log %) price-changes)
        cumulative-log-return (reduce + log-returns)]
    {
     :cumulative-return cumulative-log-return
     :arithmetic-returns (sort-map-by-date (zipmap (rest dates) arithmetic-returns))
     :log-returns (sort-map-by-date (zipmap (rest dates) log-returns))
     }))

;; Calculates portfolio return - accepts map {:stock cash-invested} and stock-performance generated by calculate-returns
(defn calculate-portfolio-return [cash-invested returns]
  (let [total-investment (apply + (vals cash-invested))]
    (->> cash-invested
         (map (fn [[security invested]]
                (let [weight (/ invested total-investment)
                      return (:cumulative-return (get returns security))]
                  (* weight return))))
         (apply +))))

;; Calculates cumulative return UP TILL a given date
(defn get-cumulative-return-till-given-date [cumulative-returns date]
  (sum-up-to-key date cumulative-returns)
)

;; Only calculates portfolio return up till a given date
(defn calculate-portfolio-return-for-given-date [cash-invested returns date]
  ;(println "------")
  ;(println "Calculating returns on: " date)
  ;(println cash-invested)
  ;(println "------")
  (let [total-investment (apply + (vals cash-invested))]
    (->> cash-invested
         (map (fn [[security invested]]
                (let [weight (/ invested total-investment)
                      return (get-cumulative-return-till-given-date (:log-returns (get returns security)) date)
                     ]
                  (* weight return))))
         (apply +))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/calculate-portfolio-return-for-given-date</span>","value":"#'portfolio/calculate-portfolio-return-for-given-date"}
;; <=

;; @@
(calculate-returns [100 110 120 130])
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:cumulative-return</span>","value":":cumulative-return"},{"type":"html","content":"<span class='clj-double'>0.262364264467491</span>","value":"0.262364264467491"}],"value":"[:cumulative-return 0.262364264467491]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:arithmetic-returns</span>","value":":arithmetic-returns"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.10000000000000009</span>","value":"0.10000000000000009"},{"type":"html","content":"<span class='clj-double'>0.09090909090909105</span>","value":"0.09090909090909105"},{"type":"html","content":"<span class='clj-double'>0.08333333333333304</span>","value":"0.08333333333333304"}],"value":"[0.10000000000000009 0.09090909090909105 0.08333333333333304]"}],"value":"[:arithmetic-returns [0.10000000000000009 0.09090909090909105 0.08333333333333304]]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:log-returns</span>","value":":log-returns"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.09531017980432493</span>","value":"0.09531017980432493"},{"type":"html","content":"<span class='clj-double'>0.0870113769896299</span>","value":"0.0870113769896299"},{"type":"html","content":"<span class='clj-double'>0.08004270767353615</span>","value":"0.08004270767353615"}],"value":"[0.09531017980432493 0.0870113769896299 0.08004270767353615]"}],"value":"[:log-returns [0.09531017980432493 0.0870113769896299 0.08004270767353615]]"}],"value":"{:cumulative-return 0.262364264467491, :arithmetic-returns [0.10000000000000009 0.09090909090909105 0.08333333333333304], :log-returns [0.09531017980432493 0.0870113769896299 0.08004270767353615]}"}
;; <=

;; **
;;; ### YFinance API
;; **

;; @@
(defn get-ticker-price-all [ticker date]
  (let [url (str "http://localhost:5000/price?ticker=" ticker "&date=" date)
        response (http/get url)
        body (json/parse-string (:body response) true)]
    (if (not= (:error body) "No data available for the provided ticker and date.")
      body
      nil)
    )
  )
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/get-ticker-price-all</span>","value":"#'portfolio/get-ticker-price-all"}
;; <=

;; @@
(get-ticker-price-all "AAPL" "2023-12-25")
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-12-26&quot;</span>","value":"\"2023-12-26\""},{"type":"html","content":"<span class='clj-double'>193.6100006104</span>","value":"193.6100006104"},{"type":"html","content":"<span class='clj-double'>192.8039855957</span>","value":"192.8039855957"}],"value":"[\"2023-12-26\" 193.6100006104 192.8039855957]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-12-27&quot;</span>","value":"\"2023-12-27\""},{"type":"html","content":"<span class='clj-double'>192.4900054932</span>","value":"192.4900054932"},{"type":"html","content":"<span class='clj-double'>192.9038391113</span>","value":"192.9038391113"}],"value":"[\"2023-12-27\" 192.4900054932 192.9038391113]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-12-28&quot;</span>","value":"\"2023-12-28\""},{"type":"html","content":"<span class='clj-double'>194.1399993896</span>","value":"194.1399993896"},{"type":"html","content":"<span class='clj-double'>193.3332977295</span>","value":"193.3332977295"}],"value":"[\"2023-12-28\" 194.1399993896 193.3332977295]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2023-12-29&quot;</span>","value":"\"2023-12-29\""},{"type":"html","content":"<span class='clj-double'>193.8999938965</span>","value":"193.8999938965"},{"type":"html","content":"<span class='clj-double'>192.2846374512</span>","value":"192.2846374512"}],"value":"[\"2023-12-29\" 193.8999938965 192.2846374512]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-02&quot;</span>","value":"\"2024-01-02\""},{"type":"html","content":"<span class='clj-double'>187.1499938965</span>","value":"187.1499938965"},{"type":"html","content":"<span class='clj-double'>185.4034118652</span>","value":"185.4034118652"}],"value":"[\"2024-01-02\" 187.1499938965 185.4034118652]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-03&quot;</span>","value":"\"2024-01-03\""},{"type":"html","content":"<span class='clj-double'>184.2200012207</span>","value":"184.2200012207"},{"type":"html","content":"<span class='clj-double'>184.0151977539</span>","value":"184.0151977539"}],"value":"[\"2024-01-03\" 184.2200012207 184.0151977539]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-04&quot;</span>","value":"\"2024-01-04\""},{"type":"html","content":"<span class='clj-double'>182.1499938965</span>","value":"182.1499938965"},{"type":"html","content":"<span class='clj-double'>181.6781768799</span>","value":"181.6781768799"}],"value":"[\"2024-01-04\" 182.1499938965 181.6781768799]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-05&quot;</span>","value":"\"2024-01-05\""},{"type":"html","content":"<span class='clj-double'>181.9900054932</span>","value":"181.9900054932"},{"type":"html","content":"<span class='clj-double'>180.9490966797</span>","value":"180.9490966797"}],"value":"[\"2024-01-05\" 181.9900054932 180.9490966797]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-08&quot;</span>","value":"\"2024-01-08\""},{"type":"html","content":"<span class='clj-double'>182.0899963379</span>","value":"182.0899963379"},{"type":"html","content":"<span class='clj-double'>185.3235168457</span>","value":"185.3235168457"}],"value":"[\"2024-01-08\" 182.0899963379 185.3235168457]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-09&quot;</span>","value":"\"2024-01-09\""},{"type":"html","content":"<span class='clj-double'>183.9199981689</span>","value":"183.9199981689"},{"type":"html","content":"<span class='clj-double'>184.9040527344</span>","value":"184.9040527344"}],"value":"[\"2024-01-09\" 183.9199981689 184.9040527344]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-10&quot;</span>","value":"\"2024-01-10\""},{"type":"html","content":"<span class='clj-double'>184.3500061035</span>","value":"184.3500061035"},{"type":"html","content":"<span class='clj-double'>185.9527130127</span>","value":"185.9527130127"}],"value":"[\"2024-01-10\" 184.3500061035 185.9527130127]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-11&quot;</span>","value":"\"2024-01-11\""},{"type":"html","content":"<span class='clj-double'>186.5399932861</span>","value":"186.5399932861"},{"type":"html","content":"<span class='clj-double'>185.3534851074</span>","value":"185.3534851074"}],"value":"[\"2024-01-11\" 186.5399932861 185.3534851074]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-12&quot;</span>","value":"\"2024-01-12\""},{"type":"html","content":"<span class='clj-double'>186.0599975586</span>","value":"186.0599975586"},{"type":"html","content":"<span class='clj-double'>185.6830596924</span>","value":"185.6830596924"}],"value":"[\"2024-01-12\" 186.0599975586 185.6830596924]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-16&quot;</span>","value":"\"2024-01-16\""},{"type":"html","content":"<span class='clj-double'>182.1600036621</span>","value":"182.1600036621"},{"type":"html","content":"<span class='clj-double'>183.395980835</span>","value":"183.395980835"}],"value":"[\"2024-01-16\" 182.1600036621 183.395980835]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-17&quot;</span>","value":"\"2024-01-17\""},{"type":"html","content":"<span class='clj-double'>181.2700042725</span>","value":"181.2700042725"},{"type":"html","content":"<span class='clj-double'>182.4471893311</span>","value":"182.4471893311"}],"value":"[\"2024-01-17\" 181.2700042725 182.4471893311]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-18&quot;</span>","value":"\"2024-01-18\""},{"type":"html","content":"<span class='clj-double'>186.0899963379</span>","value":"186.0899963379"},{"type":"html","content":"<span class='clj-double'>188.3896179199</span>","value":"188.3896179199"}],"value":"[\"2024-01-18\" 186.0899963379 188.3896179199]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-19&quot;</span>","value":"\"2024-01-19\""},{"type":"html","content":"<span class='clj-double'>189.3300018311</span>","value":"189.3300018311"},{"type":"html","content":"<span class='clj-double'>191.3158721924</span>","value":"191.3158721924"}],"value":"[\"2024-01-19\" 189.3300018311 191.3158721924]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-22&quot;</span>","value":"\"2024-01-22\""},{"type":"html","content":"<span class='clj-double'>192.3000030518</span>","value":"192.3000030518"},{"type":"html","content":"<span class='clj-double'>193.6428985596</span>","value":"193.6428985596"}],"value":"[\"2024-01-22\" 192.3000030518 193.6428985596]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-23&quot;</span>","value":"\"2024-01-23\""},{"type":"html","content":"<span class='clj-double'>195.0200042725</span>","value":"195.0200042725"},{"type":"html","content":"<span class='clj-double'>194.9312591553</span>","value":"194.9312591553"}],"value":"[\"2024-01-23\" 195.0200042725 194.9312591553]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-24&quot;</span>","value":"\"2024-01-24\""},{"type":"html","content":"<span class='clj-double'>195.4199981689</span>","value":"195.4199981689"},{"type":"html","content":"<span class='clj-double'>194.2521209717</span>","value":"194.2521209717"}],"value":"[\"2024-01-24\" 195.4199981689 194.2521209717]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-25&quot;</span>","value":"\"2024-01-25\""},{"type":"html","content":"<span class='clj-double'>195.2200012207</span>","value":"195.2200012207"},{"type":"html","content":"<span class='clj-double'>193.9225463867</span>","value":"193.9225463867"}],"value":"[\"2024-01-25\" 195.2200012207 193.9225463867]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-26&quot;</span>","value":"\"2024-01-26\""},{"type":"html","content":"<span class='clj-double'>194.2700042725</span>","value":"194.2700042725"},{"type":"html","content":"<span class='clj-double'>192.1747741699</span>","value":"192.1747741699"}],"value":"[\"2024-01-26\" 194.2700042725 192.1747741699]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-29&quot;</span>","value":"\"2024-01-29\""},{"type":"html","content":"<span class='clj-double'>192.0099945068</span>","value":"192.0099945068"},{"type":"html","content":"<span class='clj-double'>191.4856567383</span>","value":"191.4856567383"}],"value":"[\"2024-01-29\" 192.0099945068 191.4856567383]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-30&quot;</span>","value":"\"2024-01-30\""},{"type":"html","content":"<span class='clj-double'>190.9400024414</span>","value":"190.9400024414"},{"type":"html","content":"<span class='clj-double'>187.8003540039</span>","value":"187.8003540039"}],"value":"[\"2024-01-30\" 190.9400024414 187.8003540039]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-01-31&quot;</span>","value":"\"2024-01-31\""},{"type":"html","content":"<span class='clj-double'>187.0399932861</span>","value":"187.0399932861"},{"type":"html","content":"<span class='clj-double'>184.1649932861</span>","value":"184.1649932861"}],"value":"[\"2024-01-31\" 187.0399932861 184.1649932861]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-01&quot;</span>","value":"\"2024-02-01\""},{"type":"html","content":"<span class='clj-double'>183.9900054932</span>","value":"183.9900054932"},{"type":"html","content":"<span class='clj-double'>186.6218719482</span>","value":"186.6218719482"}],"value":"[\"2024-02-01\" 183.9900054932 186.6218719482]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-02&quot;</span>","value":"\"2024-02-02\""},{"type":"html","content":"<span class='clj-double'>179.8600006104</span>","value":"179.8600006104"},{"type":"html","content":"<span class='clj-double'>185.6131591797</span>","value":"185.6131591797"}],"value":"[\"2024-02-02\" 179.8600006104 185.6131591797]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-05&quot;</span>","value":"\"2024-02-05\""},{"type":"html","content":"<span class='clj-double'>188.1499938965</span>","value":"188.1499938965"},{"type":"html","content":"<span class='clj-double'>187.4408111572</span>","value":"187.4408111572"}],"value":"[\"2024-02-05\" 188.1499938965 187.4408111572]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-06&quot;</span>","value":"\"2024-02-06\""},{"type":"html","content":"<span class='clj-double'>186.8600006104</span>","value":"186.8600006104"},{"type":"html","content":"<span class='clj-double'>189.0587615967</span>","value":"189.0587615967"}],"value":"[\"2024-02-06\" 186.8600006104 189.0587615967]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-07&quot;</span>","value":"\"2024-02-07\""},{"type":"html","content":"<span class='clj-double'>190.6399993896</span>","value":"190.6399993896"},{"type":"html","content":"<span class='clj-double'>189.1686248779</span>","value":"189.1686248779"}],"value":"[\"2024-02-07\" 190.6399993896 189.1686248779]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-08&quot;</span>","value":"\"2024-02-08\""},{"type":"html","content":"<span class='clj-double'>189.3899993896</span>","value":"189.3899993896"},{"type":"html","content":"<span class='clj-double'>188.0800170898</span>","value":"188.0800170898"}],"value":"[\"2024-02-08\" 189.3899993896 188.0800170898]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-09&quot;</span>","value":"\"2024-02-09\""},{"type":"html","content":"<span class='clj-double'>188.6499938965</span>","value":"188.6499938965"},{"type":"html","content":"<span class='clj-double'>188.8500061035</span>","value":"188.8500061035"}],"value":"[\"2024-02-09\" 188.6499938965 188.8500061035]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-12&quot;</span>","value":"\"2024-02-12\""},{"type":"html","content":"<span class='clj-double'>188.4199981689</span>","value":"188.4199981689"},{"type":"html","content":"<span class='clj-double'>187.1499938965</span>","value":"187.1499938965"}],"value":"[\"2024-02-12\" 188.4199981689 187.1499938965]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-13&quot;</span>","value":"\"2024-02-13\""},{"type":"html","content":"<span class='clj-double'>185.7700042725</span>","value":"185.7700042725"},{"type":"html","content":"<span class='clj-double'>185.0399932861</span>","value":"185.0399932861"}],"value":"[\"2024-02-13\" 185.7700042725 185.0399932861]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-14&quot;</span>","value":"\"2024-02-14\""},{"type":"html","content":"<span class='clj-double'>185.3200073242</span>","value":"185.3200073242"},{"type":"html","content":"<span class='clj-double'>184.1499938965</span>","value":"184.1499938965"}],"value":"[\"2024-02-14\" 185.3200073242 184.1499938965]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-15&quot;</span>","value":"\"2024-02-15\""},{"type":"html","content":"<span class='clj-double'>183.5500030518</span>","value":"183.5500030518"},{"type":"html","content":"<span class='clj-double'>183.8600006104</span>","value":"183.8600006104"}],"value":"[\"2024-02-15\" 183.5500030518 183.8600006104]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-16&quot;</span>","value":"\"2024-02-16\""},{"type":"html","content":"<span class='clj-double'>183.4199981689</span>","value":"183.4199981689"},{"type":"html","content":"<span class='clj-double'>182.3099975586</span>","value":"182.3099975586"}],"value":"[\"2024-02-16\" 183.4199981689 182.3099975586]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-20&quot;</span>","value":"\"2024-02-20\""},{"type":"html","content":"<span class='clj-double'>181.7899932861</span>","value":"181.7899932861"},{"type":"html","content":"<span class='clj-double'>181.5599975586</span>","value":"181.5599975586"}],"value":"[\"2024-02-20\" 181.7899932861 181.5599975586]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-21&quot;</span>","value":"\"2024-02-21\""},{"type":"html","content":"<span class='clj-double'>181.9400024414</span>","value":"181.9400024414"},{"type":"html","content":"<span class='clj-double'>182.3200073242</span>","value":"182.3200073242"}],"value":"[\"2024-02-21\" 181.9400024414 182.3200073242]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-22&quot;</span>","value":"\"2024-02-22\""},{"type":"html","content":"<span class='clj-double'>183.4799957275</span>","value":"183.4799957275"},{"type":"html","content":"<span class='clj-double'>184.3699951172</span>","value":"184.3699951172"}],"value":"[\"2024-02-22\" 183.4799957275 184.3699951172]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-02-23&quot;</span>","value":"\"2024-02-23\""},{"type":"html","content":"<span class='clj-double'>185.0099945068</span>","value":"185.0099945068"},{"type":"html","content":"<span class='clj-double'>182.5200042725</span>","value":"182.5200042725"}],"value":"[\"2024-02-23\" 185.0099945068 182.5200042725]"}],"value":"([\"2023-12-26\" 193.6100006104 192.8039855957] [\"2023-12-27\" 192.4900054932 192.9038391113] [\"2023-12-28\" 194.1399993896 193.3332977295] [\"2023-12-29\" 193.8999938965 192.2846374512] [\"2024-01-02\" 187.1499938965 185.4034118652] [\"2024-01-03\" 184.2200012207 184.0151977539] [\"2024-01-04\" 182.1499938965 181.6781768799] [\"2024-01-05\" 181.9900054932 180.9490966797] [\"2024-01-08\" 182.0899963379 185.3235168457] [\"2024-01-09\" 183.9199981689 184.9040527344] [\"2024-01-10\" 184.3500061035 185.9527130127] [\"2024-01-11\" 186.5399932861 185.3534851074] [\"2024-01-12\" 186.0599975586 185.6830596924] [\"2024-01-16\" 182.1600036621 183.395980835] [\"2024-01-17\" 181.2700042725 182.4471893311] [\"2024-01-18\" 186.0899963379 188.3896179199] [\"2024-01-19\" 189.3300018311 191.3158721924] [\"2024-01-22\" 192.3000030518 193.6428985596] [\"2024-01-23\" 195.0200042725 194.9312591553] [\"2024-01-24\" 195.4199981689 194.2521209717] [\"2024-01-25\" 195.2200012207 193.9225463867] [\"2024-01-26\" 194.2700042725 192.1747741699] [\"2024-01-29\" 192.0099945068 191.4856567383] [\"2024-01-30\" 190.9400024414 187.8003540039] [\"2024-01-31\" 187.0399932861 184.1649932861] [\"2024-02-01\" 183.9900054932 186.6218719482] [\"2024-02-02\" 179.8600006104 185.6131591797] [\"2024-02-05\" 188.1499938965 187.4408111572] [\"2024-02-06\" 186.8600006104 189.0587615967] [\"2024-02-07\" 190.6399993896 189.1686248779] [\"2024-02-08\" 189.3899993896 188.0800170898] [\"2024-02-09\" 188.6499938965 188.8500061035] [\"2024-02-12\" 188.4199981689 187.1499938965] [\"2024-02-13\" 185.7700042725 185.0399932861] [\"2024-02-14\" 185.3200073242 184.1499938965] [\"2024-02-15\" 183.5500030518 183.8600006104] [\"2024-02-16\" 183.4199981689 182.3099975586] [\"2024-02-20\" 181.7899932861 181.5599975586] [\"2024-02-21\" 181.9400024414 182.3200073242] [\"2024-02-22\" 183.4799957275 184.3699951172] [\"2024-02-23\" 185.0099945068 182.5200042725])"}
;; <=

;; **
;;; ### Portfolio Processing Section
;; **

;; @@
(defn calculate-portfolio-new [data]
  (loop [cash 0.0
         portfolio {}
         portfolio-value {}
         current-value 0.0
         stock-performance {}
         cash-invested {}
         cash-invested-by-date {}
         data (rest data)]
    (if (empty? data)
      [cash portfolio portfolio-value current-value cash-invested cash-invested-by-date stock-performance]
      (let [[date action amount ticker] (first data)
            ticker-prices (get-ticker-price-all ticker (parse-date date))]
        (cond
          (= action "buy")
          (if (pos? (Double. amount))
            (let [price (second (first ticker-prices))              ; Gets open price of next trading day
                  currPrice (nth (last ticker-prices) 2)
                  prices (mapv #(nth % 2) ticker-prices)                ; Extracts the prices from ticker-prices
                  amounts (repeatedly (count prices) #(Double. amount))	; Creates a sequence of amounts matching the number of prices
                  trading-dates (mapv #(first %) ticker-prices)
                  ]
              (recur (- cash (* (Double. amount) price))
                     (assoc portfolio ticker (+ (get portfolio ticker 0) (Double. amount)))
                     (merge-with + portfolio-value (zipmap (map first ticker-prices) (map #(- % (* (Double. amount) price))(map * prices amounts)))) ; Updates portfolio-value with the calculated values
                     (+ current-value (* (Double. amount) currPrice))
                     
                     (assoc stock-performance ticker (calculate-returns-with-corresponding-date prices trading-dates))
                     (assoc cash-invested ticker (+ (get cash-invested ticker 0) (* (Double. amount) price)))
                     (assoc cash-invested-by-date date cash-invested)
                     (rest data)))
            (recur cash portfolio portfolio-value current-value cash-invested cash-invested-by-date stock-performance (rest data)))

          (= action "sell")
          (let [price (second (first ticker-prices))
                currPrice (second (last ticker-prices))
                prices (map second ticker-prices)
                amounts (repeatedly (count prices) #(Double. amount))]
            (recur (+ cash (* (Double. amount) price))
                   (assoc portfolio ticker (- (get portfolio ticker 0) (Double. amount)))
                   (merge-with + portfolio-value (zipmap (map first ticker-prices) (map #(- % (* (Double. amount) price))(map * prices amounts)))) ; Updates portfolio-value with the calculated values
                   (- current-value (* (Double. amount) currPrice))
                   
                   (assoc stock-performance ticker (calculate-returns prices))
                   (assoc cash-invested ticker (- (get cash-invested ticker 0) (* (Double. amount) price)))
                   (assoc cash-invested-by-date date cash-invested)
                   (rest data)))
          )))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/calculate-portfolio-new</span>","value":"#'portfolio/calculate-portfolio-new"}
;; <=

;; @@
(def input-file "testPortfolio.csv")

(let [data (read-csv input-file)
      [cash portfolio portfolio-value current-value cash-invested cash-invested-by-date stock-performance] (calculate-portfolio-new data)
      sorted-portfolio-value (sort-map-by-date portfolio-value)
      cash-invested-by-dates (into [] cash-invested-by-date)
      returns-by-date (zipmap (map #(first %) cash-invested-by-date)(mapv #(calculate-portfolio-return-for-given-date (second %) stock-performance (first %)) cash-invested-by-dates))
      ]
  
  (println "Cash:" cash)
  (println "Portfolio:" portfolio)
  (println cash-invested)
  (println "Portfolio Return: " (calculate-portfolio-return cash-invested stock-performance))
  (println "Returns by date: ")
  (println returns-by-date)
  (println "Portfolio Value:")
  ;(run! println sorted-portfolio-value)
  (println "Current Portfolio Value:" (+ cash current-value))
  ;(println "Individual Stocks Performance:")
  ;(run! println stock-performance)
)
;; @@
;; ->
;;; Cash: -127382.8006363135
;;; Portfolio: {IEF 100.0, SHY 170.0, RSP 35.0, SPY 150.0, GSG 250.0, AGG 300.0}
;;; {IEF 9070.99990845, SHY 13787.499618528002, RSP 4944.7999572755, SPY 65155.500793454994, GSG 5449.999809275, AGG 28974.00054933}
;;; Portfolio Return:  0.09264652407481558
;;; Returns by date: 
;;; {2023-10-13 0.0718271991092975, 2023-11-3 0.12948711521584647, 2023-12-1 0.11553343160721412}
;;; Portfolio Value:
;;; Current Portfolio Value: 12169.599113444478
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; 
;; **

;; @@

;; @@
