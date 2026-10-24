(ns cim-portfolio.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [cim_portfolio.util :as util]))

(deftest parse-date-test
  (testing "every accepted input format normalizes to yyyy-MM-dd"
    (is (= "2024-10-05" (util/parse-date "2024-10-05")))
    (is (= "2024-10-05" (util/parse-date "2024/10/05")))
    (is (= "2024-10-05" (util/parse-date "20241005")))
    (is (= "2024-10-05" (util/parse-date "2024-10-5"))))
  (testing "anything else throws, including impossible calendar dates"
    (is (thrown? IllegalArgumentException (util/parse-date "date")))
    (is (thrown? IllegalArgumentException (util/parse-date "2024-02-30")))
    (is (thrown? IllegalArgumentException (util/parse-date "15/10/2024")))))

(deftest number-of-days-between-test
  (is (= 197 (util/number-of-days-between "2023-10-13" "2024-04-27")))
  (is (= 0 (util/number-of-days-between "2024-01-01" "2024-01-01")))
  (is (= -1 (util/number-of-days-between "2024-01-02" "2024-01-01"))))
