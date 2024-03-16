;; gorilla-repl.fileformat = 1

;; **
;;; # Clojure Wrapper over Python's yfinance API
;;; 
;;; ### Requires python, yfinance etc. to be installed on local machine
;; **

;; @@
(ns cim_portfolio.server
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [clojure.data.json :as json]
  )
)

(require-python '[yfinance :as yf]
                '[datetime :as dt])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-keyword'>:ok</span>","value":":ok"}
;; <=

;; @@
;; Test if yfinance working through clojure-python wrapper
(yf/download "AAPL" "2024-03-12")
;; @@
;; ->
;;; [*********************100%%**********************]  1 of 1 completed
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>                  Open        High  ...   Adj Close     Volume\nDate                                ...                       \n2024-03-12  173.149994  174.029999  ...  173.229996   59825400\n2024-03-13  172.770004  173.190002  ...  171.130005   52488700\n2024-03-14  172.910004  174.309998  ...  173.000000   72913500\n2024-03-15  171.169998  172.619995  ...  172.619995  121664700\n\n[4 rows x 6 columns]</span>","value":"                  Open        High  ...   Adj Close     Volume\nDate                                ...                       \n2024-03-12  173.149994  174.029999  ...  173.229996   59825400\n2024-03-13  172.770004  173.190002  ...  171.130005   52488700\n2024-03-14  172.910004  174.309998  ...  173.000000   72913500\n2024-03-15  171.169998  172.619995  ...  172.619995  121664700\n\n[4 rows x 6 columns]"}
;; <=

;; @@
(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
def get_ticker_price_all(ticker, date):
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date)
        if len(data) > 0:
            break
        date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
        if count > 10:
            data = 'Error'
            break
    data.reset_index(inplace=True)
    data['Date'] = data['Date'].dt.strftime('%Y-%m-%d')
    return data[['Date', 'Open', 'Adj Close']].to_json(orient = 'values')"))

(def get-ticker-price-all-wrapper (:get_ticker_price_all (:globals pythonWrapper)))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.server/get-ticker-price-all-wrapper</span>","value":"#'cim_portfolio.server/get-ticker-price-all-wrapper"}
;; <=

;; @@
(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date))
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.server/get-ticker-price-all</span>","value":"#'cim_portfolio.server/get-ticker-price-all"}
;; <=

;; @@
;; Test if function is working

(get-ticker-price-all "AAPL" "2024-03-10")
;; @@
;; ->
;;; [*********************100%%**********************]  1 of 1 completed
;;; 
;; <-
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-11&quot;</span>","value":"\"2024-03-11\""},{"type":"html","content":"<span class='clj-double'>172.9400024414</span>","value":"172.9400024414"},{"type":"html","content":"<span class='clj-double'>172.75</span>","value":"172.75"}],"value":"[\"2024-03-11\" 172.9400024414 172.75]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-12&quot;</span>","value":"\"2024-03-12\""},{"type":"html","content":"<span class='clj-double'>173.1499938965</span>","value":"173.1499938965"},{"type":"html","content":"<span class='clj-double'>173.2299957275</span>","value":"173.2299957275"}],"value":"[\"2024-03-12\" 173.1499938965 173.2299957275]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-13&quot;</span>","value":"\"2024-03-13\""},{"type":"html","content":"<span class='clj-double'>172.7700042725</span>","value":"172.7700042725"},{"type":"html","content":"<span class='clj-double'>171.1300048828</span>","value":"171.1300048828"}],"value":"[\"2024-03-13\" 172.7700042725 171.1300048828]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-14&quot;</span>","value":"\"2024-03-14\""},{"type":"html","content":"<span class='clj-double'>172.9100036621</span>","value":"172.9100036621"},{"type":"html","content":"<span class='clj-double'>173.0</span>","value":"173.0"}],"value":"[\"2024-03-14\" 172.9100036621 173.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-03-15&quot;</span>","value":"\"2024-03-15\""},{"type":"html","content":"<span class='clj-double'>171.1699981689</span>","value":"171.1699981689"},{"type":"html","content":"<span class='clj-double'>172.6199951172</span>","value":"172.6199951172"}],"value":"[\"2024-03-15\" 171.1699981689 172.6199951172]"}],"value":"[[\"2024-03-11\" 172.9400024414 172.75] [\"2024-03-12\" 173.1499938965 173.2299957275] [\"2024-03-13\" 172.7700042725 171.1300048828] [\"2024-03-14\" 172.9100036621 173.0] [\"2024-03-15\" 171.1699981689 172.6199951172]]"}
;; <=

;; @@

;; @@
