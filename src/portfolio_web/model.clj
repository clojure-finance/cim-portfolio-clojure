;;; # CIM Portfolio 
;;; ewidjaja@hku.hk Edward Widjaja

;;; This is the backend logic (model) to generate the results on the website

(ns portfolio-web.model 
  (:require [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
            [clojure.math :as math]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cim_portfolio.regression :as reg]
            [cim_portfolio.portfoliofunctions :as portfolio]
            [clojure.data.json :as json]))

;; (def portfolio-options {:starting-cash 200000}) ;; Make this dynamic later on 

(defn process-trades [raw-data]
  (let [;; Variables 

        ;; Raw Variables
        trades (:trades raw-data)
        starting-cash (:starting-cash raw-data)

        ;; Processed Variables
        [cash portfolio portfolio-composition-by-date portfolio-value current-value cash-invested cash-invested-by-date stock-performance] (portfolio/analyze-portfolio trades)
        unique-tickers (keys portfolio)
        sorted-portfolio-value (map #(vector (first %) (+ starting-cash (second %)))
                                    (util/sort-map-by-date portfolio-value)) ;; Starting Cash + Each Portfolio Value, by Date
        cash-invested-by-dates (into [] cash-invested-by-date)
        current-portfolio-value (+ starting-cash (+ cash current-value))
        annualized-return (portfolio/calculate-annualized-return starting-cash current-portfolio-value (first (first sorted-portfolio-value))
                                                                 (last (last sorted-portfolio-value)))

        ;; Read from bottom to top for this variable to understand it (Deprecated)
        ;; The reason why I incorporated a lot of different data in this one variable is so that we don't have to fetch from yfinance multiple times (preventing rate limits)
        complete-portfolio-return-data (zipmap
                                        (concat (map #(first %) (rest cash-invested-by-date)) ;; Gets the remaining dates
                                                [(.toString (java.time.LocalDate/now))]) ;; Add the current date as the last entry

                                        ;; (concat 0 ;; Returns this message for the first date of portfolio construction
                                        (mapv #(portfolio/calculate-portfolio-return-and-weights-for-given-date ;; It will have the returns, weights, initial and final portfolio values
                                                (second (first %)) ;; This is the portfolio
                                                (first (first %)) ;; This is the start date
                                                (first (second %)) ;; This is the end date
                                                )
                                              (partition 2 1 (seq ;; This line creates a sliding window with window size = 2, and increment = 1,
                                                              ;; ensuring that the iterator (the map function) is able to see entry at index "i+1" when iterating at index "i" 
                                                              ;; The seq function will guarantee insertion order of entries in map as we are using "array-map"
                                                              (assoc portfolio-composition-by-date ;; Adds the new line into the historical portfolio compositions
                                                                     (.toString (java.time.LocalDate/now)) {}) ;; This line provides the current date with an empty portfolio
                                                              ))))

        cumulative-portfolio-return (portfolio/calculate-portfolio-cumulative-return (map #(:portfolio-cumulative-return %) (vals complete-portfolio-return-data)))


        ;; The following variable holds the cumulative portfolio return for the past 1 year, ending at the past 5 weeks from the current date, and ending at the current date 

        one-year-cumulative-returns-past-five-weeks
        (let [;; Date Parser
              date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
              date-parser (fn [d] (java.time.LocalDate/parse d date-formatter))

              ;; Date Variables
              today (java.time.LocalDate/now)
              all-trade-execution-dates (keys portfolio-composition-by-date) ;; Take All Order Execution Dates (These are all date strings, i.e. "yyyy-MM-dd")
              earliest-trade-execution-date (reduce #(if (neg? (.compareTo %1 %2)) %1 %2) (map date-parser all-trade-execution-dates)) ;; Earliest order execution date (java.time.localDate Type)
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
              one-year-from-five-weeks-ago  (.minusDays five-weeks-ago 365)

              ;; Complete data related to portfolio-composition-by-date from oldest date to today
              set-of-portfolio-complete-data (portfolio/set-of-portfolio-log-returns-and-weights portfolio-composition-by-date
                                                                                                 (.toString one-year-from-five-weeks-ago)
                                                                                                 (.toString today))
              ;; Complete Log Returns Time-Series from oldest date to today
              portfolio-log-returns (:portfolio-log-returns set-of-portfolio-complete-data)

              ;; Function to take log returns from start date to end date, and convert to arithmetic returns
              ;; log-returns-by-date is a map of date strings (i.e. yyyy-MM-dd) as keys, and log returns as values. start-date and end-date are date strings.
              get-cumulative-returns (fn [log-returns-by-date start-date end-date]
                                       (let [;; If start-date is earlier than earliest-trade-execution-date, change start-date to earliest-trade-execution-date
                                             start-date-enhanced (if (.isBefore (date-parser start-date) earliest-trade-execution-date) (.toString earliest-trade-execution-date) start-date)

                                             ;; Additional info: if end-date is greater than current date (today), then dates after current date are not considered in the calculation
                                             
                                             ;; Get all log returns where date is greater or equal to enhanced-start-date and less than end-date
                                             log-returns-filtered-by-date (into {}
                                                                                (filter
                                                                                 (fn [[d log-returns]]
                                                                                   (and (or (.isAfter (date-parser d) (date-parser start-date-enhanced)) (.isEqual (date-parser d) (date-parser start-date-enhanced)))
                                                                                        (or (.isBefore (date-parser d) (date-parser end-date)) (.isEqual (date-parser d) (date-parser end-date)))))
                                                                                 log-returns-by-date))

                                             ;; Get Cumulative Arithmetic Return
                                             cumulative-return (-
                                                                (Math/exp ;; Take the exponential
                                                                 (reduce + ;; Sum all log returns
                                                                         (vals log-returns-filtered-by-date)))
                                                                1)]
                                         cumulative-return))

              ;; 1-Year Portfolio Cumulative returns
              cumulative-return-today (get-cumulative-returns portfolio-log-returns (.toString one-year-ago) (.toString today))
              cumulative-return-one-week-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-one-week-ago) (.toString one-week-ago))
              cumulative-return-two-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-two-weeks-ago) (.toString two-weeks-ago))
              cumulative-return-three-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-three-weeks-ago) (.toString three-weeks-ago))
              cumulative-return-four-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString  one-year-from-four-weeks-ago) (.toString four-weeks-ago))
              cumulative-return-five-weeks-ago (get-cumulative-returns portfolio-log-returns (.toString one-year-from-five-weeks-ago) (.toString five-weeks-ago))]

          {:today (.toString today)
           :one-week-ago (.toString one-week-ago)
           :two-weeks-ago (.toString two-weeks-ago)
           :three-weeks-ago (.toString three-weeks-ago)
           :four-weeks-ago (.toString four-weeks-ago)
           :five-weeks-ago (.toString five-weeks-ago)
           :one-year-cumulative-return-from-today cumulative-return-today
           :one-year-cumulative-return-from-one-week-ago cumulative-return-one-week-ago
           :one-year-cumulative-return-from-two-weeks-ago cumulative-return-two-weeks-ago
           :one-year-cumulative-return-from-three-weeks-ago cumulative-return-three-weeks-ago
           :one-year-cumulative-return-from-four-weeks-ago cumulative-return-four-weeks-ago
           :one-year-cumulative-return-from-five-weeks-ago cumulative-return-five-weeks-ago

           ;; For use in other variables
           :complete-ticker-prices (:all-ticker-prices set-of-portfolio-complete-data)
           :one-year-from-five-weeks-ago (.toString one-year-from-five-weeks-ago)

           ;; Testing purposes only
           :portfolio-value-by-date (:portfolio-value set-of-portfolio-complete-data)})

        ;; Contains usage of deprecated function, will replace with set-of-portfolio-log-returns-and-weights soon
        current-stock-holdings {:values (:current-stock-holdings (get complete-portfolio-return-data (.toString (java.time.LocalDate/now))))
                                :weights (:stock-weights (get complete-portfolio-return-data (.toString (java.time.LocalDate/now))))}

        volatility (portfolio/volatility (map second sorted-portfolio-value))
        portfolio-value-by-day sorted-portfolio-value
        rolling-annualized-volatility (portfolio/rolling-annualized-volatility (map second portfolio-value-by-day) 21)

        ;; Calculates the annualized rolling ewma volatility
        default-rolling-ewma-volatility (portfolio/ewma-rolling-volatility (map second portfolio-value-by-day) 21 0.06) ;; Lambda = 1 - alpha = 0.94
        alternative-rolling-ewma-volatility (portfolio/ewma-rolling-volatility (map second portfolio-value-by-day) 21 0.03) ;; Lambda = 1 - alpha = 0.97

        ;; Graphs

        ;; Alpha and Beta Rolling Regression (Sept 2022 to Present)
        alpha-beta-figs

        (if :show-alpha-beta-regression-by-day
          (let [unique-tickers (loop [data (rest trades)
                                      complete-tickers #{}]
                                 (if (empty? data)
                                   complete-tickers
                                   (recur (rest data)
                                          (let [[date action amount ticker] (first data)]
                                            (-> complete-tickers
                                                (conj ticker))))))]
            (loop [tickers unique-tickers
                   plotly-data []]
              (if (empty? tickers)

                ;; Final Plotly Data
                plotly-data

                ;; Getting the data for Plotly
                (let [ticker (first tickers)

                      ;; The following commented code is used to replace "stock-data" and "market-data" and is optional, 
                      ;; as it shows the data from only "One year from five weeks ago", but uses recycled data so less calls to yfinance

                      ;; Recycle Data from previous computation and change the structure from
                      ;; {"2025-01-31" [$250 $251], "2025-02-01" [$251.25 $249.27], ...} to
                      ;; [["2025-01-31" $250 $251], ["2025-02-01" $251.25 $249.27], ...]
                      ;; stock-data (mapv 
                      ;;             (fn [[k v]]
                      ;;               (into [k] v)) ;; Flatten the data to the new structure
                      ;;             (get (:complete-ticker-prices one-year-cumulative-returns-past-five-weeks) ticker)) ;; Recycle Data

                      ;; market-data (client/get-ticker-price-with-end "^GSPC" (:one-year-from-five-weeks-ago one-year-cumulative-returns-past-five-weeks)
                      ;;                                               (.toString (java.time.LocalDate/now))) ;; Switch the Market Index here
                      
                      stock-data (client/get-ticker-price-with-end ticker "2022-09-01" (.toString (java.time.LocalDate/now))) ;; Note that the date is start date
                      market-data (client/get-ticker-price-with-end "^GSPC" "2022-09-01" (.toString (java.time.LocalDate/now))) ;; Switch the Market Index here
                      model-data (reg/get-alpha-beta stock-data market-data)
                      alpha-data (map vector (:plotted-dates model-data) (:plotted-alpha model-data))
                      beta-data (map vector (:plotted-dates model-data) (:plotted-beta model-data))]
                  (recur (disj tickers ticker)
                         (-> plotly-data
                             (conj {:x (map first alpha-data)
                                    :y (map second alpha-data)
                                    :type "scatter"
                                    :mode "lines"
                                    :name (str ticker " α")})
                             (conj {:x (map first beta-data)
                                    :y (map second beta-data)
                                    :type "scatter"
                                    :mode "lines"
                                    :name (str ticker " β")})))))))
          "")

        ;; Stock Performance (Log Returns based on Closing Price)
        one-dollar-invested-at-time-zero
        (let [;; The following is a map where keys are the complete tickers from the creation of the first portfolio to current portfolio
              ;; The values are also maps where the keys are the trade dates from one-year-from-five-weeks-ago to today, and values are vectors containing the opening price and closing price
              ;; Data is already sorted by time.
              ;; The following is the form of the data: {"NVDA" {"2025-01-31" [$250 $251], "2025-02-01" [$251.25 $249.27], ...}, 
              ;;                                         "MSFT" {"2025-01-31" [$172 $180], "2025-02-01" [$177 $175], ...}, ...}
              prices-until-end-date (:complete-ticker-prices one-year-cumulative-returns-past-five-weeks)

              ;; The following will return each stock and their logged dollar performance over time in the following format.
              ;; {"NVDA" {"2025-01-31" 0, "2025-02-01" 0.006, ...}, 
              ;;  "MSFT" {"2025-01-31" 0, "2025-02-01" -0.0255, ...}, ...}

              log-dollar-performance (loop
                                      [tickers (keys prices-until-end-date)
                                       one-dollar-performance {} ;; Dollar Performance but not logged 
                                       log-one-dollar-performance {}]
                                       (if (empty? tickers)

                                         log-one-dollar-performance

                                         (recur
                                          (rest tickers) ;; Remove first ticker in collection

                                          ;; Add new ticker with its dollar performance to variable one-dollar-performance
                                          ;; It will have the tickers as the key, and values which are maps with keys being the trade dates, and values being the dollar performance
                                          ;; e.g. {"NVDA" {"2025-01-31" 1, "2025-02-01" 1.16, ...}, 
                                          ;;       "MSFT" {"2025-01-31" 1, "2025-02-01" 0.98, ...}, ...}

                                          (assoc one-dollar-performance (first tickers)
                                                 (util/sort-map-by-date
                                                  (zipmap
                                                   (keys
                                                    (get prices-until-end-date (first tickers))) ;; Get the dates for the price data 
                                                   (map #(/ % (second (first (vals (get prices-until-end-date (first tickers)))))) ;; Divide all closing prices by the first trade date closing price to get one dollar performance 
                                                        (map second ;; Just take closing prices for all (I think taking open price for first date introduces unnecessary complexity and confusion) 
                                                             (vals
                                                              (get prices-until-end-date (first tickers)))) ;; Get the price data for the first ticker in collection)
                                                        ))))

                                          ;; Now, just do the same things as the above, but take the natural logarithm 
                                          (assoc log-one-dollar-performance (first tickers)
                                                 (util/sort-map-by-date
                                                  (zipmap
                                                   (keys
                                                    (util/sort-map-by-date (get prices-until-end-date (first tickers)))) ;; Get the dates for the price data 
                                                   (map #(Math/log (/ % (second (first (vals (get prices-until-end-date (first tickers))))))) ;; Divide all closing prices by the first trade date closing price to get one dollar performance, and take the natural logarithm 
                                                        (map second ;; Just take closing prices for all (I think taking open price for first date introduces unnecessary complexity and confusion) 
                                                             (vals
                                                              (util/sort-map-by-date (get prices-until-end-date (first tickers))))) ;; Get the price data for the first ticker in collection) 
                                                        )))))))

              ;; This will hold a map of all tickers currently and previously existing in the portfolio, and the graphs of its log returns
              ;; The data format will be the following:
              ;; {"NVDA" {:x ["2025-01-01" "2025-01-02" ...]
              ;;          :y [0.123 -0.234 ...]
              ;;          :type "scatter"
              ;;          :mode "lines"
              ;;          :name "NVDA Performance")},
              ;;  "MSFT" {...},}

              plotly-data (loop
                           [tickers (keys log-dollar-performance)
                            plotly-data {}]

                            (if (empty? tickers)

                              plotly-data

                              (recur
                               (rest tickers) ;; Remove first ticker in collection
                               (assoc plotly-data (first tickers)
                                      {:x (keys (get log-dollar-performance (first tickers))) ;; Already ordered
                                       :y (vals (get log-dollar-performance (first tickers))) ;; Already ordered
                                       :type "scatter"
                                       :mode "lines"
                                       :name (str (first tickers) " Performance")}))))]
          plotly-data)

        ;; Portfolio Value by Day
        portfolio-value-figs

        {:x (map #(first %) portfolio-value-by-day)
         :y (map #(second %) portfolio-value-by-day)
         :type "scatter"
         :mode "lines"
         :name "Portfolio Value by Day"}

        ;; 30-Day Rolling Annualized Volatility of Portfolio (Historical Standard Deviation)
        rolling-annualized-volatility-figs

        {:x (drop 21 (map #(first %) portfolio-value-by-day)) ;; drop the first 21 elements because window size is 21 days
         :y rolling-annualized-volatility
         :type "scatter"
         :mode "lines"
         :name "30-Day Rolling Annualized Volatility of Portfolio"}

        ;; 30-Day EWMA Rolling Volatility of Portfolio (Default is lambda = 0.94)
        default-rolling-ewma-volatility-figs

        {:x (map #(first %) portfolio-value-by-day)
         :y (concat (repeat 21 nil) default-rolling-ewma-volatility) ;; The first 30 days (21 trading days) will have no value because not enough data to compute volatility
         :type "scatter"
         :mode "lines"
         :name "30-Day EWMA Rolling Volatility of Portfolio (λ = 0.94)"}

        ;; 30-Day EWMA Rolling Volatility of Portfolio (Alternative is lambda = 0.97)
        alternative-rolling-ewma-volatility-figs

        {:x (map #(first %) portfolio-value-by-day)
         :y (concat (repeat 21 nil) alternative-rolling-ewma-volatility) ;; The first 30 days (21 trading days) will have no value because not enough data to compute volatility
         :type "scatter"
         :mode "lines"
         :name "30-Day EWMA Rolling Volatility of Portfolio (λ = 0.97)"}]
    
    
    
    {:current-portfolio-value current-portfolio-value
     :cash (+ starting-cash cash)
     :stocks (- current-portfolio-value (+ starting-cash cash))
     :unique-tickers unique-tickers

     :annualized-portfolio-return (* annualized-return 100)
     :portfolio-volatility volatility
     :annualized-portfolio-volatility (* (math/sqrt 252) volatility)

     :stocks-held-and-shorted portfolio
     :cash-invested cash-invested
     :cumulative-portfolio-return (* cumulative-portfolio-return 100)
     :current-stock-holdings current-stock-holdings

     :portfolio-returns-by-date complete-portfolio-return-data ;; This gives the final portfolio returns and values before a new trade is executed, which changes the composition of the portfolio
     :portfolio-value-by-day portfolio-value-by-day ;; This gives the final portfolio values, only takes into account values after change in portfolio composition, and includes cash
     :individual-stock-performance-by-day stock-performance

     :past-five-weeks-1y-cumulative-return one-year-cumulative-returns-past-five-weeks

     :rolling-annualized-volatility rolling-annualized-volatility ;; Window size is 21 days (no. of trading days/month)

     ;; Plotly Graphs

     :alpha-beta-figs alpha-beta-figs
     :stock-performances-graphs one-dollar-invested-at-time-zero
     :portfolio-value-figs portfolio-value-figs
     :rolling-annualized-volatility-figs rolling-annualized-volatility-figs
     :default-rolling-ewma-volatility-figs default-rolling-ewma-volatility-figs
     :alternative-rolling-ewma-volatility-figs alternative-rolling-ewma-volatility-figs

     ;; Test Data (will delete later)
     :test-data default-rolling-ewma-volatility}))