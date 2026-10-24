(ns cim_portfolio.portfoliofunctions
  (:require [cim_portfolio.corporate-actions :as actions]
            [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
            [clojure.math :as math]
            [clojure.string :as string]))

;;; ### Portfolio Analysis Helper Functions

;; Computes arithmetic, log, and cumulative log return for a given list of prices
(defn calculate-returns [prices]
  (let [price-changes (map #(if (zero? (first %)) ;; Edge case if price is 0 at some point
                              1
                              (double (/ (second %) (first %))))
                           (partition 2 1 prices))
        arithmetic-returns (mapv #(- % 1.0) price-changes)
        log-returns (mapv #(Math/log %) price-changes)
        cumulative-log-return (reduce + log-returns)
        cumulative-arithmetic-return (- (Math/exp cumulative-log-return) 1)]
    {:cumulative-log-return cumulative-log-return
     :cumulative-arithmetic-return cumulative-arithmetic-return
     :arithmetic-returns arithmetic-returns
     :log-returns log-returns}))

;; Variation of the above function - additionally associates the returns with their corresponding date
(defn calculate-returns-with-corresponding-date [prices dates]
  (let [price-changes (map #(if (zero? (first %)) ;; Edge case which can happen when at some point, portfolio is empty
                              1
                              (double (/ (second %) (first %)))) ;; Next Day (or next date in data) price divided by Current Day Price
                           (partition 2 1 prices));; This transforms a 1D array to a 2D array with a sliding window of size=2 and increment=1
        arithmetic-returns (mapv #(- % 1.0) price-changes) ;; Calculate arithmetic returns from day x to day x+1, day x+1 to day x+2, ...
        log-returns (mapv #(Math/log %) price-changes)
        cumulative-log-return (reduce + log-returns)
        cumulative-arithmetic-return (- (Math/exp cumulative-log-return) 1)]
    {:cumulative-log-return cumulative-log-return
     :cumulative-arithmetic-return cumulative-arithmetic-return
     :arithmetic-returns (util/sort-map-by-date (zipmap (rest dates) arithmetic-returns))
     :log-returns (util/sort-map-by-date (zipmap (rest dates) log-returns))}))

;; This function calculates the log returns for a set of portfolios over time (portfolio-composition-by-date), returning information about each portfolio while they existed
;; Multiple variables are returned to prevent the need to fetch to yfinance multiple times, which causes increased computation time and risks rate limiting
;; This function excludes the consideration of CASH
(defn set-of-portfolio-log-returns-and-weights-without-cash [portfolio-composition-by-date complete-stock-prices]
  (let [;; Java Date Parser
        date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
        date-parser (fn [d] (java.time.LocalDate/parse d date-formatter))

        ;; Get all prices from previous fetch in analyze-portfolio function
        sorted-prices-until-end-date-enhanced complete-stock-prices

        ;; All trading dates from the start of the portfolio to today: the union of every ticker's
        ;; dates, so holdings on different exchange calendars all appear (like the cash-inclusive series,
        ;; see sum-pnl-series-with-forward-fill). Returns date strings (i.e. "yyyy-MM-dd"), sorted.
        all-trade-dates (sort util/jt-compare-dates
                              (distinct (mapcat keys (vals sorted-prices-until-end-date-enhanced))))

        ;; [open close] of a ticker on date d; on a day its exchange was closed, the last known prices
        ;; (otherwise the holding would count as worth $0 that day). [0 0] before its first quote.
        prices-on (fn [prices d]
                    (or (get prices d)
                        (some-> (first (rsubseq prices < d)) val)
                        [0 0]))

        ;; Get all portfolio-composition-by-date execution dates (already converted into java.time.LocalDate)
        all-trade-execution-dates (map date-parser (keys portfolio-composition-by-date)) ;; Take All Order Execution Dates

;; Function to get the latest order execution date before an input date (input-date is a String, not java.time.LocalDate)
        ;; If there are no order execution dates earlier than the input date, then return earliest order execution date
        get-nearest-execution-date (fn [input-date]
                                     (.toString ;; Result will be a java.time.LocalDate, so convert to String
                                      (if
                                       (nil? (reduce (fn [best d] ;; Get the latest order execution date relative to input-date
                                                       (if (and (or (.isBefore d (date-parser input-date)) (.isEqual d (date-parser input-date)))
                                                                (or (nil? best) (.isAfter d best)))
                                                         d
                                                         best))
                                                     nil all-trade-execution-dates)) ;; if reduce returns "nil", then no order exists before each start date 
                                        ;; If there are no execution dates earlier than input-date, then return earliest execution date
                                        (reduce #(if (neg? (.compareTo %1 %2)) %1 %2) ;; Get minimum date in portfolio-composition-by-date
                                                (map date-parser (keys portfolio-composition-by-date)))
                                        ;; Otherwise return the latest order execution date relative to input-date (same expression as condition)
                                        (reduce (fn [best d]
                                                  (if (and (or (.isBefore d (date-parser input-date)) (.isEqual d (date-parser input-date)))
                                                           (or (nil? best) (.isAfter d best)))
                                                    d
                                                    best))
                                                nil all-trade-execution-dates))))

        ;; Get the values of each holding on each trade date based on the existing portfolio on that date
        ;; Returns the following format: {"2025-01-10" {"NVDA" ($100 * 25 units) "MSFT" ($100 * 25 units) ...}, 
        ;;                                "2025-01-13" {"NVDA" ($101 * 25 units) "MSFT" ($99 * 25 units) ...}, ...}
        holding-values-by-date (util/sort-map-by-date
                                (into {}
                                      (map (fn [d]
                                             [d (into {} ;; Date is set as key
                                                      (map (fn [[ticker prices]]
                                                             [ticker ;; Ticker is inner map's key
                                                              (if (= d (first (keys (get sorted-prices-until-end-date-enhanced ticker))))
                                                                ;; Multiply price of stock at trade date by the amount in existing portfolio to get holding value
                                                                ;; Use opening price only if trade date is the first execution date of that stock, otherwise always use closing price (This is correct even if the same stock is bought/sold in a future date, because we only care about the stock holdings at the end of day, this is not a measure of PnL (but should it be? Seems too complicated tho...)!!!)
                                                                (* (first (prices-on prices d)) (get (get portfolio-composition-by-date (get-nearest-execution-date d)) ticker 0)) ;; Notice that if ticker is not in existing portfolio, amount is 0
                                                                (* (last (prices-on prices d)) (get (get portfolio-composition-by-date (get-nearest-execution-date d)) ticker 0))) ;; Notice that if ticker is not in existing portfolio, amount is 0
                                                              ])
                                                           sorted-prices-until-end-date-enhanced) ;; For each ticker, get date-prices key-value pair
                                                      )])
                                           all-trade-dates)) ; For each trade date 
                                )
        ;; Get total portfolio value by date (using the existing portfolio on that date), sorted by date
        portfolio-value-by-date (util/sort-map-by-date
                                 (into {}
                                       (map (fn [[d holdings]]
                                              [d (reduce + (vals holdings))])
                                            holding-values-by-date)))

        ;; Get portfolio weights by date
        ;; Returns the following format: {"2025-01-10" {"NVDA" 20% "MSFT" 100% "TSLA" -20% ...}, 
        ;;                                "2025-01-13" {"NVDA" 10% "MSFT" 100% "TSLA" -10% ...}, ...}
        portfolio-weights-by-date (util/sort-map-by-date
                                   (into {} ;; This is unsorted, becareful 
                                         (map (fn [[d portfolio-value]]
                                                [d (if (not (pos? portfolio-value)) ;; Edge case when portfolio value is 0 (no stocks held) or negative (net-short book, where value-based weights would have inverted signs)
                                                     (into {}
                                                           (map (fn [ticker]
                                                                  [ticker (double 0)])
                                                                (keys sorted-prices-until-end-date-enhanced)))
                                                     (into {}
                                                           (map (fn [ticker]
                                                                  [ticker (/ (get (get holding-values-by-date d) ticker) portfolio-value)]) ;; Divides the stock holdings on date "d", by the total portfolio value on date "d"
                                                                (keys sorted-prices-until-end-date-enhanced))))])
                                              portfolio-value-by-date)))

;; This variable holds all of the time-series log returns of the portfolio
        ;; On the trade date when a new order execution happens, we need to replace return with 0 because the portfolio changes. There is no return on these dates since there is a change in the portfolio composition.
        portfolio-log-returns-by-date (reduce
                                       (fn [m d]
                                         (assoc m d 0)) ;; This essentially makes it so that execution dates are given 0 log return.
                                       (:log-returns (calculate-returns-with-corresponding-date (vals portfolio-value-by-date) (keys portfolio-value-by-date))) ;; When using this function, arguments have to be ordered by time
                                       (keys portfolio-composition-by-date))]

    {:stock-weights portfolio-weights-by-date
     :current-stock-holdings holding-values-by-date
     :portfolio-log-returns portfolio-log-returns-by-date
     :portfolio-value portfolio-value-by-date}))

; Calculates the annualized return of the portfolio (accepts the starting value, ending value of portfolio, the start and end date)
; Returns nil when the period is shorter than one full day — annualizing a same-day value is undefined (and used to divide by zero)
(defn calculate-annualized-return [starting-value ending-value start-date end-date]
  (let [return (/ (- ending-value starting-value) starting-value)
        number-of-days (util/number-of-days-between start-date end-date)]
    (when (pos? number-of-days)
      (- (math/pow (+ 1 return) (/ 365 number-of-days)) 1))))

; Calculates the volatility of a given list of prices (expected input: portfolio value list)
(defn volatility [prices]
  (let [daily-returns (:arithmetic-returns (calculate-returns prices))
        volatility (* 100 (util/std-dev daily-returns))]
    volatility))

;; Bias-corrected EWMA of a series (pandas ewm adjust=true): the zero-seeded recursion
;; s_t = (1 - alpha) * s_(t-1) + alpha * x_t divided by the accumulated weight 1 - (1 - alpha)^t,
;; making each value an exact weighted mean of the observations so far. Without the correction the
;; recursion has to be seeded with the first observation, and with a long memory (small alpha) that
;; seed dominates the first months of the series.

(defn bias-corrected-ewma [alpha xs]
  (let [lambda (- 1.0 alpha)]
    (->> (reductions (fn [s x] (+ (* lambda s) (* alpha x))) 0.0 xs)
         (rest)
         (map-indexed (fn [i s] (/ s (- 1.0 (Math/pow lambda (inc i)))))))))

;; Calculates the Annualized EWMA volatility (in %) as the
;; bias-corrected EWMA of squared returns: the RiskMetrics decay, but weight-normalized (see
;; bias-corrected-ewma) so early values don't lean on the first squared return as a seed.
;; No window is needed, so there is one value per daily return — from the second price point onward, however short the history.
;; Prices here are the Portfolio values by date (I assume is already sorted), and follow the following structure:
;; (10000, 10001.21, 10011.8, ...)
;; In the future, user may input the alpha parameter in the dashboard. Might also have to make sure that when portfolio is changed, the return is 0.

(defn ewma-rolling-volatility [prices alpha]
  (let [returns (:arithmetic-returns (calculate-returns prices))
        returns-squared (map #(* % %) returns)]
    (when (seq returns-squared)
      (mapv #(* 100 (Math/sqrt 252) (Math/sqrt %))
            (bias-corrected-ewma alpha returns-squared)))))

;; Converts an effective window length in trading days into the EWMA alpha whose weighted mean age
;; matches a simple moving average of that length: alpha = 2 / (window + 1), the "span" convention
;; (as in pandas ewm(span=...)). E.g. a one-year window of 252 trading days gives alpha ~0.0079,
;; i.e. lambda = 1 - alpha ~0.992.

(defn ewma-alpha-for-window [window-days]
  (/ 2.0 (inc window-days)))

;; A Sharpe ratio estimated from only a handful of returns is degenerate — at the very first return
;; the bias-corrected mean is r_1 and the vol is sqrt(252)*|r_1|, so the ratio is exactly
;; +/- sqrt(252) (~15.9) by construction, dwarfing the rest of the chart. Mask the series until it
;; is based on at least this many returns (one trading month).
(def sharpe-min-periods 21)

;; Calculates the annualized EWMA Sharpe ratio: a bias-corrected EWMA mean daily return (annualized)
;; divided by the annualized EWMA volatility in % from ewma-rolling-volatility, both with the same
;; alpha and the same bias correction (see bias-corrected-ewma).
;; Sharing the decay keeps numerator and denominator on the same effective window: a return fades out
;; of both at the same rate, instead of dropping out of a fixed-length mean after 21 days while still
;; lifting the volatility (which made the ratio jump on days when nothing happened).
;; One value per daily return, aligned with ewma-rolling-volatility — nil until min-periods returns
;; exist (default sharpe-min-periods), then a value from there onward.

(defn ewma-sharpe-ratio
  ([prices alpha] (ewma-sharpe-ratio prices alpha sharpe-min-periods))
  ([prices alpha min-periods]
   (let [returns (:arithmetic-returns (calculate-returns prices))
         volatility (ewma-rolling-volatility prices alpha)]
     (when (seq returns)
       (map (fn [n mean-return vol]
              (when (and (>= n min-periods)
                         (pos? vol)) ;; nil instead of dividing by zero when the portfolio value never moved
                (/ (* 252 mean-return) (/ vol 100))))
            (iterate inc 1) ;; n = number of returns the estimate is based on
            (bias-corrected-ewma alpha returns)
            volatility)))))

;;; ### Portfolio Processing Section

;; Each trade contributes a PnL series keyed by ITS ticker's trading dates. Tickers on different
;; exchange calendars miss each other's dates, so summing the series directly makes a holding's
;; entire PnL vanish on days its market is closed (phantom drops in portfolio value that
;; contaminate every downstream statistic). Carry each series' last known PnL forward over the
;; union of all dates instead, from the series' first date onward.
(defn sum-pnl-series-with-forward-fill [pnl-series]
  (let [sorted-series (map util/sort-map-by-date pnl-series)
        all-dates (sort util/jt-compare-dates (distinct (mapcat keys sorted-series)))
        fill (fn [pnl-by-date]
               (first
                (reduce (fn [[filled last-pnl] d]
                          (cond
                            (contains? pnl-by-date d) [(assoc filled d (get pnl-by-date d)) (get pnl-by-date d)]
                            (some? last-pnl) [(assoc filled d last-pnl) last-pnl]
                            :else [filled nil])) ;; Date is before this trade existed — contributes nothing
                        [{} nil]
                        all-dates)))]
    (util/sort-map-by-date (apply merge-with + {} (map fill sorted-series)))))

;; A one-day close-to-close drop at least this large in a held stock is flagged as a likely
;; unrecorded corporate action. Splits can't trip it — the price series is already
;; split-adjusted — and ordinary trading essentially never moves a stock this far in a day.
(def corporate-action-drop-threshold 0.4)

(defn zero-cost-trade-dates
  "Dates (normalized to yyyy-MM-dd) of trades with an explicit price of 0 — the convention
   for recording shares received in a corporate action (see README). Once such a trade
   exists on a date, the corporate-action warning for that date is silenced: the user has
   already recorded the receipt. `trades` is the parsed table (header row + data rows)."
  [trades]
  (set (keep (fn [[date _action _amount _ticker price :as row]]
               (when (and (<= 5 (count row))
                          (try (zero? (Double/parseDouble price)) ;; Tolerate malformed rows, like adjust-trade does
                               (catch Exception _ false)))
                 (util/parse-date date)))
             (rest trades))))

(defn detect-possible-corporate-actions
  "Scans every ticker's (split-adjusted) daily close-to-close returns for one-day drops of
   at least corporate-action-drop-threshold on days the ticker was held going into the drop.
   Yahoo reports no spin-off events, so such a price break usually means shares of a
   spun-off company were received but never recorded — the views show a warning telling the
   user to record them as a buy at price 0 (zero cash, offsets the parent's drop).

   complete-stock-prices is {ticker {date [open close]}} with dates ascending (the enhanced
   form returned by analyze-portfolio); portfolio-composition-by-date is the date-sorted map
   of post-trade portfolio snapshots; recorded-receipt-dates (see zero-cost-trade-dates)
   are drop dates to stay silent about because a share receipt is already recorded there.
   Returns [{:ticker _ :date _ :drop-pct _} ...] sorted by date then ticker; :date is the
   day the price broke."
  ([complete-stock-prices portfolio-composition-by-date]
   (detect-possible-corporate-actions complete-stock-prices portfolio-composition-by-date #{}))
  ([complete-stock-prices portfolio-composition-by-date recorded-receipt-dates]
   (let [snapshots (vec portfolio-composition-by-date)
         ;; Position after the last trade on/before `date`. Snapshots only ever gain tickers,
         ;; so when the latest one lacks the ticker, every earlier one does too and the
         ;; backwards scan correctly falls through to 0.
         held-on? (fn [ticker date]
                    (not (zero? (or (some (fn [[d pf]]
                                            (when (<= (compare d date) 0) (get pf ticker)))
                                          (rseq snapshots))
                                    0))))]
     (->> (for [[ticker prices] complete-stock-prices
                [[d1 [_ close1]] [d2 [_ close2]]] (partition 2 1 prices)
                :when (and (pos? close1)
                           (<= (/ close2 close1) (- 1 corporate-action-drop-threshold))
                           (not (contains? recorded-receipt-dates d2))
                           (held-on? ticker d1))]
            {:ticker ticker :date d2 :drop-pct (* 100.0 (- 1 (/ close2 close1)))})
          (sort-by (juxt :date :ticker))
          vec))))

;; spin-offs: {ticker [{:date :ratio}]} from corporate-actions/separate-spin-offs — spin-offs Yahoo
;; booked as splits, whose pre-spin prices are restored so price-0 receipt rows don't double-count
(defn analyze-portfolio [data & {:keys [spin-offs]}]
  (loop [cash 0.0 ;; This is the cash spent by buying or obtained by selling so far
         portfolio {}
         portfolio-composition-by-date (array-map) ;; Even if array-maps sort the data by key, they have a limit of sorting up to 8 items, so please use util/sort-map-by-date
         per-trade-pnl-series [] ;; One {date PnL} map per trade, summed with forward-fill at the end so mixed exchange calendars don't drop a holding's PnL on its market's closed days
         current-value 0.0
         cash-invested {}
         cash-invested-by-date (array-map)
         change-in-cash-by-date (array-map) ;; This gets the change in the starting cash by date
         ;; complete-stock-prices looks like the following: {"NVDA" [["2025-01-31" $250 $251], ["2025-02-01" $251.25 $249.27], ...], 
         ;;                                                  "MSFT" [["2025-01-31" $172 $180], ["2025-02-01" $177 $175], ...], 
         ;;                                                  ...}
         ;; and it should have the complete stock prices from the first date the stock appears in the portfolio, 
         ;; and will have prices up till today even if it is not in the portfolio anymore
         complete-stock-prices {}
         data (rest data)]
    (if (empty? data)
      (let [;; Turns price data into cleaner form, where keys are the stock tickers, and values are maps where keys are dates and vals are open and close prices, e.g.
            ;;  {"NVDA" {"2025-01-31" [$250 $251], "2025-02-01" [$251.25 $249.27], ...}, 
            ;;   "MSFT" {"2025-01-31" [$172 $180], "2025-02-01" [$177 $175], ...}, ...}
            complete-stock-prices-enhanced
            (into {}
                  (map (fn [[ticker prices]]
                         [ticker (util/sort-map-by-date ;; Sort by date
                                  (into {} ;; this makes everything (the dates) unsorted 
                                        (map (fn [[date opening-price closing-price]]
                                               [date [opening-price closing-price]])
                                             prices)))])
                       complete-stock-prices))]
        [cash portfolio (util/sort-map-by-date portfolio-composition-by-date) (sum-pnl-series-with-forward-fill per-trade-pnl-series) current-value cash-invested (util/sort-map-by-date cash-invested-by-date) (util/sort-map-by-date change-in-cash-by-date) complete-stock-prices-enhanced] ;; When no more rows, return final values
        )
      (let [[date action amount ticker set-price] (first data)

            ;; Java Datetime related functions
            date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
            date-parser (fn [d] (java.time.LocalDate/parse d date-formatter))

            ticker-prices (filter ;; Filter ticker prices such that only data on the trade date and after is taken  
                           (fn [[d open-price close-price]]
                             (or (.isEqual (date-parser d) (date-parser date))
                                 (.isAfter (date-parser d) (date-parser date))))

                           ;; Obtain stock price data (Please check if vector is ordered by date) 
                           (if (= (get complete-stock-prices ticker "") "") ;; Check if we have fetched this ticker previously 
                             (actions/undo-spin-off-price-adjustment ;; Real pre-spin prices for spin-offs Yahoo booked as splits
                              (client/get-ticker-price-all ticker (util/parse-date date)) ;; Get prices for only one of the tickers from the trade date until today
                              (get spin-offs ticker))
                             (get complete-stock-prices ticker 0)) ;; If already previously fetched, then no need to re-fetch
                           )
            _ (when (empty? ticker-prices) ;; Unknown/delisted ticker, failed fetch, or trade date after the last available quote — without this, nil prices NPE further down
                (throw (ex-info (str "No price data found for ticker \"" ticker
                                     "\" on or after " date
                                     ". Check that the ticker symbol is correct and the trade date is not in the future.")
                                {:ticker ticker :date date})))
            executed-date (first (first ticker-prices)) ; gets the date the buy/sell order is executed
            set-price (if (nil? set-price) set-price (str (client/convert-currency ticker set-price))) ;; Converts the inputted price into USD (by default) if not in USD, this returns a string for consistency
            ]
        (cond
          (= (string/lower-case action) "buy")
          (if (pos? (Double. amount))
            (let [price (if (nil? set-price) (second (first ticker-prices)) (Double. set-price)) ; Gets open price of trading day OR the set-price if available
                  currPrice (nth (last ticker-prices) 2) ; Gets adj close price of latest day
                  prices (mapv #(nth % 2) ticker-prices) ; Extracts the closing prices from ticker-prices (trade date to today)
                  amounts (repeatedly (count prices) #(Double. amount))] ; Repeats amount for num of trading days (trade date to today)
              (recur (- cash (* (Double. amount) price)) ;; Cash spent to buy stocks = - (Amount of stocks * Market price of stock when traded)
                     (assoc portfolio ticker (+ (get portfolio ticker 0) (Double. amount))) ;; Updates the current quantity of the stock in the portfolio
                     (assoc portfolio-composition-by-date executed-date (assoc portfolio ticker (+ (get portfolio ticker 0) (Double. amount)))) ;; Save a snapshot of the portfolio composition every order
                     (conj per-trade-pnl-series ;; Collect this trade's PnL series; the series are summed with forward-fill once all trades are processed
                           (zipmap ;; {T PnL, T+1 PnL, ...}
                            (map first ticker-prices) ;; Dates for the PnL UNTIL TODAY
                            (map #(- % (* (Double. amount) price)) (map * prices amounts)))) ;; PnL for each trading day relative to trade date = Market value of each holding for trading days after the trade date - Market value of the holdings on the trade date
                     (+ current-value (* (Double. amount) currPrice)) ;; Current (Latest) market value of stocks
                     (assoc cash-invested ticker (+ (get cash-invested ticker 0) (* (Double. amount) price))) ;; Updates the amount of cash spent on the trade date so far for each ticker
                     (assoc cash-invested-by-date executed-date (assoc cash-invested ticker (+ (get cash-invested ticker 0) (* (Double. amount) price)))) ;; Same like above but with the trade date
                     ;; Be careful of signage, buy orders should "deplete" cash
                     (assoc change-in-cash-by-date executed-date
                            (if (empty? change-in-cash-by-date)
                              ;; If this is the first trade, just put in the initial investment
                              (- 0 (* (Double. amount) price))

                              ;; Add new trade notional to most recent cash change
                              (- (get change-in-cash-by-date
                                      ;; Get most recent trade date
                                      (.toString
                                       (reduce #(if (> (.compareTo %1 %2) 0) %1 %2) ;; .compareTo returns a positive integer if %1 is greater than %0, zero if they are equal, otherwise negative integer 
                                               (map date-parser
                                                    (keys change-in-cash-by-date)))) 0) (* (Double. amount) price))))
                     (if (= (get complete-stock-prices ticker "") "") ;; Check if we have to store new stock prices into map
                       (assoc complete-stock-prices ticker ticker-prices)
                       complete-stock-prices) ;; Keep previously fetched stock price data
                     (rest data)))
            (recur cash portfolio portfolio-composition-by-date per-trade-pnl-series current-value cash-invested cash-invested-by-date change-in-cash-by-date complete-stock-prices (rest data))) ;; If negative amount, ignore

          (= (string/lower-case action) "sell")
          (let [price (if (nil? set-price) (second (first ticker-prices)) (Double. set-price))
                currPrice (nth (last ticker-prices) 2)
                prices (mapv #(nth % 2) ticker-prices) ; Extracts the closing prices from ticker-prices (trade date to today), same as the buy branch — only the trade itself happens at the open
                amounts (repeatedly (count prices) #(Double. amount))]
            (recur (+ cash (* (Double. amount) price))
                   (assoc portfolio ticker (- (get portfolio ticker 0) (Double. amount)))
                   (assoc portfolio-composition-by-date executed-date (assoc portfolio ticker (- (get portfolio ticker 0) (Double. amount))))
                   (conj per-trade-pnl-series (zipmap (map first ticker-prices)
                                                      (map #(- (* (Double. amount) price) %) (map * prices amounts)))) ;; PnL for each trading day relative to trade date (short) = Market value of the holdings on the trade date - Market value of each holding for trading days after the trade date
                   (- current-value (* (Double. amount) currPrice))
                   (assoc cash-invested ticker (- (get cash-invested ticker 0) (* (Double. amount) price)))
                   (assoc cash-invested-by-date executed-date (assoc cash-invested ticker (- (get cash-invested ticker 0) (* (Double. amount) price))))
                   ;; Be careful of signage, sell orders should "increase" cash 
                   (assoc change-in-cash-by-date executed-date
                          (if (empty? change-in-cash-by-date)

                            ;; If this is the first trade, just put in the initial investment
                            (+ 0 (* (Double. amount) price))

                            ;; Add new trade notional to most recent cash change
                            (+ (get change-in-cash-by-date
                            ;; Get most recent trade date 
                                    (.toString
                                     (reduce #(if (> (.compareTo %1 %2) 0) %1 %2) ;; .compareTo returns a positive integer if %1 is greater than %0, zero if they are equal, otherwise negative integer  
                                             (map date-parser
                                                  (keys change-in-cash-by-date)))) 0) (* (Double. amount) price))))

                   (if (= (get complete-stock-prices ticker "") "") ;; Check if we have to store new stock prices into map 
                     (assoc complete-stock-prices ticker ticker-prices)
                     complete-stock-prices)
                   (rest data))) ;; Remove the first row
          )))))