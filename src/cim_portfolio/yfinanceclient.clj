;;; # Clojure Wrapper over Python's yfinance API 
;;; ### Requires python, yfinance etc. to be installed on local machine
(ns cim_portfolio.yfinanceclient
  ;; NOTE: do not require libpython-clj2.require here — merely loading it runs a
  ;; bare (py/initialize!) via libpython-clj2.metadata, which would initialize
  ;; python with auto-detected settings BEFORE the configured initialize! below
  ;; (CIM_PORTFOLIO_PYTHON / CIM_PORTFOLIO_LIBPYTHON would be silently ignored)
  (:require [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [clojure.data.json :as json]))

;; CIM_PORTFOLIO_LIBPYTHON should point at the matching libpython .so when the
;; interpreter's shared library is not on the system loader path (e.g. pyenv
;; installs, where an older system libpython would otherwise be loaded)
(let [python-exe (or (System/getenv "CIM_PORTFOLIO_PYTHON")
                     "/home/edward/miniconda3/envs/cim-portfolio/bin/python")]
  (if-let [libpython (System/getenv "CIM_PORTFOLIO_LIBPYTHON")]
    (py/initialize! :python-executable python-exe :library-path libpython)
    (py/initialize! :python-executable python-exe)))

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
        data = yf.download(ticker, start=date, progress=False, auto_adjust=True) # Adjusted for dividends, consistent with get_ticker_price_with_end
        if len(data) > 0:
            break
        if count >= 10:
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
    return data[['Date', 'Open', 'Close']].to_json(orient = 'values')
    
def get_ticker_price_with_end(ticker, start_date, end_date):
    # Note that one day is not added to the start date here 
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=start_date, end=end_date, progress=False, auto_adjust=True) # Note that this is adjusted for dividends!
        if len(data) > 0:
            break
        if count >= 10:
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

(def get-ticker-price-with-end-wrapper (:get_ticker_price_with_end (:globals pythonWrapper)))

(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date)))

(defn get-ticker-price-with-end [ticker start_date end_date]
  (json/read-str (get-ticker-price-with-end-wrapper ticker start_date end_date)))
;; Test if function is working + price is converted to USD

(get-ticker-price-all "0700.HK" "2025-01-25")

(get-ticker-price-with-end "0700.HK" "2025-01-25" (.toString (java.time.LocalDate/now)))

