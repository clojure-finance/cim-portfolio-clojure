;;; # Clojure Wrapper over Python's yfinance API 
;;; ### Requires python, yfinance etc. to be installed on local machine
(ns cim_portfolio.yfinanceclient
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [nextjournal.clerk :as clerk]
            [clojure.data.json :as json]
  )
)

;; (require-python '[yfinance :as yf]
;;                 '[datetime :as dt])


;; Test if yfinance working through clojure-python wrapper
;; (yf/download "AAPL" "2025-01-15" :progress false :auto_adjust false)

(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
from currency_converter import CurrencyConverter

def get_ticker_price_all(ticker, date):
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date, progress=False, auto_adjust=False)
        if len(data) > 0:
            break
        date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
        if count > 10:
            return 'ERROR'
    data.reset_index(inplace=True)
    data['Date'] = data['Date'].dt.strftime('%Y-%m-%d')
    stock = yf.Ticker(ticker)

    if 'currency' in stock.info and stock.info['currency'] != 'USD':
        c = CurrencyConverter()
        fx_to_usd = c.convert(1, stock.info['currency'], 'USD')
    else:
        fx_to_usd = 1
    data['Open'] = data['Open'] * fx_to_usd
    data['Close'] = data['Close'] * fx_to_usd
    return data[['Date', 'Open', 'Close']].to_json(orient = 'values')"))

(def get-ticker-price-all-wrapper (:get_ticker_price_all (:globals pythonWrapper)))

(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date))
)

;; Test if function is working + price is converted to USD

(get-ticker-price-all "0700.HK" "2025-01-25")

(get-ticker-price-all "0700.HK" "2025-01-25")
