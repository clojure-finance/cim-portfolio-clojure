;;; # Clojure Wrapper over Python's yfinance API 
;;; ### Requires python, yfinance etc. to be installed on local machine
(ns cim_portfolio.yfinanceclient
  ;; NOTE: do not require libpython-clj2.require here — merely loading it runs a
  ;; bare (py/initialize!) via libpython-clj2.metadata, which would initialize
  ;; python with auto-detected settings BEFORE the configured initialize! below
  ;; (CIM_PORTFOLIO_PYTHON / CIM_PORTFOLIO_LIBPYTHON would be silently ignored)
  (:require [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [clojure.data.json :as json]
            [clj-yfinance.core :as yf]
            [clojure-finance.ecbjure.fx :as fx])
  (:import  (java.time Instant LocalDate ZoneOffset)))

;; CIM_PORTFOLIO_LIBPYTHON should point at the matching libpython .so when the
;; interpreter's shared library is not on the system loader path (e.g. pyenv
;; installs, where an older system libpython would otherwise be loaded)
(let [python-exe (or (System/getenv "CIM_PORTFOLIO_PYTHON")
                     "/home/edward/miniconda3/envs/cim-portfolio/bin/python")]
  (if-let [libpython (System/getenv "CIM_PORTFOLIO_LIBPYTHON")]
    (py/initialize! :python-executable python-exe :library-path libpython)
    (py/initialize! :python-executable python-exe)))

;; Fetch latest rates from ECB
(def c (fx/make-converter))

;; (require-python '[yfinance :as yf]
;;                 '[datetime :as dt])

;; Test if yfinance working through clojure-python wrapper
;; (yf/download "AAPL" "2025-01-15" :progress false :auto_adjust false)

(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
from currency_converter import CurrencyConverter

def get_ticker_price_all_deprecated(ticker, date): # This function should fetch all prices in trading dates from the trade date to T+30 (30 days after the trade date) only (function is deprecated)
    # DEPRECATED FUNCTION
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date, progress=False, auto_adjust=False) # Prices are not adjusted for dividends.
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

def get_ticker_price_all(ticker, date): # This function should fetch all prices in all trading dates from the trade date to today.
    # date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d') # Trade date is at least one day after the order date (Comment for new changes to order format)
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date, end=datetime.today().date(), progress=False, auto_adjust=True) # Prices are adjusted for dividends.
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
    return data[['Date', 'Open', 'Close']].to_json(orient = 'values')
    
def convert_currency(ticker, ticker_price, target_currency='USD'):
    # This function is used to convert a price in a foreign currency (e.g. for a non USD-denominated asset) to a target currency
    stock = yf.Ticker(ticker)
    ticker_price = float(ticker_price)
            
    if 'currency' in stock.info and stock.info['currency'] != target_currency:
        c = CurrencyConverter()
        fx_to_target = c.convert(1, stock.info['currency'], target_currency)
    else:
        fx_to_target = 1
    
    converted_price = ticker_price * fx_to_target
    return converted_price
    "))

(def get-ticker-price-all-wrapper (:get_ticker_price_all (:globals pythonWrapper)))

(def get-ticker-price-with-end-wrapper (:get_ticker_price_with_end (:globals pythonWrapper)))

(def convert-currency-wrapper (:convert_currency (:globals pythonWrapper)))

(defn python-get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date)))

(defn python-get-ticker-price-with-end [ticker start_date end_date]
  (json/read-str (get-ticker-price-with-end-wrapper ticker start_date end_date)))

(defn python-convert-currency
  ([ticker ticker_price target_currency] (convert-currency-wrapper ticker ticker_price target_currency))
  ([ticker ticker_price] (convert-currency-wrapper ticker ticker_price) ;; No target currency defaults to USD
                         ))

(defn convert-currency
  ;; Accepts the price as a number or a string (trade files carry strings), like the Python wrapper's float() coercion did
  ([ticker ticker-price target-currency] (let [stock-currency (:currency (yf/fetch-info ticker))] (fx/convert c (Double/parseDouble (str ticker-price)) stock-currency target-currency)))
  ([ticker ticker-price] (let [stock-currency (:currency (yf/fetch-info ticker))] (fx/convert c (Double/parseDouble (str ticker-price)) stock-currency "USD"))) ;; No target currency defaults to USD
  )

(defn get-ticker-price-all [ticker date]
  (let [yf-response (yf/fetch-historical ticker :start (.toEpochSecond (.atStartOfDay (LocalDate/parse date) ZoneOffset/UTC)) ;; Convert date string, e.g. 2026-01-31, to Epoch Seconds
                                         :interval "1d"
                                         :auto-adjust true) ;; Prices are adjusted for dividends (and splits), like the Python wrapper's auto_adjust=True
        stock-currency (:currency (yf/fetch-info ticker))
        ticker-prices (pop ;; For some reason, the last datapoint is duplicated 
                       (vec
                        (map #(vector (str (.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp %)) ZoneOffset/UTC))) ;; Convert Seconds since Epoch to a Date String, e.g. 2026-01-31 
                                      (fx/convert c (:open %) stock-currency "USD")
                                      (fx/convert c (:close %) stock-currency "USD"))
                             yf-response)))]
    ticker-prices))

(defn get-ticker-price-with-end [ticker start-date end-date]
  (let [yf-response (yf/fetch-historical ticker :start (.toEpochSecond (.atStartOfDay (LocalDate/parse start-date) ZoneOffset/UTC)) ;; Convert date string, e.g. 2026-01-31, to Epoch Seconds
                                         :end (.toEpochSecond (.atStartOfDay (LocalDate/parse end-date) ZoneOffset/UTC))
                                         :interval "1d"
                                         :auto-adjust true) ;; Prices are adjusted for dividends (and splits), like the Python wrapper's auto_adjust=True
        stock-currency (:currency (yf/fetch-info ticker))
        ticker-prices (pop ;; For some reason, the last datapoint is duplicated 
                       (vec
                        (map #(vector (str (.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp %)) ZoneOffset/UTC))) ;; Convert Seconds since Epoch to a Date String, e.g. 2026-01-31 
                                      (fx/convert c (:open %) stock-currency "USD")
                                      (fx/convert c (:close %) stock-currency "USD"))
                             yf-response)))]
    ticker-prices))

;; Test if function is working + price is converted to USD
(comment
  (get-ticker-price-all "0700.HK" "2025-01-25")

  (get-ticker-price-with-end "0700.HK" "2025-01-25" (.toString (java.time.LocalDate/now))))

