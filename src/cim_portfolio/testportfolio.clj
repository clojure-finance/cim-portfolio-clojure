(ns cim_portfolio.testportfolio
  (:require [cim_portfolio.util :as util]
            [cim_portfolio.yfinanceclient :as client]
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

(let [data (util/read-multiple-csv input-files)
      [cash portfolio portfolio-composition-by-date portfolio-value current-value cash-invested cash-invested-by-date stock-performance] (portfolio/analyze-portfolio data)
      sorted-portfolio-value 	(map #(vector (first %) (+ (:starting-cash portfolio-options) (second %)))
                                   (util/sort-map-by-date portfolio-value))
      cash-invested-by-dates 	(into [] cash-invested-by-date) ;; Cash invested cumulatively to each stock by each trade date
      current-portfolio-value 	(+ (:starting-cash portfolio-options) (+ cash current-value))
      annualized-return 		(portfolio/calculate-annualized-return (:starting-cash portfolio-options) current-portfolio-value (first (first sorted-portfolio-value)) (last (last sorted-portfolio-value)))
      cumulative-portfolio-return (portfolio/calculate-portfolio-return cash-invested stock-performance)
      returns-by-date 			(zipmap (map #(first %) cash-invested-by-date) (mapv #(portfolio/calculate-portfolio-return-for-given-date (second %) stock-performance (first %)) cash-invested-by-dates))
      volatility 				(portfolio/volatility (map second sorted-portfolio-value))]

  ;; Bad practice but better than storing all of the above vars as global
  (def portfolio-value-by-day sorted-portfolio-value)
  
  stock-performance)