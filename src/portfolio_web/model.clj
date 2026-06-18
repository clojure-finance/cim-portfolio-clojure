;;; # CIM Portfolio
;;; ewidjaja@hku.hk Edward Widjaja

(ns portfolio-web.model
  (:require [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
            [clojure.math :as math]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cim_portfolio.regression :as reg]
            [cim_portfolio.portfoliofunctions :as portfolio]
            [clojure.data.json :as json]))

(defn process-trades [raw-data]
  (let [date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
        date-parser (fn [d] (java.time.LocalDate/parse d date-formatter))

        trades (:trades raw-data)
        starting-cash (:starting-cash raw-data)

        [cash portfolio portfolio-composition-by-date portfolio-value current-value cash-invested cash-invested-by-date change-in-cash-by-date complete-stock-prices]
        (portfolio/analyze-portfolio trades)

        unique-tickers (keys portfolio)
        sorted-portfolio-value (map #(vector (first %) (+ starting-cash (second %))) portfolio-value)

        cash-invested-by-dates (into [] cash-invested-by-date)
        current-portfolio-value (+ starting-cash (+ cash current-value))
        annualized-return (portfolio/calculate-annualized-return starting-cash current-portfolio-value
                                                                 (first (first sorted-portfolio-value))
                                                                 (last (last sorted-portfolio-value)))

        portfolio-value-by-day sorted-portfolio-value

        one-year-cumulative-returns-past-five-weeks-without-cash
        (let [today (java.time.LocalDate/now)
              all-trade-execution-dates (keys portfolio-composition-by-date)
              earliest-trade-execution-date (reduce #(if (neg? (.compareTo %1 %2)) %1 %2) (map date-parser all-trade-execution-dates))
              one-week-ago (.minusDays today 7)
              two-weeks-ago (.minusDays today 14)
              three-weeks-ago (.minusDays today 21)
              four-weeks-ago (.minusDays today 28)
              five-weeks-ago (.minusDays today 35)
              one-year-ago (.minusDays today 365)
              one-year-from-one-week-ago (.minusDays one-week-ago 365)
              one-year-from-two-weeks-ago (.minusDays two-weeks-ago 365)
              one-year-from-three-weeks-ago (.minusDays three-weeks-ago 365)
              one-year-from-four-weeks-ago (.minusDays four-weeks-ago 365)
              one-year-from-five-weeks-ago (.minusDays five-weeks-ago 365)

              set-of-portfolio-complete-data (portfolio/set-of-portfolio-log-returns-and-weights-without-cash portfolio-composition-by-date complete-stock-prices)
              portfolio-log-returns (:portfolio-log-returns set-of-portfolio-complete-data)

              get-cumulative-returns
              (fn [log-returns-by-date start-date end-date]
                (let [start-date-enhanced (if (.isBefore (date-parser start-date) earliest-trade-execution-date)
                                            (.toString earliest-trade-execution-date) start-date)
                      log-returns-filtered-by-date
                      (into {} (filter (fn [[d _]]
                                         (and (or (.isAfter (date-parser d) (date-parser start-date-enhanced))
                                                  (.isEqual (date-parser d) (date-parser start-date-enhanced)))
                                              (or (.isBefore (date-parser d) (date-parser end-date))
                                                  (.isEqual (date-parser d) (date-parser end-date)))))
                                       log-returns-by-date))
                      cumulative-return (- (Math/exp (reduce + (vals log-returns-filtered-by-date))) 1)]
                  cumulative-return))

              cumulative-return-today (get-cumulative-returns portfolio-log-returns (.toString one-year-ago) (.toString today))
              cumulative-return-one-week-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-one-week-ago) (.toString one-week-ago))
              cumulative-return-two-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-two-weeks-ago) (.toString two-weeks-ago))
              cumulative-return-three-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-three-weeks-ago) (.toString three-weeks-ago))
              cumulative-return-four-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-four-weeks-ago) (.toString four-weeks-ago))
              cumulative-return-five-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-five-weeks-ago) (.toString five-weeks-ago))

              current-portfolio-weights (last (last (:stock-weights set-of-portfolio-complete-data)))
              current-portfolio-holdings (last (last (:current-stock-holdings set-of-portfolio-complete-data)))]

          {:today (.toString today)
           :one-week-ago (.toString one-week-ago)
           :two-weeks-ago (.toString two-weeks-ago)
           :three-weeks-ago (.toString three-weeks-ago)
           :four-weeks-ago (.toString four-weeks-ago)
           :five-weeks-ago (.toString five-weeks-ago)
           :one-year-ago (.toString one-year-ago)
           :one-year-from-one-week-ago (.toString one-year-from-one-week-ago)
           :one-year-from-two-weeks-ago (.toString one-year-from-two-weeks-ago)
           :one-year-from-three-weeks-ago (.toString one-year-from-three-weeks-ago)
           :one-year-from-four-weeks-ago (.toString one-year-from-four-weeks-ago)
           :one-year-from-five-weeks-ago (.toString one-year-from-five-weeks-ago)
           :one-year-cumulative-return-from-today cumulative-return-today
           :one-year-cumulative-return-from-one-week-ago cumulative-return-one-week-ago
           :one-year-cumulative-return-from-two-weeks-ago cumulative-return-two-weeks-ago
           :one-year-cumulative-return-from-three-weeks-ago cumulative-return-three-weeks-ago
           :one-year-cumulative-return-from-four-weeks-ago cumulative-return-four-weeks-ago
           :one-year-cumulative-return-from-five-weeks-ago cumulative-return-five-weeks-ago
           :current-portfolio-weights current-portfolio-weights
           :current-portfolio-holdings current-portfolio-holdings})

        one-year-cumulative-returns-past-five-weeks-with-cash
        (let [today (java.time.LocalDate/now)
              all-trade-execution-dates (keys portfolio-composition-by-date)
              earliest-trade-execution-date (reduce #(if (neg? (.compareTo %1 %2)) %1 %2) (map date-parser all-trade-execution-dates))
              one-week-ago (.minusDays today 7)
              two-weeks-ago (.minusDays today 14)
              three-weeks-ago (.minusDays today 21)
              four-weeks-ago (.minusDays today 28)
              five-weeks-ago (.minusDays today 35)
              one-year-ago (.minusDays today 365)
              one-year-from-one-week-ago (.minusDays one-week-ago 365)
              one-year-from-two-weeks-ago (.minusDays two-weeks-ago 365)
              one-year-from-three-weeks-ago (.minusDays three-weeks-ago 365)
              one-year-from-four-weeks-ago (.minusDays four-weeks-ago 365)
              one-year-from-five-weeks-ago (.minusDays five-weeks-ago 365)

              set-of-portfolio-complete-data (portfolio/set-of-portfolio-log-returns-and-weights
                                              portfolio-composition-by-date
                                              portfolio-value-by-day
                                              (.toString (.minusDays five-weeks-ago 365))
                                              (.toString today))
              portfolio-log-returns (:portfolio-log-returns set-of-portfolio-complete-data)

              get-cumulative-returns
              (fn [log-returns-by-date start-date end-date]
                (let [start-date-enhanced (if (.isBefore (date-parser start-date) earliest-trade-execution-date)
                                            (.toString earliest-trade-execution-date) start-date)
                      log-returns-filtered-by-date
                      (into {} (filter (fn [[d _]]
                                         (and (or (.isAfter (date-parser d) (date-parser start-date-enhanced))
                                                  (.isEqual (date-parser d) (date-parser start-date-enhanced)))
                                              (or (.isBefore (date-parser d) (date-parser end-date))
                                                  (.isEqual (date-parser d) (date-parser end-date)))))
                                       log-returns-by-date))
                      cumulative-return (- (Math/exp (reduce + (vals log-returns-filtered-by-date))) 1)]
                  cumulative-return))

              cumulative-return-today (get-cumulative-returns portfolio-log-returns (.toString one-year-ago) (.toString today))
              cumulative-return-one-week-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-one-week-ago) (.toString one-week-ago))
              cumulative-return-two-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-two-weeks-ago) (.toString two-weeks-ago))
              cumulative-return-three-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-three-weeks-ago) (.toString three-weeks-ago))
              cumulative-return-four-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-four-weeks-ago) (.toString four-weeks-ago))
              cumulative-return-five-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-five-weeks-ago) (.toString five-weeks-ago))]

          {:today (.toString today)
           :one-week-ago (.toString one-week-ago)
           :two-weeks-ago (.toString two-weeks-ago)
           :three-weeks-ago (.toString three-weeks-ago)
           :four-weeks-ago (.toString four-weeks-ago)
           :five-weeks-ago (.toString five-weeks-ago)
           :one-year-ago (.toString one-year-ago)
           :one-year-from-one-week-ago (.toString one-year-from-one-week-ago)
           :one-year-from-two-weeks-ago (.toString one-year-from-two-weeks-ago)
           :one-year-from-three-weeks-ago (.toString one-year-from-three-weeks-ago)
           :one-year-from-four-weeks-ago (.toString one-year-from-four-weeks-ago)
           :one-year-from-five-weeks-ago (.toString one-year-from-five-weeks-ago)
           :one-year-cumulative-return-from-today cumulative-return-today
           :one-year-cumulative-return-from-one-week-ago cumulative-return-one-week-ago
           :one-year-cumulative-return-from-two-weeks-ago cumulative-return-two-weeks-ago
           :one-year-cumulative-return-from-three-weeks-ago cumulative-return-three-weeks-ago
           :one-year-cumulative-return-from-four-weeks-ago cumulative-return-four-weeks-ago
           :one-year-cumulative-return-from-five-weeks-ago cumulative-return-five-weeks-ago
           :portfolio-log-returns portfolio-log-returns})

        current-stock-holdings-and-weights {:values (:current-portfolio-holdings one-year-cumulative-returns-past-five-weeks-without-cash)
                                            :weights (:current-portfolio-weights one-year-cumulative-returns-past-five-weeks-without-cash)}

        volatility (portfolio/volatility (map second sorted-portfolio-value))
        rolling-annualized-volatility (portfolio/rolling-annualized-volatility (map second portfolio-value-by-day) 21)

        default-rolling-ewma-volatility (portfolio/ewma-rolling-volatility (map second portfolio-value-by-day) 21 0.06)
        alternative-rolling-ewma-volatility (portfolio/ewma-rolling-volatility (map second portfolio-value-by-day) 21 0.03)

        default-rolling-sharpe-ratio (portfolio/rolling-sharpe-ratio (map second portfolio-value-by-day) default-rolling-ewma-volatility 21)
        alternative-rolling-sharpe-ratio (portfolio/rolling-sharpe-ratio (map second portfolio-value-by-day) alternative-rolling-ewma-volatility 21)

        alpha-beta-figs
        (if (raw-data :show-capm-metrics)
          (let [unique-tickers (loop [data (rest trades) complete-tickers #{}]
                                 (if (empty? data) complete-tickers
                                     (recur (rest data)
                                            (let [[date action amount ticker price] (first data)]
                                              (conj complete-tickers ticker)))))
                market-data (client/get-ticker-price-all "^GSPC" "2022-09-01")]
            (loop [tickers unique-tickers plotly-data []]
              (if (empty? tickers) plotly-data
                  (let [ticker (first tickers)
                        stock-data (client/get-ticker-price-all ticker "2022-09-01")
                        model-data (reg/get-alpha-beta stock-data market-data)
                        alpha-data (map vector (:plotted-dates model-data) (:plotted-alpha model-data))
                        beta-data (map vector (:plotted-dates model-data) (:plotted-beta model-data))]
                    (recur (disj tickers ticker)
                           (-> plotly-data
                               (conj {:x (map first alpha-data) :y (map second alpha-data) :type "scatter" :mode "lines" :name (str ticker " α")})
                               (conj {:x (map first beta-data) :y (map second beta-data) :type "scatter" :mode "lines" :name (str ticker " β")})))))))
          "")

        one-dollar-invested-in-portfolio-at-time-zero
        (let [log-dollar-performance
              (util/sort-map-by-date
               (into {} (map (fn [[date value]]
                               (if (neg? value) [date nil]
                                   [date (Math/log (/ value starting-cash))]))
                             portfolio-value-by-day)))]
          {:x (keys log-dollar-performance) :y (vals log-dollar-performance) :type "scatter" :mode "lines" :name "Portfolio Performance"})

        one-dollar-invested-in-stock-at-time-zero
        (if (raw-data :show-stock-performances)
          (let [prices-until-end-date complete-stock-prices
                log-dollar-performance
                (loop [tickers (keys prices-until-end-date) one-dollar-performance {} log-one-dollar-performance {}]
                  (if (empty? tickers) log-one-dollar-performance
                      (recur (rest tickers)
                             (assoc one-dollar-performance (first tickers)
                                    (util/sort-map-by-date
                                     (zipmap (keys (get prices-until-end-date (first tickers)))
                                             (map #(/ % (second (first (vals (get prices-until-end-date (first tickers))))))
                                                  (map second (vals (get prices-until-end-date (first tickers))))))))
                             (assoc log-one-dollar-performance (first tickers)
                                    (util/sort-map-by-date
                                     (zipmap (keys (util/sort-map-by-date (get prices-until-end-date (first tickers))))
                                             (map #(Math/log (/ % (second (first (vals (get prices-until-end-date (first tickers)))))))
                                                  (map second (vals (util/sort-map-by-date (get prices-until-end-date (first tickers))))))))))))
                plotly-data
                (loop [tickers (keys log-dollar-performance) plotly-data {}]
                  (if (empty? tickers) plotly-data
                      (recur (rest tickers)
                             (assoc plotly-data (first tickers)
                                    {:x (map first portfolio-value-by-day)
                                     :y (concat (repeat (count (filter (fn [d] (.isBefore (date-parser d) (date-parser (first (keys (get log-dollar-performance (first tickers)))))))
                                                                       (map first portfolio-value-by-day))) nil)
                                                (vals (get log-dollar-performance (first tickers))))
                                     :type "scatter" :mode "lines" :name (str (first tickers) " Performance")}))))]
            plotly-data)
          "")

        portfolio-value-figs {:x (map first portfolio-value-by-day) :y (map second portfolio-value-by-day) :type "scatter" :mode "lines" :name "Portfolio Value by Day"}
        rolling-annualized-volatility-figs {:x (drop 21 (map first portfolio-value-by-day)) :y rolling-annualized-volatility :type "scatter" :mode "lines" :name "30-Day Rolling Annualized Volatility"}
        default-rolling-ewma-volatility-figs {:x (map first portfolio-value-by-day) :y (concat (repeat 21 nil) default-rolling-ewma-volatility) :type "scatter" :mode "lines" :name "30-Day EWMA Rolling Volatility (λ = 0.94)"}
        alternative-rolling-ewma-volatility-figs {:x (map first portfolio-value-by-day) :y (concat (repeat 21 nil) alternative-rolling-ewma-volatility) :type "scatter" :mode "lines" :name "30-Day EWMA Rolling Volatility (λ = 0.97)"}
        default-rolling-sharpe-ratio-figs {:x (map first portfolio-value-by-day) :y (concat (repeat 21 nil) default-rolling-sharpe-ratio) :type "scatter" :mode "lines" :name "30-Day Annualized Rolling Sharpe Ratio (EWMA λ = 0.94)"}
        alternative-rolling-sharpe-ratio-figs {:x (map first portfolio-value-by-day) :y (concat (repeat 21 nil) alternative-rolling-sharpe-ratio) :type "scatter" :mode "lines" :name "30-Day Annualized Rolling Sharpe Ratio (EWMA λ = 0.97)"}]

    {:current-portfolio-value current-portfolio-value
     :cash (+ starting-cash cash)
     :stocks (- current-portfolio-value (+ starting-cash cash))
     :unique-tickers unique-tickers
     :annualized-portfolio-return (* annualized-return 100)
     :portfolio-volatility volatility
     :annualized-portfolio-volatility (* (math/sqrt 252) volatility)
     :stocks-held-and-shorted portfolio
     :cash-invested cash-invested
     :current-stock-holdings-and-weights current-stock-holdings-and-weights
     :portfolio-value-by-day portfolio-value-by-day
     :past-five-weeks-1y-cumulative-return-excl-cash one-year-cumulative-returns-past-five-weeks-without-cash
     :past-five-weeks-1y-cumulative-return-incl-cash one-year-cumulative-returns-past-five-weeks-with-cash
     :rolling-annualized-volatility rolling-annualized-volatility
     :alpha-beta-figs alpha-beta-figs
     :portfolio-one-dollar-performance-graph one-dollar-invested-in-portfolio-at-time-zero
     :stock-performances-graphs one-dollar-invested-in-stock-at-time-zero
     :portfolio-value-figs portfolio-value-figs
     :rolling-annualized-volatility-figs rolling-annualized-volatility-figs
     :default-rolling-ewma-volatility-figs default-rolling-ewma-volatility-figs
     :alternative-rolling-ewma-volatility-figs alternative-rolling-ewma-volatility-figs
     :default-rolling-sharpe-ratio-figs default-rolling-sharpe-ratio-figs
     :alternative-rolling-sharpe-ratio-figs alternative-rolling-sharpe-ratio-figs
     :test-data (client/convert-currency "6758.T" 3000)}))
