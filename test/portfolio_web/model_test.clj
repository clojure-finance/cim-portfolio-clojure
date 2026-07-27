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
