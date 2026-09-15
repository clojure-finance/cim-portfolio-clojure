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
        stock-returns-by-date (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date stock-prices stock-dates))
        market-returns-by-date (:arithmetic-returns (portfolio/calculate-returns-with-corresponding-date market-prices market-dates))
        ;; Regress only on dates where BOTH the stock and the market traded. Pairing the two
        ;; return series by position instead would shift them against each other whenever the
        ;; calendars differ (foreign listings, halts, or a stock younger than the market range),
        ;; regressing each stock return on the wrong market day.
        common-dates (filter #(contains? market-returns-by-date %) (keys stock-returns-by-date))
        stock-returns (map stock-returns-by-date common-dates)
        market-returns (map market-returns-by-date common-dates)
        ;; The first full window covers returns 1..252, so its regression is plotted on the
        ;; 252nd common date. drop (unlike the previous subvec) yields an empty series instead
        ;; of throwing when the stock is younger than the window.
        plotted-dates (vec (drop 251 common-dates))
        model (rolling-capm-regression stock-returns market-returns 252)]
    (-> {}
        (assoc :plotted-dates plotted-dates)
        (assoc :plotted-alpha (vec (model :alpha)))
        (assoc :plotted-beta (vec (map first (model :beta)))))))

;; Annualized CAPM alpha (and beta) of the whole portfolio against the market over its full history.
;; portfolio-value-by-day is a seq of [date value] pairs sorted by date; market-data is a seq of
;; [date open close] rows, e.g. as returned by cim_portfolio.yfinanceclient/get-ticker-price-all.
;; Both value series are restricted to the dates they share BEFORE returns are taken, so each
;; portfolio return covers the same interval as its market return (the portfolio series spans the
;; union of every holding's exchange calendar). Like the per-stock rolling alpha, returns are raw
;; (no risk-free rate); the daily intercept is annualized by 252 trading days.
;; Returns nil when fewer than 21 common daily returns exist (a month of data — shorter regressions
;; are noise), or when the portfolio value went negative (a return on negative capital is undefined).
(defn portfolio-alpha-beta [portfolio-value-by-day market-data]
  (let [market-close-by-date (into {} (map (fn [[d _ close]] [d close]) market-data))
        common (filter (fn [[d _]] (contains? market-close-by-date d)) portfolio-value-by-day)
        portfolio-returns (:arithmetic-returns (portfolio/calculate-returns (map second common)))
        market-returns (:arithmetic-returns (portfolio/calculate-returns (map (comp market-close-by-date first) common)))]
    (when (and (<= 21 (count portfolio-returns))
               (not-any? (fn [[_ v]] (neg? v)) common))
      (let [model (calculate-regression portfolio-returns market-returns)]
        {:alpha (* 252 (:intercept model))
         :beta (first (:beta model))}))))

;; Example usage (fetches live data)
(comment
  (require '[cim_portfolio.yfinanceclient :as client])
  (let [stock-data (client/get-ticker-price-all "NVDA" "2022-09-01")
        market-data (client/get-ticker-price-all "^GSPC" "2022-09-01")]
    (get-alpha-beta stock-data market-data)))
