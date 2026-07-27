(ns cim-portfolio.portfoliofunctions-test
  (:require [clojure.test :refer :all]
            [cim_portfolio.portfoliofunctions :as pf]))

(defn- approx= [a b]
  (< (Math/abs (- (double a) (double b))) 1e-9))

(deftest ewma-rolling-volatility-short-history-test
  (testing "yields a value for every daily return, even with far fewer than 21 days of data"
    (let [vols (pf/ewma-rolling-volatility [100 101 99 102] 0.06)]
      (is (= 3 (count vols)))
      (is (every? pos? vols)))))

(deftest ewma-rolling-volatility-recursion-test
  (testing "matches the RiskMetrics recursion var_t = (1 - alpha) * var_(t-1) + alpha * r_t^2, seeded with r_1^2"
    (let [prices [100.0 102.0 101.0 103.0]
          alpha 0.06
          [r1 r2 r3] (map (fn [[a b]] (- (/ b a) 1.0)) (partition 2 1 prices))
          v1 (* r1 r1)
          v2 (+ (* (- 1 alpha) v1) (* alpha (* r2 r2)))
          v3 (+ (* (- 1 alpha) v2) (* alpha (* r3 r3)))
          expected (map #(* 100 (Math/sqrt 252) (Math/sqrt %)) [v1 v2 v3])
          actual (pf/ewma-rolling-volatility prices alpha)]
      (is (every? true? (map approx= actual expected))))))

(deftest ewma-rolling-volatility-no-returns-test
  (testing "nil when fewer than two price points, since no return exists yet"
    (is (nil? (pf/ewma-rolling-volatility [] 0.06)))
    (is (nil? (pf/ewma-rolling-volatility [100] 0.06)))))

(deftest rolling-sharpe-ratio-alignment-test
  (testing "pairs each 21-day mean return with the EWMA volatility of the window's last day"
    (let [prices (mapv double (range 100 130)) ;; 30 price points -> 29 daily returns -> 9 complete 21-day windows
          vols (pf/ewma-rolling-volatility prices 0.06)
          sharpe (pf/rolling-sharpe-ratio prices vols 21)]
      (is (= 9 (count sharpe)))
      (is (every? some? sharpe)))))

(deftest sum-pnl-series-with-forward-fill-test
  (testing "a holding's PnL is carried forward on days its market is closed instead of vanishing"
    (let [us {"2026-01-05" 100.0 "2026-01-06" 150.0 "2026-01-07" 120.0}
          jp {"2026-01-05" 50.0 "2026-01-07" 80.0}] ;; closed on 2026-01-06
      (is (= {"2026-01-05" 150.0 "2026-01-06" 200.0 "2026-01-07" 200.0}
             (pf/sum-pnl-series-with-forward-fill [us jp])))))
  (testing "a series contributes nothing before its trade's first date"
    (let [early {"2026-01-05" 10.0 "2026-01-06" 20.0}
          late {"2026-01-06" 5.0}]
      (is (= {"2026-01-05" 10.0 "2026-01-06" 25.0}
             (pf/sum-pnl-series-with-forward-fill [early late])))))
  (testing "no trades yields an empty map"
    (is (= {} (pf/sum-pnl-series-with-forward-fill [])))))

(deftest rolling-sharpe-ratio-zero-volatility-test
  (testing "returns nil instead of dividing by zero when the portfolio value never moved"
    (let [prices (repeat 25 100.0)
          vols (pf/ewma-rolling-volatility prices 0.06)
          sharpe (pf/rolling-sharpe-ratio prices vols 21)]
      (is (seq sharpe))
      (is (every? nil? sharpe)))))
