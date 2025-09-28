(ns cim_portfolio.regression
    (:require [cim_portfolio.plot :as plot]
              [cim_portfolio.portfolio :as port]
              [fastmath.ml.regression :as reg]
              [libpython-clj2.require :refer [require-python]]
              [libpython-clj2.python :refer [py. py.. py.-] :as py]
              [clojure.data.json :as json]
    ))

(defn calculate-regression [stock-returns market-returns] ;; both returns are 1D sequences
    (reg/lm
        stock-returns
        (map vector market-returns)
    ))

(defn rolling-capm-regression [complete-stock-returns complete-market-returns window-size]
  (let [stock-windows (partition window-size 1 complete-stock-returns)
        market-windows (partition window-size 1 complete-market-returns)
        rolling-regression (map calculate-regression stock-windows market-windows)
        alpha-seq (map :intercept rolling-regression)
        beta-seq (map :beta rolling-regression)]

        {:alpha alpha-seq
         :beta beta-seq}
  ))

;; Define simple python script to get sample data (for now)

(def get-python-data (py/run-simple-string 
"from datetime import datetime, timedelta
import yfinance as yf

# Both are already in USD

nvidia_data = yf.download('NVDA', start='2022-09-01', end='2025-09-01')
snp_data = stock_data = yf.download('^GSPC', start='2022-09-01', end='2025-09-01') 

nvidia_data.reset_index(inplace=True)
snp_data.reset_index(inplace=True)

nvidia_data['Date'] = nvidia_data['Date'].dt.strftime('%Y-%m-%d')
snp_data['Date'] = snp_data['Date'].dt.strftime('%Y-%m-%d')

stock_data = nvidia_data[['Date', 'Open', 'Close']].to_json(orient = 'values')
market_data = snp_data[['Date', 'Open', 'Close']].to_json(orient = 'values')"))

(def stock-data (json/read-str (:stock_data (:globals get-python-data))))

(def market-data (json/read-str (:market_data (:globals get-python-data))))

;; They are the same size 

(count stock-data)

(count market-data)

;; Calculate day-by-day returns

(def stock-dates (map first stock-data)) ;; Dates
(def stock-prices (map #(nth % 2) stock-data)) ;; Closing Prices for NVIDIA stocks
(def market-dates (map first market-data)) ;; Dates
(def market-prices (map #(nth % 2) market-data)) ;; Closing Prices for S&P500 index

(def stock-returns (vals (:arithmetic-returns (port/calculate-returns-with-corresponding-date stock-prices stock-dates))))
(def market-returns (vals (:arithmetic-returns (port/calculate-returns-with-corresponding-date market-prices market-dates))))

;; Regression

;; (We assume that there are 252 trading days in a year, so we can set the window to be 252 data points as we are doing
;; a 12-month rolling window)

(def model (rolling-capm-regression stock-returns market-returns 252))

;; Plotting Alphas

(count (model :alpha))

(count (vec stock-dates))

(def plotted-dates (subvec (vec stock-dates) 252))
(def plotted-alphas (vec (model :alpha)))

(plot/list-plot (map vector plotted-dates plotted-alphas) :x-title "Time" :y-title "α (NVDA)")

;; Plotting Betas

(def plotted-betas (vec (map first (model :beta))))

(plot/list-plot (map vector plotted-dates plotted-betas) :x-title "Time" :y-title "β (NVDA)")






