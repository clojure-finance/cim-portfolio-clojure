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
(yf/download "AAPL" "2024-06-25" :progress false)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>                  Open        High  ...   Adj Close    Volume\nDate                                ...                      \n2024-06-25  209.149994  211.380005  ...  209.070007  56713900\n2024-06-26  211.500000  214.860001  ...  213.250000  66213200\n2024-06-27  214.690002  215.740005  ...  214.100006  49718000\n2024-06-28  215.804993  216.070007  ...  213.160095  34274691\n\n[4 rows x 6 columns]</span>","value":"                  Open        High  ...   Adj Close    Volume\nDate                                ...                      \n2024-06-25  209.149994  211.380005  ...  209.070007  56713900\n2024-06-26  211.500000  214.860001  ...  213.250000  66213200\n2024-06-27  214.690002  215.740005  ...  214.100006  49718000\n2024-06-28  215.804993  216.070007  ...  213.160095  34274691\n\n[4 rows x 6 columns]"}
;; <=

;; @@
(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
from currency_converter import CurrencyConverter

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
    stock = yf.Ticker(ticker)

    if stock.info['currency'] != 'USD':
        c = CurrencyConverter()
        fx_to_usd = c.convert(1, stock.info['currency'], 'USD')
    else:
        fx_to_usd = 1
    data['Open'] = data['Open'] * fx_to_usd
    data['Adj Close'] = data['Adj Close'] * fx_to_usd
    return data[['Date', 'Open', 'Adj Close']].to_json(orient = 'values')"))

(def get-ticker-price-all-wrapper (:get_ticker_price_all (:globals pythonWrapper)))

(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date))
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.yfinanceclient/get-ticker-price-all</span>","value":"#'cim_portfolio.yfinanceclient/get-ticker-price-all"}
;; <=

;; @@
;; Test if function is working + price is converted to USD

(get-ticker-price-all "3330.HK" "2024-06-25")
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-06-26&quot;</span>","value":"\"2024-06-26\""},{"type":"html","content":"<span class='clj-double'>0.3803823471</span>","value":"0.3803823471"},{"type":"html","content":"<span class='clj-double'>0.3791015997</span>","value":"0.3791015997"}],"value":"[\"2024-06-26\" 0.3803823471 0.3791015997]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-06-27&quot;</span>","value":"\"2024-06-27\""},{"type":"html","content":"<span class='clj-double'>0.3752593575</span>","value":"0.3752593575"},{"type":"html","content":"<span class='clj-double'>0.3637326002</span>","value":"0.3637326002"}],"value":"[\"2024-06-27\" 0.3752593575 0.3637326002]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2024-06-28&quot;</span>","value":"\"2024-06-28\""},{"type":"html","content":"<span class='clj-double'>0.3714171152</span>","value":"0.3714171152"},{"type":"html","content":"<span class='clj-double'>0.3778208523</span>","value":"0.3778208523"}],"value":"[\"2024-06-28\" 0.3714171152 0.3778208523]"}],"value":"[[\"2024-06-26\" 0.3803823471 0.3791015997] [\"2024-06-27\" 0.3752593575 0.3637326002] [\"2024-06-28\" 0.3714171152 0.3778208523]]"}
;; <=
