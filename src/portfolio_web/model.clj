;;; # CIM Portfolio
;;; 36920869+W-Edward@users.noreply.github.com Edward Widjaja

;;; This is the backend logic (model) to generate the results on the website

(ns portfolio-web.model
  (:require [cim_portfolio.yfinanceclient :as client]
            [cim_portfolio.corporate-actions :as actions]
            [clojure.math :as math]
            [cim_portfolio.regression :as reg]
            [cim_portfolio.portfoliofunctions :as portfolio])
  (:import (java.time LocalDate)))

;; True when any portfolio value that a cumulative-return window depends on is negative.
;; A return on negative capital is undefined — its log-return is NaN at zero crossings and
;; sign-inverted while the book stays net short — so callers render "n/a" instead of a number.
;; Zero values are fine: an empty portfolio is a normal state and calculate-returns already
;; treats it as a 0% day.
;; values-by-day is a seq of [date value] pairs sorted by date; the last entry before start-date
;; is included in the check because the window's first return is measured from it.
;; (A gross-exposure denominator — PnL over the sum of |holdings| — would make these windows
;; well-defined for net-short books instead of n/a.)
(defn window-contains-negative-value? [values-by-day start-date end-date]
  (let [parse (fn [d] (LocalDate/parse d))
        not-after-end (filter (fn [[d _]] (not (.isAfter (parse d) (parse end-date)))) values-by-day)
        before-start (filter (fn [[d _]] (.isBefore (parse d) (parse start-date))) not-after-end)
        in-window (remove (fn [[d _]] (.isBefore (parse d) (parse start-date))) not-after-end)]
    (boolean (some (fn [[_ v]] (neg? v)) (concat (take-last 1 before-start) in-window)))))

;; The trailing 1-year return is shown as of today and as of 1-5 weeks ago (key, weeks back)
(def ^:private one-year-return-windows
  [[:one-year-cumulative-return-from-today 0]
   [:one-year-cumulative-return-from-one-week-ago 1]
   [:one-year-cumulative-return-from-two-weeks-ago 2]
   [:one-year-cumulative-return-from-three-weeks-ago 3]
   [:one-year-cumulative-return-from-four-weeks-ago 4]
   [:one-year-cumulative-return-from-five-weeks-ago 5]])

(defn trailing-one-year-returns
  "Cumulative return over each 365-day window in one-year-return-windows, from a
   {date log-return} map: exp(sum of the window's log returns) - 1. Windows are clamped
   to start no earlier than `inception` (a LocalDate). A window's value is nil (rendered
   \"n/a\") when values-by-day was negative in it — see window-contains-negative-value?."
  [log-returns-by-date values-by-day inception today]
  (into {}
        (for [[k weeks] one-year-return-windows]
          (let [end (.minusDays today (* 7 weeks))
                start (let [s (.minusDays end 365)] (if (.isBefore s inception) inception s))
                in-window? (fn [d] (let [d (LocalDate/parse d)]
                                     (not (or (.isBefore d start) (.isAfter d end)))))
                log-return (reduce + (keep (fn [[d r]] (when (in-window? d) r)) log-returns-by-date))]
            [k (when-not (window-contains-negative-value? values-by-day (str start) (str end))
                 (- (Math/exp log-return) 1))]))))

;; Effective window of the default rolling Sharpe ratio, in trading days (~ one calendar year).
;; Converted to the EWMA decay via portfolio/ewma-alpha-for-window.
(def sharpe-ratio-window-days 252)

(defn- line-chart
  "Plotly trace for a line chart."
  [name x y]
  {:x x :y y :type "scatter" :mode "lines" :name name})

(defn process-trades [raw-data]
  (let [;; Yahoo's split events, minus spin-offs it booked as splits (those are undone in the prices instead)
        {:keys [splits spin-offs]} (actions/separate-spin-offs (actions/fetch-splits (:trades raw-data)))
        trades (actions/adjust-trades-for-splits (:trades raw-data) splits) ;; Normalize share amounts and user-supplied prices to post-split units, consistent with Yahoo's split-adjusted prices
        starting-cash (:starting-cash raw-data)

        [cash portfolio portfolio-composition-by-date portfolio-value current-value cash-invested _ _ complete-stock-prices]
        (portfolio/analyze-portfolio trades :spin-offs spin-offs)

        ;; Starting cash + cumulative PnL, as [date value] pairs sorted by date. Adding the PnL of
        ;; each trading day to the original starting cash gives the whole portfolio's value (cash included).
        portfolio-value-by-day (map (fn [[d pnl]] [d (+ starting-cash pnl)]) portfolio-value)
        dates (map first portfolio-value-by-day)
        values (map second portfolio-value-by-day)

        current-portfolio-value (+ starting-cash cash current-value)
        portfolio-inception-date (first dates) ;; First trading day on/after the first trade
        annualized-return (portfolio/calculate-annualized-return starting-cash current-portfolio-value
                                                                 portfolio-inception-date (last dates))
        today (LocalDate/now)
        ;; Used by the views to flag 1-year metrics whose window is clamped to a shorter history
        portfolio-younger-than-one-year? (.isAfter (LocalDate/parse portfolio-inception-date)
                                                   (.minusDays today 365))

        ;; Trailing 1-year returns, excluding cash (stock holdings only) and including cash
        first-execution-date (LocalDate/parse (first (keys portfolio-composition-by-date)))
        stocks-only (portfolio/set-of-portfolio-log-returns-and-weights-without-cash portfolio-composition-by-date complete-stock-prices)
        one-year-returns-excl-cash (trailing-one-year-returns (:portfolio-log-returns stocks-only)
                                                              (:portfolio-value stocks-only) ;; net-short stock book -> n/a
                                                              first-execution-date today)
        one-year-returns-incl-cash (trailing-one-year-returns (:log-returns (portfolio/calculate-returns-with-corresponding-date values dates))
                                                              portfolio-value-by-day ;; losses beyond starting cash -> n/a
                                                              first-execution-date today)

        ;; Latest value and weight of each holding: {ticker number}
        current-stock-holdings-and-weights {:values (val (last (:current-stock-holdings stocks-only)))
                                            :weights (val (last (:stock-weights stocks-only)))}

        volatility (when (<= 3 (count values)) ;; Sample std-dev needs ≥2 daily returns, i.e. ≥3 value points — nil renders as "n/a" instead of a bogus 0%
                     (portfolio/volatility values))

        ;; CAPM alpha and beta of the whole portfolio (cash included) against the S&P 500 since inception; nil renders as "n/a"
        portfolio-capm (reg/portfolio-alpha-beta portfolio-value-by-day
                                                 (client/get-ticker-price-all "^GSPC" portfolio-inception-date))

        ;; Held tickers with a one-day drop too large to be anything but an unrecorded corporate
        ;; action (e.g. a spin-off) — rendered as a warning banner explaining how to record the
        ;; received shares, since the drop would otherwise appear as a huge phantom loss
        corporate-action-warnings (portfolio/detect-possible-corporate-actions complete-stock-prices
                                                                               portfolio-composition-by-date
                                                                               (portfolio/zero-cost-trade-dates trades))

        ;; Annualized EWMA volatility (bias-corrected RiskMetrics recursion — one value per daily return, no minimum window)
        default-rolling-ewma-volatility (portfolio/ewma-rolling-volatility values 0.06) ;; Lambda = 1 - alpha = 0.94
        alternative-rolling-ewma-volatility (portfolio/ewma-rolling-volatility values 0.03) ;; Lambda = 1 - alpha = 0.97

        ;; Annualized EWMA Sharpe ratio: EWMA mean return over EWMA volatility, both with the same lambda.
        ;; Default is a ~1-year effective window (sharpe-ratio-window-days): a Sharpe ratio over a shorter window is mostly noise
        default-rolling-sharpe-ratio (portfolio/ewma-sharpe-ratio values (portfolio/ewma-alpha-for-window sharpe-ratio-window-days))
        alternative-rolling-sharpe-ratio (portfolio/ewma-sharpe-ratio values 0.06) ;; Lambda = 1 - alpha = 0.94

        ;; Rolling 1-year alpha and beta of each traded stock vs. the S&P 500, from Sept 2022 on
        ;; (optional, it fetches every ticker's history again): [α trace, β trace] per ticker
        alpha-beta-figs
        (if (:show-capm-metrics raw-data)
          (let [market-data (client/get-ticker-price-all "^GSPC" "2022-09-01")] ;; Switch the market index here
            (vec (mapcat (fn [ticker]
                           (let [{:keys [plotted-dates plotted-alpha plotted-beta]}
                                 (reg/get-alpha-beta (client/get-ticker-price-all ticker "2022-09-01") market-data)]
                             [(line-chart (str ticker " α") plotted-dates plotted-alpha)
                              (line-chart (str ticker " β") plotted-dates plotted-beta)]))
                         (distinct (map #(nth % 3) (rest trades))))))
          "")

        ;; Portfolio performance: value of one dollar invested at inception (starting cash = initial value)
        one-dollar-invested-in-portfolio-at-time-zero
        (line-chart "Portfolio Performance"
                    dates
                    (map #(when (pos? %) (/ % starting-cash)) values)) ;; Non-positive values cannot be drawn on the log-scale axis, so leave a gap

        ;; Each stock's performance: its close divided by its first close, over the portfolio's dates
        ;; (nil before the stock's first quote; on days its exchange was closed, the last close)
        one-dollar-invested-in-stock-at-time-zero
        (if (:show-stock-performances raw-data)
          (into {}
                (map (fn [[ticker prices]] ;; prices: {date [open close]}, sorted by date
                       (let [first-date (key (first prices))
                             first-close (second (val (first prices)))]
                         [ticker
                          (line-chart (str ticker " Performance")
                                      dates
                                      (map (fn [d]
                                             (when (>= (compare d first-date) 0)
                                               (/ (second (val (first (rsubseq prices <= d)))) first-close)))
                                           dates))])))
                complete-stock-prices)
          "")]

    {:current-portfolio-value current-portfolio-value
     :cash (+ starting-cash cash)
     :stocks (- current-portfolio-value (+ starting-cash cash))
     :unique-tickers (keys portfolio)

     :annualized-portfolio-return (when annualized-return (* annualized-return 100))
     :annualized-portfolio-volatility (when volatility (* (math/sqrt 252) volatility))
     :annualized-portfolio-alpha (when portfolio-capm (* 100 (:alpha portfolio-capm)))
     :annualized-sharpe-ratio (last default-rolling-sharpe-ratio) ;; Latest point of the 1-year-window EWMA series, so it matches the chart's last value

     :stocks-held-and-shorted portfolio
     :cash-invested cash-invested
     :current-stock-holdings-and-weights current-stock-holdings-and-weights
     :portfolio-value-by-day portfolio-value-by-day ;; Includes cash

     :past-five-weeks-1y-cumulative-return-excl-cash one-year-returns-excl-cash
     :past-five-weeks-1y-cumulative-return-incl-cash one-year-returns-incl-cash

     :portfolio-inception-date portfolio-inception-date
     :portfolio-younger-than-one-year? portfolio-younger-than-one-year?

     :corporate-action-warnings corporate-action-warnings

     ;; Plotly traces
     :alpha-beta-figs alpha-beta-figs
     :portfolio-one-dollar-performance-graph one-dollar-invested-in-portfolio-at-time-zero
     :stock-performances-graphs one-dollar-invested-in-stock-at-time-zero
     :portfolio-value-figs (line-chart "Portfolio Value by Day" dates values)
     :default-rolling-ewma-volatility-figs (line-chart "Annualized EWMA Volatility of Portfolio (λ = 0.94)"
                                                       dates (cons nil default-rolling-ewma-volatility)) ;; Only the first day has no value — there is no return yet
     :alternative-rolling-ewma-volatility-figs (line-chart "Annualized EWMA Volatility of Portfolio (λ = 0.97)"
                                                           dates (cons nil alternative-rolling-ewma-volatility))
     :default-rolling-sharpe-ratio-figs (line-chart "Annualized EWMA Sharpe Ratio (1-year window)"
                                                    dates (cons nil default-rolling-sharpe-ratio))
     :alternative-rolling-sharpe-ratio-figs (line-chart "Annualized EWMA Sharpe Ratio (λ = 0.94)"
                                                        dates (cons nil alternative-rolling-sharpe-ratio))}))
