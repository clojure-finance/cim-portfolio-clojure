;;; # CIM Portfolio 
;;; ewidjaja@hku.hk Edward Widjaja

;;; This is the backend logic (model) to generate the results on the website

(ns portfolio-web.model 
  (:require [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
            [cim_portfolio.plot :as plot]
            [clojure.math :as math]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cim_portfolio.regression :as reg]
            [cim_portfolio.portfoliofunctions :as portfolio]
            [nextjournal.clerk :as clerk]
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


        ;; The following variable holds the cumulative portfolio return for the past 1 year, ending at the past 5 weeks from the current date

        ;;  cumulative-portfolio-return-last-week 
        ;;  (let [today (java.time.LocalDate/now)
        ;;        date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
        ;;        ;; all-ticker-prices (:all-ticker-prices (get complete-portfolio-return-data (.toString today)))

        ;;        all-ticker-prices (into {}
        ;;                                (map (fn [[ticker amount]]
        ;;                                       [ticker (client/get-ticker-price-with-end ticker (first (keys portfolio-composition-by-date)) (.toString today))])
        ;;                                     portfolio))

        ;;        all-trade-execution-dates (map #(java.time.LocalDate/parse % date-formatter) (keys complete-portfolio-return-data)) ;; Execution date for trade orders

        ;;        all-trade-date (map #(java.time.LocalDate/parse % date-formatter) (map #(first %) (get all-ticker-prices (first unique-tickers)))) ;; Get all trade dates from start of portfolio to today


        ;;        last-week (reduce (fn [best d] ;; Get the most recent trading day 7 days ago (or other date as required)
        ;;                            (if (and (.isBefore d (.minus today (java.time.Period/ofDays 7))) ;; Is the trading date before the date 7 days ago?
        ;;                                     (or (nil? best) (.isAfter d best)))
        ;;                              d
        ;;                              best))
        ;;                          nil all-trade-date)


        ;;        closest-trade-date (reduce (fn [best d] ;; Get the latest order date before the date in question (1 week ago, 2 weeks ago, ...)
        ;;                                     (if (and (.isBefore d last-week) ;; There will be a problem if the closest trade date is the exact same date as a week before today (future RA please fix hehe) 
        ;;                                              (or (nil? best) (.isAfter d best)))
        ;;                                       d
        ;;                                       best))
        ;;                                   nil all-trade-execution-dates)

        ;;        initial-market-values (into {}
        ;;                                    (map (fn [[ticker amount]]
        ;;                                           [ticker (* amount ;; Obtain the market value of the portfolio's holdings
        ;;                                                      (second  ;; Get the opening price! Don't get the closing price.
        ;;                                                       (first ;; filter returns an array, so need to get first result
        ;;                                                        (filter #(= (first %) (.toString closest-trade-date)) (get all-ticker-prices ticker)) ;; Get the price of all stocks on the trade date or date 1 year back, whichever is greater
        ;;                                                        )))])
        ;;                                         (get portfolio-composition-by-date (.toString closest-trade-date))))

        ;;        current-market-values (into {}
        ;;                                    (map (fn [[ticker amount]]
        ;;                                           [ticker (* amount ;; Obtain the market value of the portfolio's holdings
        ;;                                                      (last  ;; Get the closing price.
        ;;                                                       (first ;; filter returns an array, so need to get first result
        ;;                                                        (filter #(= (first %) (.toString last-week)) (get all-ticker-prices ticker)) ;; Get the price of all stocks on the date in question
        ;;                                                        )))])
        ;;                                         (get portfolio-composition-by-date (.toString closest-trade-date))))

        ;;        arithmetic-return-since-last-trade (- (/ (reduce + (vals current-market-values)) (reduce + (vals initial-market-values))) 1)

        ;;        one-year-cumulative-return-to-date (->> complete-portfolio-return-data
        ;;                                                (filter (fn [[k v]] (.isBefore (java.time.LocalDate/parse k date-formatter) last-week)))
        ;;                                                (map second)
        ;;                                                (map :portfolio-cumulative-return)
        ;;                                                (vec)
        ;;                                                (concat [arithmetic-return-since-last-trade])
        ;;                                                (portfolio/calculate-portfolio-cumulative-return)
        ;;                                                )
        ;;        ]
        ;;    one-year-cumulative-return-to-date) 
        
        test-data (portfolio/portfolio-log-and-cumulative-returns (get portfolio-composition-by-date "2025-01-10")
                                                                   "2025-01-10" (.toString (java.time.LocalDate/now)))

        current-stock-holdings {:values (:current-stock-holdings (get complete-portfolio-return-data (.toString (java.time.LocalDate/now))))
                                :weights (:stock-weights (get complete-portfolio-return-data (.toString (java.time.LocalDate/now))))}

        volatility (portfolio/volatility (map second sorted-portfolio-value))
        portfolio-value-by-day sorted-portfolio-value
        rolling-annualized-volatility (portfolio/rolling-annualized-volatility (map second portfolio-value-by-day) 21)

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

                ;; Graphing
                plotly-data

                ;; Getting the data for Plotly
                (let [ticker (first tickers)
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

        ;; Portfolio Value by Day
        portfolio-value-figs

        {:x (map #(first %) portfolio-value-by-day)
         :y (map #(second %) portfolio-value-by-day)
         :type "scatter"
         :mode "lines"
         :name "Portfolio Value by Day"}

        rolling-annualized-volatility-figs

        ;; 30-Day Rolling Annualized Volatility of Portfolio
        {:x (drop 21 (map #(first %) portfolio-value-by-day)) ;; drop the first 21 elements because window size is 21 days
         :y rolling-annualized-volatility
         :type "scatter"
         :mode "lines"
         :name "30-Day Rolling Annualized Volatility of Portfolio"}]



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

     :one-week-ago-return test-data ;; This is a 1-year return ending at the date last week

     :rolling-annualized-volatility rolling-annualized-volatility ;; Window size is 21 days (no. of trading days/month)

     ;; Plotly Graphs

     :alpha-beta-figs alpha-beta-figs
     :portfolio-value-figs portfolio-value-figs
     :rolling-annualized-volatility-figs rolling-annualized-volatility-figs

     ;; Test Data (will delete later)
     :test-data (:all-ticker-prices (get complete-portfolio-return-data (.toString (java.time.LocalDate/now))))}))