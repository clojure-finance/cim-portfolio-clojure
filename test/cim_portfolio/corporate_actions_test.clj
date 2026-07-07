(ns cim-portfolio.corporate-actions-test
  (:require [clojure.test :refer :all]
            [cim_portfolio.corporate-actions :as actions]))

;; SBS-style 5:1 split, effective 2026-05-07 (timestamp = market open that day)
(def splits {"SBS" {:1778160600 {:date 1778160600 :numerator 5.0 :denominator 1.0 :splitRatio "5:1"}}})

(deftest adjust-trades-for-splits-test
  (testing "Pre-split buy with user-supplied price: amount multiplied, price divided"
    (is (= [["Date" "Action" "Amount" "Ticker" "Price"]
            ["2026-03-02" "buy" "500.0" "SBS" "5.76"]]
           (actions/adjust-trades-for-splits
            [["Date" "Action" "Amount" "Ticker" "Price"]
             ["2026-03-02" "buy" "100" "SBS" "28.80"]]
            splits))))

  (testing "Pre-split trade without price column: only amount adjusted, row stays 4 columns"
    (is (= [["h"] ["2026-03-02" "buy" "500.0" "SBS"]]
           (actions/adjust-trades-for-splits [["h"] ["2026-03-02" "buy" "100" "SBS"]] splits))))

  (testing "Trade on the split's effective date is already post-split and untouched"
    (is (= [["h"] ["2026-05-07" "buy" "100" "SBS" "6.58"]]
           (actions/adjust-trades-for-splits [["h"] ["2026-05-07" "buy" "100" "SBS" "6.58"]] splits))))

  (testing "Post-split trade untouched"
    (is (= [["h"] ["2026-06-01" "sell" "200" "SBS"]]
           (actions/adjust-trades-for-splits [["h"] ["2026-06-01" "sell" "200" "SBS"]] splits))))

  (testing "Ticker without split events untouched"
    (is (= [["h"] ["2026-03-02" "buy" "10" "NVDA" "180"]]
           (actions/adjust-trades-for-splits [["h"] ["2026-03-02" "buy" "10" "NVDA" "180"]] splits))))

  (testing "Malformed short rows pass through unchanged instead of crashing"
    (is (= [["h"] ["2026-03-02" "buy"]]
           (actions/adjust-trades-for-splits [["h"] ["2026-03-02" "buy"]] splits))))

  (testing "Slash date format accepted (normalized via util/parse-date)"
    (is (= [["h"] ["2026/03/02" "buy" "500.0" "SBS"]]
           (actions/adjust-trades-for-splits [["h"] ["2026/03/02" "buy" "100" "SBS"]] splits)))))
