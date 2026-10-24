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

;; Yahoo's booking of the CTVA/VYLR spin-off: a 6665:1000 "split" stamped 2026-10-01 13:30 UTC
(def ctva-fake-split {:1790568000 {:date 1790861400 :numerator 6665.0 :denominator 1000.0 :splitRatio "6665:1000"}})

(deftest separate-spin-offs-test
  (testing "Known spin-off is moved out of the splits, other tickers' splits are kept"
    (is (= {:splits {"CTVA" {} "SBS" (splits "SBS")}
            :spin-offs {"CTVA" [{:date "2026-10-01" :ratio 6.665}]}}
           (actions/separate-spin-offs (assoc splits "CTVA" ctva-fake-split)))))

  (testing "Ticker lookup ignores case"
    (is (= {"ctva" [{:date "2026-10-01" :ratio 6.665}]}
           (:spin-offs (actions/separate-spin-offs {"ctva" ctva-fake-split})))))

  (testing "A real split of a listed ticker on another date stays a split"
    (is (= {:splits {"CTVA" (splits "SBS")} :spin-offs {}}
           (actions/separate-spin-offs {"CTVA" (splits "SBS")}))))

  (testing "No splits reported (nil or failed fetch): nothing to undo"
    (is (= {:splits {"CTVA" {}} :spin-offs {}}
           (actions/separate-spin-offs {"CTVA" nil}))))

  (testing "Pre-spin CTVA trade keeps its share count once the fake split is removed"
    (is (= [["h"] ["2026-09-15" "buy" "100" "CTVA" "80"]]
           (actions/adjust-trades-for-splits
            [["h"] ["2026-09-15" "buy" "100" "CTVA" "80"]]
            (:splits (actions/separate-spin-offs {"CTVA" ctva-fake-split})))))))

(deftest undo-spin-off-price-adjustment-test
  (let [rows [["2026-09-30" 11.0 11.65] ["2026-10-01" 14.44 12.57] ["2026-10-02" 12.5 12.6]]]
    (testing "Rows before the ex-date are multiplied back; the ex-date and later are untouched"
      (let [[[d o c] ex later] (actions/undo-spin-off-price-adjustment rows [{:date "2026-10-01" :ratio 6.665}])]
        (is (= "2026-09-30" d))
        (is (< (abs (- c 77.648)) 0.01))
        (is (< (abs (- o 73.315)) 0.01))
        (is (= [ex later] (rest rows)))))

    (testing "No spin-offs for the ticker: rows returned as is"
      (is (= rows (actions/undo-spin-off-price-adjustment rows nil)))
      (is (= rows (actions/undo-spin-off-price-adjustment rows []))))))
