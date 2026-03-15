(ns cim-portfolio.core-test
  (:require [clojure.test :refer :all]
            [cim-portfolio.core :refer :all]))

(deftest a-test
  (testing "core namespace is loaded"
    (is (some? #'cim-portfolio.core/-main))))
