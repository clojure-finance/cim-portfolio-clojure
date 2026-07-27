(ns cim-portfolio.regression-test
  (:require [clojure.test :refer [deftest is testing]]
            [cim_portfolio.regression :as reg]))

(defn- dates [n]
  (let [start (java.time.LocalDate/parse "2025-01-01")]
    (mapv #(.toString (.plusDays start (long %))) (range n))))

(defn- rows [ds prices]
  (mapv (fn [d p] [d p p]) ds prices)) ;; [date open close], like get-ticker-price-all

(deftest get-alpha-beta-recovers-known-coefficients-test
  (testing "a stock whose returns are exactly twice the market's regresses to beta 2, alpha 0"
    (let [n 300
          ds (dates n)
          market-prices (mapv #(+ 100.0 (* 10.0 (Math/sin (* 0.1 %)))) (range n))
          market-returns (mapv (fn [[a b]] (- (/ b a) 1.0)) (partition 2 1 market-prices))
          stock-prices (vec (reductions (fn [p r] (* p (+ 1.0 (* 2.0 r)))) 100.0 market-returns))
          {:keys [plotted-dates plotted-alpha plotted-beta]}
          (reg/get-alpha-beta (rows ds stock-prices) (rows ds market-prices))]
      (is (= (count plotted-dates) (count plotted-alpha) (count plotted-beta)))
      (is (= (- (- n 1) 251) (count plotted-dates))) ;; one 252-return window per plotted date
      (is (every? #(< (Math/abs (- % 2.0)) 1e-6) plotted-beta))
      (is (every? #(< (Math/abs %) 1e-6) plotted-alpha)))))

(deftest get-alpha-beta-short-history-test
  (testing "a stock younger than the 252-day window yields empty series instead of throwing"
    (let [ds (dates 50)
          prices (mapv #(+ 100.0 (* 0.5 %)) (range 50))
          result (reg/get-alpha-beta (rows ds prices) (rows ds prices))]
      (is (= [] (:plotted-dates result)))
      (is (= [] (:plotted-alpha result)))
      (is (= [] (:plotted-beta result))))))

(deftest get-alpha-beta-calendar-join-test
  (testing "stock and market returns are joined on common dates when the calendars differ"
    (let [n 300
          ds (dates n)
          prices (mapv #(+ 100.0 (* 10.0 (Math/sin (* 0.1 %)))) (range n))
          market (rows ds prices)
          missing-date (ds 150) ;; the stock's market is closed that day
          stock (vec (remove #(= missing-date (first %)) (rows ds prices)))
          {:keys [plotted-dates plotted-alpha plotted-beta]} (reg/get-alpha-beta stock market)]
      (is (= (count plotted-dates) (count plotted-alpha) (count plotted-beta)))
      ;; 299 stock rows -> 298 stock returns, all on market dates -> windows over 298 common returns
      (is (= (- 298 251) (count plotted-dates)))
      (is (not-any? #(= missing-date %) plotted-dates)))))
