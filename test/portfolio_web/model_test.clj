(ns portfolio-web.model-test
  (:require [clojure.test :refer [deftest is testing]]
            [portfolio-web.model :as model]))

(deftest window-contains-negative-value-test
  (let [values [["2026-01-05" 100.0] ["2026-01-06" -50.0] ["2026-01-07" 80.0] ["2026-01-08" 90.0]]]
    (testing "negative value inside the window"
      (is (true? (model/window-contains-negative-value? values "2026-01-06" "2026-01-08"))))
    (testing "the value the window's first return is measured from counts, even though it precedes the window"
      (is (true? (model/window-contains-negative-value? values "2026-01-07" "2026-01-08"))))
    (testing "window that ends before the book went short is clean"
      (is (false? (model/window-contains-negative-value? values "2026-01-05" "2026-01-05"))))
    (testing "negative values before the measurement basis of the window are ignored"
      (is (false? (model/window-contains-negative-value? [["2026-01-05" -10.0] ["2026-01-06" 70.0] ["2026-01-07" 80.0] ["2026-01-08" 90.0]]
                                                         "2026-01-08" "2026-01-08"))))
    (testing "zero values are fine — an empty portfolio is a normal state, not an undefined one"
      (is (false? (model/window-contains-negative-value? [["2026-01-05" 100.0] ["2026-01-06" 0.0] ["2026-01-07" 80.0]]
                                                         "2026-01-05" "2026-01-07"))))))

(deftest trailing-one-year-returns-test
  (let [today (java.time.LocalDate/parse "2026-10-24")
        inception (java.time.LocalDate/parse "2026-01-02")
        log-returns {"2026-10-20" (Math/log 1.1) "2026-01-05" (Math/log 1.2)}
        values [["2026-01-02" 100.0] ["2026-01-05" 120.0] ["2026-10-20" 132.0]]
        returns (model/trailing-one-year-returns log-returns values inception today)]
    (testing "each window sums the log returns dated inside it"
      (is (< (Math/abs (- 0.32 (:one-year-cumulative-return-from-today returns))) 1e-12))
      (is (< (Math/abs (- 0.2 (:one-year-cumulative-return-from-one-week-ago returns))) 1e-12)))
    (testing "all six windows are present"
      (is (= 6 (count returns))))
    (testing "a window touching a negative value is nil (rendered n/a)"
      (is (nil? (:one-year-cumulative-return-from-today
                 (model/trailing-one-year-returns log-returns (conj values ["2026-10-21" -5.0]) inception today)))))))
