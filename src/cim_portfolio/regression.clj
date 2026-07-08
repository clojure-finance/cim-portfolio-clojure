(ns cim_portfolio.regression
  (:require
   [cim_portfolio.portfoliofunctions :as portfolio]
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

;; Computes 12-month (252 trading days) rolling alphas and betas.
;; stock-data / market-data are sequences of [date open close] rows,
;; e.g. as returned by cim_portfolio.yfinanceclient/get-ticker-price-all.

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

;; Example usage (fetches live data)
(comment
  (require '[cim_portfolio.yfinanceclient :as client])
  (let [stock-data (client/get-ticker-price-all "NVDA" "2022-09-01")
        market-data (client/get-ticker-price-all "^GSPC" "2022-09-01")]
    (get-alpha-beta stock-data market-data)))
