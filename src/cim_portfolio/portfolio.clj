;;; # CIM Portfolio
;;; karanvs@connect.hku.hk Karanveer Singh  
;;; edwardaw@connect.hku.hk Edward Widjaja
;;; ### Required packages
^{:clay {:hide-info-line true
         :hide-ui-header true}}

(ns cim_portfolio.portfolio
  (:require [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
            [cim_portfolio.corporate-actions :as actions]
            [cim_portfolio.plot :as plot]
            [clojure.math :as math]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cim_portfolio.regression :as reg]
            [cim_portfolio.portfoliofunctions :as portfolio]
            [nextjournal.clerk :as clerk]))

;;; ### Program Configuration

(def portfolio-options {:starting-cash 200000})

(def view-options {:show-individual-stock-performance-by-day true
                   :show-cumulative-portfolio-return-by-day true
                   :show-portfolio-value-by-day true
                   :show-alpha-beta-regression-by-day true})

;; Please enter the relative paths of 1 or more CSV files (in a list) containing your trades below:

(def input-files ["./examples/testPortfolio1.csv"])

;; ### Portfolio Performance

(let [data (actions/adjust-trades-for-splits (util/read-multiple-csv input-files)) ;; Normalize share amounts to post-split units, consistent with Yahoo's split-adjusted prices
      [cash portfolio portfolio-value current-value cash-invested cash-invested-by-date stock-performance] (portfolio/analyze-portfolio data)
      sorted-portfolio-value 	(map #(vector (first %) (+ (:starting-cash portfolio-options) (second %)))
                                   (util/sort-map-by-date portfolio-value))
      cash-invested-by-dates 	(into [] cash-invested-by-date)
      current-portfolio-value 	(+ (:starting-cash portfolio-options) (+ cash current-value))
      annualized-return 		(portfolio/calculate-annualized-return (:starting-cash portfolio-options) current-portfolio-value (first (first sorted-portfolio-value)) (last (last sorted-portfolio-value)))
      cumulative-portfolio-return (portfolio/calculate-portfolio-return cash-invested stock-performance)
      returns-by-date 			(zipmap (map #(first %) cash-invested-by-date) (mapv #(portfolio/calculate-portfolio-return-for-given-date (second %) stock-performance (first %)) cash-invested-by-dates))
      volatility 				(portfolio/volatility (map second sorted-portfolio-value))]

  ;; Bad practice but better than storing all of the above vars as global
  (def portfolio-value-by-day sorted-portfolio-value)

  ;; Portfolio Basic Performance
  (str "Current Portfolio Value: $" (format "%.2f" current-portfolio-value)
       " [Cash: ~$" (format "%.2f" (+ (:starting-cash portfolio-options) cash))
       "| Stocks: ~$" (format "%.2f" (- current-portfolio-value (+ (:starting-cash portfolio-options) cash))) "] \n\n"

       "Annualized Return of portfolio: " (format "%.2f" (* annualized-return 100)) "%\n\n"

       "Volatility of portfolio: " (format "%.4f" volatility) "%\n\n"
       "Annualized volatility of portfolio: " (format "%.4f" (* (Math/sqrt 252) volatility)) "%\n\n"

       "Portfolio (units held/shorted of each stock): " (pr-str portfolio) "\n\n"
       "Cash invested in each stock: " (pr-str cash-invested) "\n\n"
       "Cumulative Portfolio Return: " (pr-str cumulative-portfolio-return) "\n\n"

       "----------------------------------\n"
       (if (:show-cumulative-portfolio-return-by-day view-options)
         (str "Portfolio Return by date: \n" (pr-str returns-by-date) "\n")
         "")
       "----------------------------------\n\n"

       "----------------------------------\n"
       "Portfolio Value Day-by-Day: \n"
       (if (:show-portfolio-value-by-day view-options)
         (str (clojure.string/join "\n" (map #(str (first %) " " (format "%.2f" (second %))) sorted-portfolio-value)) "\n")
         "Omitting...\n")
       "----------------------------------\n\n"

       "----------------------------------\n"
       "Individual Stock Performance: \n"
       (if (:show-individual-stock-performance-by-day view-options)
         (with-out-str (clojure.pprint/pprint stock-performance))
         "Omitting...\n")
       "----------------------------------\n"))

;; ### Visualization
;; #### Portfolio Value
(plot/list-plot (cons (:starting-cash portfolio-options) (map #(second %) portfolio-value-by-day)) :joined true :plot-size 800 :x-title "Day" :y-title "Portfolio Value (in $)" :color "#7f3b08")

;; #### 30 Day Rolling Annualized Volatility of Portfolio

; 21 trading days in a month therefore 21 window-size used

(plot/list-plot (portfolio/rolling-annualized-volatility (map second portfolio-value-by-day) 21)
                :joined true :plot-size 800 :x-title "Time" :y-title "Annualized Volatility (in %)" :color "red")

;; #### Alpha and Beta Rolling Regression (Sept 2022 to Present)
(if :show-alpha-beta-regression-by-day
  (let [unique-tickers (loop [data (rest (util/read-multiple-csv input-files))
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
        (let [n (count plotly-data)
              cols 1 ;; Edit number of columns here
              rows (int (Math/ceil (/ n cols)))
              traces (map-indexed
                      (fn [i trace]
                        (let [row (inc (int (/ i cols)))
                              col (inc (mod i cols))
                              xaxis (str "x" (+ (* (- row 1) cols) col))
                              yaxis (str "y" (+ (* (- row 1) cols) col))]
                          (assoc trace :xaxis xaxis :yaxis yaxis)))
                      plotly-data)
              layout {:grid {:rows rows :columns cols :pattern "independent"}
                      :width (* 750 cols)
                      :height (* 375 rows)
                      :margin {:l 70 :r 20 :b 70 :t 20} ; Further increase left and bottom margins
                      :paper_bgcolor "transparent"
                      :plot_bgcolor "transparent"}]
          (clerk/plotly {:data traces :layout layout
                         :config {:displayModeBar false
                                  :displayLogo false}}))

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
