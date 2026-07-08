(ns cim_portfolio.regression
  (:require [cim_portfolio.plot :as plot]
            [cim_portfolio.portfoliofunctions :as portfolio]
            [cim_portfolio.yfinanceclient :as client]
            [fastmath.ml.regression :as reg]))

(defn calculate-regression [stock-returns market-returns] ;; both returns are 1D sequences
  (reg/lm
   stock-returns
   (map vector market-returns)))

(defn rolling-capm-regression [complete-stock-returns complete-market-returns window-size]
  (let [stock-windows (partition window-size 1 complete-stock-returns)
        market-windows (partition window-size 1 complete-market-returns)
        rolling-regression (map calculate-regression stock-windows market-windows)
        alpha-seq (map :intercept rolling-regression)
        beta-seq (map :beta rolling-regression)]

    {:alpha alpha-seq
     :beta beta-seq}))

;; Fetch sample data (NVDA vs the S&P 500, both already in USD) via the native client

(def stock-data (client/get-ticker-price-with-end "NVDA" "2022-09-01" "2025-09-01"))

(def market-data (client/get-ticker-price-with-end "^GSPC" "2022-09-01" "2025-09-01"))

;; They are the same size 

(count stock-data)

(count market-data)

;; Calculate day-by-day returns

(def stock-dates (map first stock-data)) ;; Dates
(def stock-prices (map #(nth % 2) stock-data)) ;; Closing Prices for NVIDIA stocks
(def market-dates (map first market-data)) ;; Dates
(def market-prices (map #(nth % 2) market-data)) ;; Closing Prices for S&P500 index

(def stock-returns (vals (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date stock-prices stock-dates))))
(def market-returns (vals (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date market-prices market-dates))))

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

;; Creating a function to return alphas and betas

(defn get-alpha-beta [stock-data market-data]
  (let [stock-dates (map first stock-data)
        stock-prices (map #(nth % 2) stock-data)
        market-dates (map first market-data)
        market-prices (map #(nth % 2) market-data)
        stock-returns (vals (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date stock-prices stock-dates)))
        market-returns (vals (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date market-prices market-dates)))
        plotted-dates (subvec (vec stock-dates) 252)
        model (rolling-capm-regression stock-returns market-returns 252)]
    (-> {}
        (assoc :plotted-dates plotted-dates)
        (assoc :plotted-alpha (vec (model :alpha)))
        (assoc :plotted-beta (vec (map first (model :beta)))))))

;; Testing said function

(def regression-dataset (get-alpha-beta stock-data market-data))

;; Alpha

(plot/list-plot (map vector (:plotted-dates regression-dataset) (:plotted-alpha regression-dataset))
                :x-title "Time" :y-title "α (NVDA)")

;; Beta

(plot/list-plot (map vector (:plotted-dates regression-dataset) (:plotted-beta regression-dataset))
                :x-title "Time" :y-title "β (NVDA)")
