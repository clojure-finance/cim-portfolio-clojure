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

(deftest bias-corrected-ewma-test
  (testing "the first value is the first observation itself, for any alpha"
    (is (approx= 5.0 (first (pf/bias-corrected-ewma 0.01 [5.0 7.0]))))
    (is (approx= 5.0 (first (pf/bias-corrected-ewma 0.5 [5.0 7.0])))))
  (testing "a constant series stays exactly constant instead of drifting off a seed"
    (is (every? #(approx= 3.0 %) (pf/bias-corrected-ewma 0.06 (repeat 10 3.0)))))
  (testing "with near-total memory the running value approaches the plain average"
    (let [[_ m2] (pf/bias-corrected-ewma 1e-9 [1.0 2.0])]
      (is (< (Math/abs (- m2 1.5)) 1e-6)))))

(deftest ewma-rolling-volatility-recursion-test
  (testing "matches the bias-corrected EWMA of squared returns: var_t = sum(lambda^i * r_(t-i)^2) / sum(lambda^i)"
    (let [prices [100.0 102.0 101.0 103.0]
          alpha 0.06
          lambda (- 1 alpha)
          [r1 r2 r3] (map (fn [[a b]] (- (/ b a) 1.0)) (partition 2 1 prices))
          v1 (* r1 r1)
          v2 (/ (+ (* r2 r2) (* lambda r1 r1)) (+ 1 lambda))
          v3 (/ (+ (* r3 r3) (* lambda r2 r2) (* lambda lambda r1 r1)) (+ 1 lambda (* lambda lambda)))
          expected (map #(* 100 (Math/sqrt 252) (Math/sqrt %)) [v1 v2 v3])
          actual (pf/ewma-rolling-volatility prices alpha)]
      (is (every? true? (map approx= actual expected))))))

(deftest ewma-rolling-volatility-no-returns-test
  (testing "nil when fewer than two price points, since no return exists yet"
    (is (nil? (pf/ewma-rolling-volatility [] 0.06)))
    (is (nil? (pf/ewma-rolling-volatility [100] 0.06)))))

(deftest ewma-sharpe-ratio-alignment-test
  (testing "yields one entry per daily return, masked as nil until sharpe-min-periods returns exist"
    (let [prices (mapv double (range 100 130)) ;; 30 price points -> 29 daily returns
          sharpe (pf/ewma-sharpe-ratio prices 0.06)]
      (is (= 29 (count sharpe)))
      (is (every? nil? (take (dec pf/sharpe-min-periods) sharpe)))
      (is (every? some? (drop (dec pf/sharpe-min-periods) sharpe))))))

(deftest ewma-sharpe-ratio-recursion-test
  (testing "the mean return is the bias-corrected EWMA with the volatility's alpha: mean_t = sum(lambda^i * r_(t-i)) / sum(lambda^i)"
    (let [prices [100.0 102.0 101.0 103.0]
          alpha 0.06
          lambda (- 1 alpha)
          [r1 r2 r3] (map (fn [[a b]] (- (/ b a) 1.0)) (partition 2 1 prices))
          m1 r1
          m2 (/ (+ r2 (* lambda r1)) (+ 1 lambda))
          m3 (/ (+ r3 (* lambda r2) (* lambda lambda r1)) (+ 1 lambda (* lambda lambda)))
          vols (pf/ewma-rolling-volatility prices alpha)
          expected (map (fn [m vol] (/ (* 252 m) (/ vol 100))) [m1 m2 m3] vols)
          actual (pf/ewma-sharpe-ratio prices alpha 1)]
      (is (= 3 (count actual)))
      (is (every? true? (map approx= actual expected))))))

(deftest ewma-alpha-for-window-test
  (testing "maps a window length to the span-equivalent alpha: alpha = 2 / (window + 1)"
    (is (approx= 0.5 (pf/ewma-alpha-for-window 3)))
    (is (approx= (/ 2.0 253) (pf/ewma-alpha-for-window 252))))
  (testing "a one-year window gives a decay near lambda = 0.992"
    (is (< 0.991 (- 1 (pf/ewma-alpha-for-window 252)) 0.993))))

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

(deftest ewma-sharpe-ratio-zero-volatility-test
  (testing "returns nil instead of dividing by zero when the portfolio value never moved"
    (let [sharpe (pf/ewma-sharpe-ratio (repeat 25 100.0) 0.06)]
      (is (seq sharpe))
      (is (every? nil? sharpe))))
  (testing "nil when fewer than two price points, since no return exists yet"
    (is (nil? (pf/ewma-sharpe-ratio [100.0] 0.06)))))
