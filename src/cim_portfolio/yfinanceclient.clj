;; gorilla-repl.fileformat = 1

;; **
;;; # Clojure Wrapper over Python's yfinance API
;;; 
;;; ### Requires python, yfinance etc. to be installed on local machine
;; **

;; @@
(ns cim_portfolio.yfinanceclient
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
(yf/download "AAPL" "2024-04-15" :progress false)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>                  Open        High  ...   Adj Close    Volume\nDate                                ...                      \n2024-04-15  175.360001  176.630005  ...  172.690002  73531800\n2024-04-16  171.750000  173.759995  ...  169.380005  73711200\n2024-04-17  169.610001  170.649994  ...  168.000000  50901200\n2024-04-18  168.029999  168.639999  ...  167.039993  43122900\n2024-04-19  166.210007  166.399994  ...  165.000000  67772100\n2024-04-22  165.520004  167.259995  ...  165.839996  48116400\n2024-04-23  165.350006  167.050003  ...  166.899994  49537800\n2024-04-24  166.539993  169.300003  ...  169.020004  48251800\n2024-04-25  169.529999  170.610001  ...  169.889999  50558300\n2024-04-26  169.880005  171.339996  ...  169.300003  44525100\n\n[10 rows x 6 columns]</span>","value":"                  Open        High  ...   Adj Close    Volume\nDate                                ...                      \n2024-04-15  175.360001  176.630005  ...  172.690002  73531800\n2024-04-16  171.750000  173.759995  ...  169.380005  73711200\n2024-04-17  169.610001  170.649994  ...  168.000000  50901200\n2024-04-18  168.029999  168.639999  ...  167.039993  43122900\n2024-04-19  166.210007  166.399994  ...  165.000000  67772100\n2024-04-22  165.520004  167.259995  ...  165.839996  48116400\n2024-04-23  165.350006  167.050003  ...  166.899994  49537800\n2024-04-24  166.539993  169.300003  ...  169.020004  48251800\n2024-04-25  169.529999  170.610001  ...  169.889999  50558300\n2024-04-26  169.880005  171.339996  ...  169.300003  44525100\n\n[10 rows x 6 columns]"}
;; <=

;; @@
(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
def get_ticker_price_all(ticker, date):
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date, progress=False)
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
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.yfinanceclient/get-ticker-price-all-wrapper</span>","value":"#'cim_portfolio.yfinanceclient/get-ticker-price-all-wrapper"}
;; <=

;; @@
(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date))
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.yfinanceclient/get-ticker-price-all</span>","value":"#'cim_portfolio.yfinanceclient/get-ticker-price-all"}
;; <=

;; @@
;; Test if function is working

(get-ticker-price-all "AAPL" "2024-04-15")
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-16&quot;</span>","value":"\"2024-04-16\""},{"type":"html","content":"<span class='clj-double'>171.75</span>","value":"171.75"},{"type":"html","content":"<span class='clj-double'>169.3800048828</span>","value":"169.3800048828"}],"value":"[\"2024-04-16\" 171.75 169.3800048828]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-17&quot;</span>","value":"\"2024-04-17\""},{"type":"html","content":"<span class='clj-double'>169.6100006104</span>","value":"169.6100006104"},{"type":"html","content":"<span class='clj-double'>168.0</span>","value":"168.0"}],"value":"[\"2024-04-17\" 169.6100006104 168.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-18&quot;</span>","value":"\"2024-04-18\""},{"type":"html","content":"<span class='clj-double'>168.0299987793</span>","value":"168.0299987793"},{"type":"html","content":"<span class='clj-double'>167.0399932861</span>","value":"167.0399932861"}],"value":"[\"2024-04-18\" 168.0299987793 167.0399932861]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-19&quot;</span>","value":"\"2024-04-19\""},{"type":"html","content":"<span class='clj-double'>166.2100067139</span>","value":"166.2100067139"},{"type":"html","content":"<span class='clj-double'>165.0</span>","value":"165.0"}],"value":"[\"2024-04-19\" 166.2100067139 165.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-22&quot;</span>","value":"\"2024-04-22\""},{"type":"html","content":"<span class='clj-double'>165.5200042725</span>","value":"165.5200042725"},{"type":"html","content":"<span class='clj-double'>165.8399963379</span>","value":"165.8399963379"}],"value":"[\"2024-04-22\" 165.5200042725 165.8399963379]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-23&quot;</span>","value":"\"2024-04-23\""},{"type":"html","content":"<span class='clj-double'>165.3500061035</span>","value":"165.3500061035"},{"type":"html","content":"<span class='clj-double'>166.8999938965</span>","value":"166.8999938965"}],"value":"[\"2024-04-23\" 165.3500061035 166.8999938965]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-24&quot;</span>","value":"\"2024-04-24\""},{"type":"html","content":"<span class='clj-double'>166.5399932861</span>","value":"166.5399932861"},{"type":"html","content":"<span class='clj-double'>169.0200042725</span>","value":"169.0200042725"}],"value":"[\"2024-04-24\" 166.5399932861 169.0200042725]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-25&quot;</span>","value":"\"2024-04-25\""},{"type":"html","content":"<span class='clj-double'>169.5299987793</span>","value":"169.5299987793"},{"type":"html","content":"<span class='clj-double'>169.8899993896</span>","value":"169.8899993896"}],"value":"[\"2024-04-25\" 169.5299987793 169.8899993896]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-04-26&quot;</span>","value":"\"2024-04-26\""},{"type":"html","content":"<span class='clj-double'>169.8800048828</span>","value":"169.8800048828"},{"type":"html","content":"<span class='clj-double'>169.3000030518</span>","value":"169.3000030518"}],"value":"[\"2024-04-26\" 169.8800048828 169.3000030518]"}],"value":"[[\"2024-04-16\" 171.75 169.3800048828] [\"2024-04-17\" 169.6100006104 168.0] [\"2024-04-18\" 168.0299987793 167.0399932861] [\"2024-04-19\" 166.2100067139 165.0] [\"2024-04-22\" 165.5200042725 165.8399963379] [\"2024-04-23\" 165.3500061035 166.8999938965] [\"2024-04-24\" 166.5399932861 169.0200042725] [\"2024-04-25\" 169.5299987793 169.8899993896] [\"2024-04-26\" 169.8800048828 169.3000030518]]"}
;; <=
