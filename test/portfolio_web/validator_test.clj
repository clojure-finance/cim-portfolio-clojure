(ns portfolio-web.validator-test
  (:require [clojure.test :refer [deftest is testing]]
            [portfolio-web.validator :as validator]))

(defn- parse [text]
  (validator/parse-manual-input {:trades text :starting-cash "50000000"}))

(defn- trade-rows [text]
  (rest (:trades (parse text)))) ;; Drop the synthetic header row the parser prepends

(defn- error-message [text]
  (try (parse text)
       nil
       (catch clojure.lang.ExceptionInfo e (.getMessage e))))

(deftest valid-input
  (testing "rows are split, trimmed, and prefixed with the canonical header"
    (let [{:keys [trades starting-cash show-capm-metrics show-stock-performances]}
          (validator/parse-manual-input {:trades " 2024-10-15 , buy , 100 , NVDA , 130 \r\n2024-11-25,buy,50,GOOG,"
                                         :starting-cash "50000000"
                                         :show-capm-metrics "true"})]
      (is (= validator/header (first trades)))
      (is (= [["2024-10-15" "buy" "100" "NVDA" "130"]
              ["2024-11-25" "buy" "50" "GOOG"]] ;; trailing empty price cell is dropped
             (rest trades)))
      (is (= 5.0E7 starting-cash))
      (is (true? show-capm-metrics))
      (is (false? show-stock-performances))))
  (testing "blank lines are skipped"
    (is (= 2 (count (trade-rows "2024-10-15,buy,100,NVDA\n\n   \n2024-11-25,sell,50,NVDA")))))
  (testing "action is case-insensitive and all supported date formats pass"
    (is (= 3 (count (trade-rows "2024-10-15,BUY,100,NVDA\n2024/10/16,Sell,50,NVDA\n20241017,buy,25,NVDA"))))))

(deftest header-handling
  (testing "a leading header line is recognized and dropped (the July-12 production crash)"
    (is (= [["2024-10-15" "buy" "100" "NVDA"]]
           (trade-rows "date,action,amount,ticker\n2024-10-15,buy,100,NVDA")))
    (is (= [["2024-10-15" "buy" "100" "NVDA"]]
           (trade-rows "Date of trade submitted (YYYY-MM-DD),Action,Amount Bought/Sold,Ticker,Price\n2024-10-15,buy,100,NVDA"))))
  (testing "a header line after the first data row is an error, with a hint"
    (is (re-find #"Line 2 .*looks like a header row"
                 (error-message "2024-10-15,buy,100,NVDA\ndate,action,amount,ticker"))))
  (testing "file input behaves identically: header dropped when present, first data row kept when absent"
    (let [file-parse #(rest (:trades (validator/parse-file-input {:trades % :starting-cash "1000"})))]
      (is (= [["2024-10-15" "buy" "100" "NVDA"]]
             (file-parse "Date of trade submitted (YYYY-MM-DD),Action,Amount Bought/Sold,Ticker,Price\r\n2024-10-15,buy,100,NVDA")))
      (is (= [["2024-10-15" "buy" "100" "NVDA"]
              ["2024-11-25" "buy" "50" "GOOG"]] ;; headerless files used to lose this first row
             (file-parse "2024-10-15,buy,100,NVDA\r\n2024-11-25,buy,50,GOOG"))))))

(deftest row-errors
  (testing "each invalid field produces a line-numbered, user-facing message"
    (is (re-find #"Line 1 .*action must be \"buy\" or \"sell\", got \"hold\""
                 (error-message "2024-10-15,hold,100,NVDA")))
    (is (re-find #"Line 1 .*\"notadate\" is not a valid date"
                 (error-message "notadate,buy,100,NVDA")))
    (is (re-find #"Line 1 .*amount must be a positive number, got \"-5\""
                 (error-message "2024-10-15,buy,-5,NVDA")))
    (is (re-find #"Line 1 .*amount must be a positive number, got \"0\""
                 (error-message "2024-10-15,buy,0,NVDA")))
    (is (re-find #"Line 1 .*ticker is missing"
                 (error-message "2024-10-15,buy,100, ,130")))
    (is (re-find #"Line 1 .*price must be a number, got \"abc\""
                 (error-message "2024-10-15,buy,100,NVDA,abc")))
    (is (re-find #"Line 1 .*expected date,action,amount,ticker"
                 (error-message "2024-10-15,buy,100"))))
  (testing "a date typo containing \"date\" does not get the header-row hint"
    (is (not (re-find #"header" (error-message "notadate,buy,100,NVDA")))))
  (testing "line numbers count blank and header lines as in the user's input"
    (is (re-find #"Line 4 " (error-message "date,action,amount,ticker\n\n2024-10-15,buy,100,NVDA\n2024-10-16,hold,5,NVDA"))))
  (testing "at most five errors are reported"
    (let [msg (error-message (apply str (repeat 7 "2024-10-15,hold,1,NVDA\n")))]
      (is (= 5 (count (re-seq #"Line \d+" msg)))))))

(deftest empty-and-cash-errors
  (testing "no usable rows"
    (is (re-find #"No trades provided" (error-message "")))
    (is (re-find #"No trades provided" (error-message "  \n \n"))))
  (testing "starting cash must be numeric — read-string is gone, reader forms are rejected"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Starting cash must be a positive number"
                          (validator/parse-manual-input {:trades "2024-10-15,buy,100,NVDA" :starting-cash "abc"})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Starting cash must be a positive number"
                          (validator/parse-manual-input {:trades "2024-10-15,buy,100,NVDA" :starting-cash "(+ 1 2)"}))))
  (testing "starting cash must be positive — zero divides the annualized return and one-dollar chart, negative feeds NaN logarithms"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Starting cash must be a positive number"
                          (validator/parse-manual-input {:trades "2024-10-15,buy,100,NVDA" :starting-cash "0"})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Starting cash must be a positive number"
                          (validator/parse-manual-input {:trades "2024-10-15,buy,100,NVDA" :starting-cash "-50000"})))))
