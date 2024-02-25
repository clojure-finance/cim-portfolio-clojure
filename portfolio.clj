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
(defn calculate-returns [prices]
  (let [price-changes (map #(double (/ (second %) (first %))) (partition 2 1 prices))
        stock-returns (reductions * price-changes)	;; TO BE DELETED
        arithmetic-returns (mapv #(- % 1.0) price-changes)
        log-returns (mapv #(Math/log %) price-changes)
        cumulative-log-return (reduce + log-returns)]
    {
     :cumulative-return cumulative-log-return
     :arithmetic-returns arithmetic-returns
     :log-returns log-returns
     }))

(defn calculate-portfolio-return [cash-invested returns]
  (let [total-investment (apply + (vals cash-invested))]
    (->> cash-invested
         (map (fn [[security invested]]
                (let [weight (/ invested total-investment)
                      return (:cumulative-return (get returns security))]
                  (* weight return))))
         (apply +))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/calculate-portfolio-return</span>","value":"#'portfolio/calculate-portfolio-return"}
;; <=

;; @@
(calculate-returns [100 110 120 130])
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:cumulative-return</span>","value":":cumulative-return"},{"type":"html","content":"<span class='clj-double'>0.262364264467491</span>","value":"0.262364264467491"}],"value":"[:cumulative-return 0.262364264467491]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:arithmetic-returns</span>","value":":arithmetic-returns"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.10000000000000009</span>","value":"0.10000000000000009"},{"type":"html","content":"<span class='clj-double'>0.09090909090909105</span>","value":"0.09090909090909105"},{"type":"html","content":"<span class='clj-double'>0.08333333333333304</span>","value":"0.08333333333333304"}],"value":"[0.10000000000000009 0.09090909090909105 0.08333333333333304]"}],"value":"[:arithmetic-returns [0.10000000000000009 0.09090909090909105 0.08333333333333304]]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:log-returns</span>","value":":log-returns"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.09531017980432493</span>","value":"0.09531017980432493"},{"type":"html","content":"<span class='clj-double'>0.0870113769896299</span>","value":"0.0870113769896299"},{"type":"html","content":"<span class='clj-double'>0.08004270767353615</span>","value":"0.08004270767353615"}],"value":"[0.09531017980432493 0.0870113769896299 0.08004270767353615]"}],"value":"[:log-returns [0.09531017980432493 0.0870113769896299 0.08004270767353615]]"}],"value":"{:cumulative-return 0.262364264467491, :arithmetic-returns [0.10000000000000009 0.09090909090909105 0.08333333333333304], :log-returns [0.09531017980432493 0.0870113769896299 0.08004270767353615]}"}
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
         data (rest data)]
    (if (empty? data)
      [cash portfolio portfolio-value current-value cash-invested stock-performance]
      (let [[date action amount ticker] (first data)
            ticker-prices (get-ticker-price-all ticker (parse-date date))]
        (cond
          (= action "buy")
          (if (pos? (Double. amount))
            (let [price (second (first ticker-prices))              ; Gets open price of next trading day
                  currPrice (nth (last ticker-prices) 2)
                  prices (mapv #(nth % 2) ticker-prices)                ; Extracts the prices from ticker-prices
                  amounts (repeatedly (count prices) #(Double. amount))]  ; Creates a sequence of amounts matching the number of prices
              (recur (- cash (* (Double. amount) price))
                     (assoc portfolio ticker (+ (get portfolio ticker 0) (Double. amount)))
                     (merge-with + portfolio-value (zipmap (map first ticker-prices) (map #(- % (* (Double. amount) price))(map * prices amounts)))) ; Updates portfolio-value with the calculated values
                     (+ current-value (* (Double. amount) currPrice))
                     
                     (assoc stock-performance ticker (calculate-returns prices))
                     (assoc cash-invested ticker (+ (get cash-invested ticker 0) (* (Double. amount) price)))
                     (rest data)))
            (recur cash portfolio portfolio-value current-value cash-invested stock-performance (rest data)))

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
                   (rest data)))
          )))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;portfolio/calculate-portfolio-new</span>","value":"#'portfolio/calculate-portfolio-new"}
;; <=

;; @@
(def input-file "testPortfolio.csv")

(let [data (read-csv input-file)
      [cash portfolio portfolio-value current-value cash-invested stock-performance] (calculate-portfolio-new data)
      sorted-portfolio-value (sort-map-by-date portfolio-value)
      ]
  (println "Cash:" cash)
  (println "Portfolio:" portfolio)
  (println cash-invested)
  (println "Portfolio Return: " (calculate-portfolio-return cash-invested stock-performance))
  (println "Portfolio Value:")
  (run! println sorted-portfolio-value)
  (println "Current Portfolio Value:" (+ cash current-value))
  (println "Individual Stocks Performance:")
  (run! println stock-performance)
)

;; @@
;; ->
;;; Cash: -127382.8006363135
;;; Portfolio: {IEF 100.0, SHY 170.0, RSP 35.0, SPY 150.0, GSG 250.0, AGG 300.0}
;;; {IEF 9070.99990845, SHY 13787.499618528002, RSP 4944.7999572755, SPY 65155.500793454994, GSG 5449.999809275, AGG 28974.00054933}
;;; Portfolio Return:  0.09264652407481555
;;; Portfolio Value:
;;; [2023-10-16 -65.46310425049933]
;;; [2023-10-17 -134.64645385699646]
;;; [2023-10-18 -833.6418151854989]
;;; [2023-10-19 -1312.1301269474925]
;;; [2023-10-20 -1840.3173828214997]
;;; [2023-10-23 -1900.8222961474976]
;;; [2023-10-24 -1539.3357849114964]
;;; [2023-10-25 -2277.452850342493]
;;; [2023-10-26 -2714.7685241679933]
;;; [2023-10-27 -2940.563964846995]
;;; [2023-10-30 -2445.206832892497]
;;; [2023-10-31 -2165.6020355219966]
;;; [2023-11-01 -1599.725189209996]
;;; [2023-11-02 -631.9567871165011]
;;; [2023-11-03 -87.25875854299557]
;;; [2023-11-06 -302.2648620794987]
;;; [2023-11-07 -218.28636168749244]
;;; [2023-11-08 -215.16426086849947]
;;; [2023-11-09 -901.5673256040029]
;;; [2023-11-10 229.94455335900375]
;;; [2023-11-13 248.51984023549971]
;;; [2023-11-14 1843.9553451515085]
;;; [2023-11-15 1875.395259843007]
;;; [2023-11-16 1880.3558158684991]
;;; [2023-11-17 2079.2668151945]
;;; [2023-11-20 2702.467975601009]
;;; [2023-11-21 2590.4751968219916]
;;; [2023-11-22 2821.8345832775003]
;;; [2023-11-24 2768.4314918309965]
;;; [2023-11-27 2669.254760736002]
;;; [2023-11-28 2875.0359726080087]
;;; [2023-11-29 2975.5102157394967]
;;; [2023-11-30 3134.9549484349973]
;;; [2023-12-01 3696.039180749509]
;;; [2023-12-04 3035.134220085507]
;;; [2023-12-05 3210.8949661244933]
;;; [2023-12-06 2907.726840942996]
;;; [2023-12-07 3481.3296127255126]
;;; [2023-12-08 3607.291088087507]
;;; [2023-12-11 3918.779964430002]
;;; [2023-12-12 4248.994693726993]
;;; [2023-12-13 5942.616310083492]
;;; [2023-12-14 6681.745395638493]
;;; [2023-12-15 6452.067279802508]
;;; [2023-12-18 6810.514869657502]
;;; [2023-12-19 7395.8018493485]
;;; [2023-12-20 6438.512935621995]
;;; [2023-12-21 7191.618270836993]
;;; [2023-12-22 7289.887485484503]
;;; [2023-12-26 7752.918033589004]
;;; [2023-12-27 8132.3781585305005]
;;; [2023-12-28 7992.216987603996]
;;; [2023-12-29 7659.874324750503]
;;; [2024-01-02 7016.677532168]
;;; [2024-01-03 6497.417640650995]
;;; [2024-01-04 6060.8387184185]
;;; [2024-01-05 6100.4886054879935]
;;; [2024-01-08 7220.397491406502]
;;; [2024-01-09 7135.317878695995]
;;; [2024-01-10 7434.969787580001]
;;; [2024-01-11 7695.802650423999]
;;; [2024-01-12 7851.6443252125]
;;; [2024-01-16 7195.804405183504]
;;; [2024-01-17 6623.303527812997]
;;; [2024-01-18 7292.361850715509]
;;; [2024-01-19 8216.821041094503]
;;; [2024-01-22 8525.573310830492]
;;; [2024-01-23 8664.945907597006]
;;; [2024-01-24 8661.582183806991]
;;; [2024-01-25 9382.964611024]
;;; [2024-01-26 9279.830665580499]
;;; [2024-01-29 10022.002181985003]
;;; [2024-01-30 10072.382049507496]
;;; [2024-01-31 8974.659900631996]
;;; [2024-02-01 10160.60184475599]
;;; [2024-02-02 10417.499637570498]
;;; [2024-02-05 9761.697864526004]
;;; [2024-02-06 10266.952133140998]
;;; [2024-02-07 10857.050037385497]
;;; [2024-02-08 10855.401668511495]
;;; [2024-02-09 11272.9494094515]
;;; [2024-02-12 11304.451560965495]
;;; [2024-02-13 9750.647220591009]
;;; [2024-02-14 10601.450405108997]
;;; [2024-02-15 11335.002307863497]
;;; [2024-02-16 10808.450889568998]
;;; [2024-02-20 10407.001686074993]
;;; [2024-02-21 10379.298171976005]
;;; [2024-02-22 12019.250183071501]
;;; [2024-02-23 12169.599113444496]
;;; Current Portfolio Value: 12169.599113444478
;;; Individual Stocks Performance:
;;; [IEF {:cumulative-return 0.047020217534437386, :arithmetic-returns [-0.009812536787861315 -0.004676558638253225 -0.004922345151818219 0.005846030426263971 0.004694330597632801 0.0017800342387754853 -0.008772841910434148 0.007617977819883048 0.001556685635651034 -0.0033304602002024453 -0.00211630548213515 0.011517799601412193 0.0061967413019519135 0.007698180441086722 -0.005784172068228566 0.005708069036951757 0.00403844917222651 -0.009892350166608277 0.0010978685508153951 -5.483744553884673E-4 0.01503350160469541 -0.007783789051861167 0.007517944491946427 6.488526227954594E-4 0.0014050050190426155 0.0012949612223283058 -3.232179947427749E-4 -0.005175316006539754 0.007586383553730425 0.004410077658143274 0.005247392797287587 -0.004900372689591537 0.00943525793096911 -0.004147209417087683 0.007047500288305253 0.004135255267995186 -0.0014783393837696224 -0.006450936202131885 1.0654944255072962E-4 0.0021285575965548365 0.014868255239316674 0.008312513586123282 -8.327177511470163E-4 -0.00218774567579072 0.0018793676874468979 0.004897864387998441 -0.0015555564374147846 -9.347244281577849E-4 6.237591407396437E-4 0.007792168577578362 -0.0038144339160011143 -0.002483675650102546 -0.0037348688263346697 0.002395100560294461 -0.006025366668987786 -0.003657949946785699 0.0033567266589284994 -4.181870926038833E-4 -0.0015688976691697887 0.005342537980459472 0.002709232333332423 -0.008105657198695049 -0.0032477009803190127 -0.00220729132803954 1.0537999469195647E-4 0.0030545700490287597 -0.0025202689252351584 -0.002842416000321202 0.004328543980924193 -0.001787050444192806 0.006002507219167397 0.0013609354762789128 0.00836286720589996 0.006193887984795365 -0.013013862220324635 -0.009732108925910032 0.005389434122399006 -0.0017868212107492454 -0.0032641629421442664 -0.0017959623042784756 9.525237520879859E-4 -0.010995991705067043 0.0037416984249998286 0.0027692207688818637 -0.004354791879501052 0.0017068878359958184 -0.0031949206275277975 1.0686043229757303E-4 0.004593529561971232], :log-returns [-0.00986099699929556 -0.004687527951052601 -0.004934499795353057 0.0058290086978680395 0.00468334658938312 0.0017784518553493824 -0.008811549840007378 0.007589107556166087 0.0015554752565239665 -0.0033360185273979322 -0.0021185480210722564 0.01145197470406366 0.006177620451224091 0.00766870064722255 -0.0058009651789573975 0.005691839740229422 0.004030316524538231 -0.009941604559516653 0.0010972663338676902 -5.485248676508038E-4 0.01492161845613696 -0.00781424086109672 0.007489825590209141 6.486422089458709E-4 0.0014040189230293504 0.0012941234831931098 -3.2327024093708106E-4 -0.005188754339555037 0.007557751663174989 0.004400381761653773 0.0052336732054991016 -0.004912418885853634 0.009391023906797753 -0.004155832940692692 0.00702278272157307 0.004126728598483446 -0.0014794332055959266 -0.006471833410592538 1.0654376656205439E-4 0.0021262954273700836 0.014758806275511353 0.008278154918345227 -8.330646531679042E-4 -0.0021901422874508955 0.001877603885537046 0.0048859088719889554 -0.0015567675714843238 -9.351615554530182E-4 6.235646838654488E-4 0.007761966424064928 -0.0038217274220229444 -0.0024867650889728947 -0.003741860863901081 0.002392236878573153 -0.006043592438912409 -0.003664656605769464 0.0033511054277756123 -4.1827455721132347E-4 -0.0015701296778836324 0.005328317251792621 0.0027055689785089924 -0.008138686642657725 -0.0032529862074751293 -0.0022097309862283325 1.0537444261036463E-4 0.0030499143283437944 -0.0025234501491134547 -0.002846463335974468 0.004319202780590196 -0.0017886491237357497 0.0059845639400157285 0.0013600102429537182 0.008328092176732709 0.00617478470242361 -0.013099284450045157 -0.009779775413743095 0.005374963092808411 -0.0017884194799334513 -0.0032695019434132613 -0.0017975769781295333 9.520703892086068E-4 -0.011056894491062536 0.0037347156842344274 0.0027653935410462144 -0.0043643016042819085 0.0017054327584875792 -0.003200035283287418 1.0685472312829686E-4 0.004583011502763797]}]
;;; [SHY {:cumulative-return 0.016650490275916415, :arithmetic-returns [7.4022444030164E-4 -2.4668552528706034E-4 -0.001479434034839966 2.469218205114476E-4 3.702437696484484E-4 0.003949167193156189 -0.0018438222623592404 0.0017240543887091508 -7.375817285456554E-4 1.230526013789035E-4 6.149978784448784E-4 1.2296184126903142E-4 -7.37491046529426E-4 0.0012300904740187857 0.0018430107167957122 0.001962186086081008 -6.119116464815555E-4 0.0029114954414817262 -0.0014699976709644513 0.0012268642311552558 -2.451850349920459E-4 7.353592076344562E-4 -0.0022042685487756453 2.4551212535506295E-4 1.227259325904395E-4 0.005029921487471389 0.0017145080352487518 -0.0011002638596874226 -1.223966028478518E-4 3.6723475560251906E-4 0.0013458458771700155 9.775239609610686E-4 3.661551241322947E-4 -4.8796588882527026E-4 0.001220790544598671 -1.2194886472760569E-4 4.87854949821509E-4 -0.0012189493882045488 2.440127004894599E-4 -6.099296121320208E-4 -2.4428889465499815E-4 8.547995660008745E-4 2.4413989562410698E-4 0.0 0.002317596173807779 0.002068814529901042 -0.001578795792817056 -0.0019461558285480685 4.873711100970013E-4 -7.308869699232945E-4 4.8767655876180704E-4 -1.2185971095457493E-4 -4.8749825388128E-4 0.0018289167941973883 -6.084639053228758E-4 9.740977036680754E-4 -6.08241873264026E-4 0.0018259293140521216 0.0014867733797609972 -0.0031638127930689874 -0.0019530766147005663 0.0015899859344847833 -3.663301920090456E-4 -2.4437176094338486E-4 -3.6655401566909607E-4 1.2226056103759042E-4 -0.0031777332824474147 0.0014713431852884096 9.79454342569408E-4 -0.0012230732814001444 6.122387969376142E-4 -8.566845635086251E-4 -1.225151430032101E-4 4.900271572447767E-4], :log-returns [7.399506093132245E-4 -2.4671595716609237E-4 -0.0014805294779288531 2.468913403361023E-4 3.7017524633699766E-4 0.003941389702080267 -0.001845524194989437 0.0017225699129079755 -7.378538757775767E-4 1.230450310285783E-4 6.148088447492097E-4 1.2295428208148202E-4 -7.377631268306968E-4 0.0012293345325857157 0.001841314456375577 0.0019602635135154227 -6.120989408220616E-4 0.0029072652474335806 -0.0014710791775454782 0.001226112248225543 -2.452150977567984E-4 7.350889635285421E-4 -0.0022067015246388373 2.454819921851529E-4 1.2271840237926957E-4 0.005017313692192931 0.0017130399441437514 -0.0011008695943206023 -1.2240409392330718E-4 3.671673414237058E-4 0.0013449410383655307 9.77046495544532E-4 3.6608810570375877E-4 -4.8808498292374637E-4 0.0012200459857271274 -1.2195630109498705E-4 4.877359872848787E-4 -0.0012196929112828004 2.4398293423259117E-4 -6.10115694866659E-4 -2.4431873804739586E-4 8.54434432914111E-4 2.4411009832949626E-4 0.0 0.002314914690060495 0.002066677480053745 -0.0015800434042167622 -0.0019480520504308243 4.8725238337194023E-4 -7.311541980216535E-4 4.875576831958052E-4 -1.2186713645240404E-4 -4.8761711978789974E-4 0.0018272463622878746 -6.086490946093907E-4 9.736235783711912E-4 -6.084269273944738E-4 0.0018242643315742799 0.0014856692265012416 -0.003168828230160245 -0.0019549863558176177 0.0015887232451105586 -3.66397307305241E-4 -2.444016245874781E-4 -3.6662121301377033E-4 1.222530878243111E-4 -0.003182792998650305 0.001470261820479837 9.789749901418293E-4 -0.0012238218459544364 6.120514552267189E-4 -8.570517274400607E-4 -1.2252264859638145E-4 4.899071331458027E-4]}]
;;; [RSP {:cumulative-return 0.13538892999556398, :arithmetic-returns [0.0049283428562936216 -0.01583312460154851 -0.012386035886001956 -0.01246938576797263 -0.006349865822438572 0.006390444286201724 -0.011021088029947501 -0.002066349279391644 -0.011536743960080309 0.007855767895236454 0.00831409574332631 0.003386565724487589 0.022158621145486945 0.016366345761646084 -0.005226270113764309 -0.0021298983502100466 -0.0016364728437322507 -0.008480595305168759 0.011715578558659567 -0.001278712677073024 0.028524669726761376 0.004702957054223855 -0.0033042135130092465 0.0048346325454684536 0.003986540752963341 -0.0018484472507178662 0.004663821865015461 0.002935684641449665 -0.0020421448225000915 -9.548814090616364E-4 0.003959867734763645 0.008500506169843547 0.015037061207298974 4.6505839220034595E-4 -0.008897812322213317 3.3498169505663533E-4 0.005291123084931515 0.003131246713092395 0.008899483027543065 0.001777285056481892 0.02122495932304025 0.013641300095027953 -0.007109705476848416 4.4277623965105484E-4 0.008404424197325566 -0.016541487249352227 0.012291330533661382 0.0033870066064096704 0.005732177484160106 9.498695017509107E-4 0.00221438763068571 -0.0038507707606723063 3.16876113643616E-4 -0.014760859936060133 -0.0016718734475109631 0.0028984427606704166 0.01072505360537579 -0.0050832572555564726 0.0017243854211423937 -0.0028690608191818123 -0.0022377932120101196 -0.0063441555516751125 -0.007867930090989073 0.0051352610745307725 0.007695805495847585 0.005262431182498473 5.107369216785784E-4 -0.005168437674137882 0.010069861000084446 1.2702738787595536E-4 0.0060952807231744455 1.8931451488679585E-4 -0.012808370931548074 0.01163231488752281 -8.845046170059545E-4 -0.008599978598390234 0.0063146160282450126 0.003929740130000692 0.0022097740068742766 0.0011968450045705126 0.0067954563692902425 -0.016623889098296152 0.009151491113720267 0.012406331028369966 -0.004540904158024994 -0.002312035993390693 0.002004209307656213 0.009938815096659281 0.0026613397060581967], :log-returns [0.004916238328523714 -0.01595980748887736 -0.012463382167981207 -0.012547780933452792 -0.006370111972818776 0.006370111972818779 -0.01108227016506324 -0.0020684871245948084 -0.011603808493883287 0.007825072005678045 0.008279724031036401 0.003380844224635722 0.02191668633849519 0.01623386070124646 -0.005239974833982689 -0.0021321698095926726 -0.0016378133280601323 -0.008516760165051282 0.01164748250856342 -0.0012795309277409924 0.028125415945488872 0.0046919327028850325 -0.003309684481301517 0.0048229832411924856 0.00397861555514276 -0.0018501577374919309 0.004652979944609221 0.002931383934250102 -0.0020442328434164864 -9.553375987421391E-4 0.00395204809488733 0.008464580315665873 0.014925125331876921 4.6495028606208054E-4 -0.008937634248949375 3.3492560121521425E-4 0.005277174274795091 0.0031263545697813727 0.008860116020075052 0.0017757075542334433 0.021002847256742518 0.013549095144805071 -0.0071350998688487315 4.426782431777946E-4 0.008369303665642343 -0.016679825316125302 0.012216405458327109 0.0033812836184249967 0.005715811068486574 9.494186611862804E-4 0.0022119394878216308 -0.0038582040671736647 3.168259190112905E-4 -0.01487088548597602 -0.0016732725875974444 0.002894250374426028 0.010667948161071994 -0.0050962209582705205 0.0017229003755509515 -0.0028731844633865665 -0.0022403008129333267 -0.0063643652274007175 -0.007899045570018066 0.00512212058873021 0.007666343842294373 0.005248632978420179 5.10606539968942E-4 -0.005181840248324833 0.010019497768096174 1.2701932058049113E-4 0.006076779641090243 1.8929659715537787E-4 -0.012891105335712631 0.011565179636720711 -8.848960220314284E-4 -0.008637171808403565 0.006294762375416026 0.003922038870627983 0.0022073360471929044 0.001196129356544292 0.006772471326140993 -0.016763616650690195 0.009109869956636936 0.012330003154433035 -0.004551245380854118 -0.0023147128754357617 0.0020022035596937997 0.009889749905961023 0.002657804612210721]}]
;;; [SPY {:cumulative-return 0.15730137929109628, :arithmetic-returns [0.0028460430522194713 7.32423587038955E-4 -0.007798764845237005 0.01560479152559413 -9.531553957361494E-4 0.019400732331508852 0.002117024625421182 0.0012231175623509394 0.0012438099594762608 0.007697610067772587 -0.0021794141485638185 0.0038608712700316516 6.152908358014741E-4 -0.0018009256999824697 9.90071307458873E-4 -7.033767879147668E-4 0.003937449697546391 0.0059159075995314225 -0.0052494012898316456 -1.9712312964526024E-4 -0.00402974153116642 0.0076304106829958585 0.004299131326959582 0.003889555195607519 0.004567245799865338 0.013790107366696613 0.0032093596598081753 -0.0016464810498708626 0.00562507132280432 0.006080884610837778 -0.013857271129320892 0.009481916774448873 0.0020096925427706136 0.004222527236930729 0.0018080855253561623 3.777311538293304E-4 -0.0028949734119285697 -0.005596355380200202 -0.008166688673321487 -0.003221079242467173 0.0013696598400785298 0.014275928543569627 -0.0015170695563433423 0.005655424681745735 -4.406401212351252E-4 6.927397251430545E-4 -0.0036712260361013715 -0.00555868058790332 0.008892802332278738 0.012466164165333193 0.002114336891747559 0.0029164817643987373 0.0010931594951388846 0.005438892542551255 -0.001270403702130607 0.007919380649962893 -7.734532411859396E-4 -0.016317320635126698 0.013088152875027292 0.010527378920671016 -0.003641181932388049 0.0029033057125988915 0.008340408570326252 4.416808231362257E-4 0.0057794285609051865 -4.3894895319585014E-4 -0.013773053340822128 0.009087639398328351 0.006899738032502878 -0.004979980381600968 -0.0055053951797491285 9.058331601861092E-4 0.020695498324130535 6.896671990148651E-4], :log-returns [0.0028420007396061375 7.32155495779784E-4 -0.007829334251255662 0.015484288763206261 -9.536099371957432E-4 0.019214937318198833 0.002114786886464637 0.0012223701634410183 0.0012430370686884033 0.007668134631041688 -0.0021817925278566244 0.0038534372349743255 6.151016220055283E-4 -0.0018025493163053065 9.895815101248118E-4 -7.036242734248356E-4 0.00392971823068207 0.005898477328300651 -0.005263227805294377 -1.9714256096299755E-4 -0.004037882818453296 0.007601446345948923 0.004289916463042864 0.0038820104333158276 0.004556847581558776 0.013695889035230372 0.0032042206574308288 -0.0016478379894493982 0.00560930968836571 0.00606247064311588 -0.013954179408624163 0.009437245558753216 0.0020076758122658514 0.004213637385105742 0.0018064529063696012 3.7765983137695336E-4 -0.0028991719525232646 -0.00561207364774271 -0.008200218753195874 -0.003226278085134916 0.0013687227116402957 0.014174987030624494 -0.0015182214715337833 0.005639492807034655 -4.40737231621549E-4 6.924998917346867E-4 -0.003677981525425893 -0.0055741875450220505 0.008853494233171787 0.012389101331846836 0.0021121048271722505 0.002912237082479941 0.0010925624313823062 0.005424155178981427 -0.0012712113490113235 0.007888186936719303 -7.737525104679045E-4 -0.01645191425809414 0.013003243074688416 0.010472351923656018 -0.0036478271712403784 0.00289909926034627 0.00830581955403764 4.415833108732703E-4 0.00576279173378502 -4.3904531948856E-4 -0.013868781838792513 0.009046595278777509 0.006876043767090116 -0.004992421806456669 -0.005520605720170145 9.054231409164149E-4 0.020484256036606786 6.89429487880269E-4]}]
;;; [GSG {:cumulative-return -0.03763375569322879, :arithmetic-returns [-0.028162539916786966 -0.013301605199420252 -0.0048146547566610964 0.009675895605308682 0.016770410164172134 -0.0018849674596709276 -0.007082134864897616 -0.029006209718703713 0.018609165493639557 0.013461571964895835 0.004743851131971288 -0.009915060848627255 -0.010014262821926079 -0.01011565459944741 0.015085225538127522 0.010067069906762205 -0.018509700771198845 -0.009187646581041209 -0.007808679804168106 -0.007378239993259261 -0.02775022088853485 0.0045870663328508865 0.01065454092106699 -0.002008078079657749 -0.02062373564926767 0.01078587566541267 0.02032518355166446 0.003486040590935602 0.008436728512089875 0.00984255738208395 -0.010233966238542336 0.004923701828151161 -0.003919643522691696 0.014264678631991767 -0.006789587357040494 -0.014648400571583897 -0.0059465231657642725 -0.010468548853003523 0.02015111389482871 -0.00296293658765423 0.006934095388632988 -0.018691547576299317 0.011528798671913698 -0.007928634538165613 0.01148848835866989 0.00197535385185188 -0.007392879208073855 9.930714203660873E-4 0.010416715941943933 -9.818584622496296E-4 0.007370988485895236 0.004390251346341412 0.008742122577268141 0.013962397771209112 0.009021867944851936 -0.010823507868235382 0.008563287401509356 -0.012264161294512799 -0.014326701855210566 -0.013081325503141561 0.004909105060396657 0.005862278613827154 0.006313703731740228 0.014961456731300116 0.001901997474754502 0.0018984772747705492 -0.001421159916273007 -0.010910794261503542 0.007673853451914958 0.0047596563736560515 -0.00521082933298489 0.004285721552380828 0.003319094086937424 -0.014177657805458987], :log-returns [-0.028566710641490453 -0.013390863956682996 -0.0048262825444095716 0.009629383914920582 0.016631339529631396 -0.0018867462464879248 -0.007107332219875714 -0.029435205890152954 0.018438133556334658 0.013371770024229982 0.004732634529463852 -0.009964542411265324 -0.010064742848116112 -0.010167165505544133 0.01497257501621569 0.010016734496676756 -0.0186831489325989 -0.009230113318943621 -0.007839327192324151 -0.007405593837833219 -0.028142533139450806 0.004576577806227435 0.010598181270648903 -0.0020100969716259805 -0.020839374895549333 0.010728123012639255 0.020121383891490447 0.0034799784359617844 0.008401338230906616 0.009794434921913937 -0.010286693317607459 0.004911620050123826 -0.003927345457845315 0.014163895399983056 -0.006812741469372832 -0.014756747768443363 -0.005964274140683421 -0.010523729556321616 0.019950767200205326 -0.0029673347741123323 0.006910165108745279 -0.01886844231070775 0.011462848472874522 -0.007960233295140753 0.011422996798341073 0.0019734054059233758 -0.007420341975900774 9.925786511528114E-4 0.01036283580283011 -9.823408010210248E-4 0.007343955508829936 0.004380642306700247 0.008704131478363724 0.01386582141552881 0.008981414025470508 -0.010882508142363245 0.008526830435752482 -0.012339986715216493 -0.014430319908880582 -0.01316763960316461 0.0048970946948756365 0.0058451623196337016 0.00629385580315345 0.014850638108989104 0.0019001909678425396 0.0018966774443871701 -0.0014221707218178704 -0.010970753511444631 0.007644559209548679 0.004748365023713856 -0.00522445305201195 0.00427656400291679 0.003313598052038659 -0.01427912094400753]}]
;;; [AGG {:cumulative-return 0.014912242813570491, :arithmetic-returns [0.006634923138713722 0.0025746259805223826 3.0814279734792116E-4 -0.005031842646307627 1.0328112441593262E-4 0.002992784213944999 0.01255271686291115 0.008071004817351124 -0.0023250008679999157 -0.002026633427969937 0.001218464945152542 0.003650713490837454 -1.0105272712046531E-4 -0.0013136635625673465 0.0020236852553361384 0.006361769175912579 -0.002207537383141722 -0.001910696082177421 -0.004735539304253744 5.061580238909791E-4 -0.004047370509657311 -0.0023367304988443083 0.0037678791915713994 -2.0284797616798578E-4 -0.0019280290985788096 0.005693347909399904 0.0018197138810915892 -0.007467242835277443 -0.002745033422378307 -9.174777842479998E-4 2.0402780389616737E-4 0.0018363440621189664 -0.0024439707779247977 -0.0022457691664466983 0.004501726284745766 -0.0011203684068838982 0.0039767557150016675 0.0019297138077105647 0.004561580444034874 0.005698012738180269 -0.009157730068907854 -0.00822666632268454 0.005222756049468424 -0.002037536370602444 -0.003062427778722454 -3.0725381501939264E-4 2.04898832969036E-4 -0.009216605343946527 0.00444444759896645 0.0023667766636927823 -0.0031824779421562033 9.269203781090418E-4 -0.0029838554194383526 8.256122780205821E-4 0.003712033084543931], :log-returns [0.006613008915569915 0.0025713173088970924 3.080953311068123E-4 -0.005044544995245974 1.032757912878068E-4 0.002988314750475406 0.012474584680611184 0.008038608455360603 -0.0023277078792009035 -0.002028689828345185 0.001217723219191509 0.003644065810612713 -1.0105783329129245E-4 -0.001314527174958764 0.002021640362677808 0.006341618539698403 -0.0022099775856776792 -0.0019125237904377274 -0.004746787495388037 5.060299691272131E-4 -0.004055583281255261 -0.00233946491411465 0.003760798515285364 -2.0286855260134246E-4 -0.0019298901391596197 0.005677202057837345 0.0018180602076248566 -0.007495262265462351 -0.0027488079356490958 -9.17898924601277E-4 2.0400699305439814E-4 0.0018346600436713986 -0.002446962149383358 -0.002248294687888451 0.004491623822640419 -0.0011209964887333359 0.003968869323248021 0.0019278543018458642 0.0045512079672578145 0.005681840467717829 -0.009199919851542069 -0.0082606920829585 0.005209164761208568 -0.0020396149717968313 -0.0030671266063380946 -3.0730102714379915E-4 2.0487784407017826E-4 -0.009259341039012672 0.0044346002084296844 0.0023639802592455915 -0.003187552795014233 9.26491052695199E-4 -0.00298831599136475 8.252716476766089E-4 0.0037051604920101465]}]
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; 
;; **
