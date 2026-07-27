;;; # Clojure client for Yahoo Finance price data
;;; ### Fetches prices via clj-yfinance and converts currencies with ECB rates (ecbjure)
(ns cim_portfolio.yfinanceclient
  (:require [clj-yfinance.core :as yf]
            [clojure-finance.ecbjure.fx :as fx])
  (:import (java.time Instant LocalDate ZoneOffset)))

;; Fetch latest rates from ECB on first use (a delay, so no network I/O at namespace load/AOT compile)
(def c (delay (fx/make-converter)))

(defn convert-currency
  ;; Accepts the price as a number or a string (trade files carry strings)
  ([ticker ticker-price target-currency] (let [stock-currency (:currency (yf/fetch-info ticker))] (fx/convert @c (Double/parseDouble (str ticker-price)) stock-currency target-currency)))
  ([ticker ticker-price] (let [stock-currency (:currency (yf/fetch-info ticker))] (fx/convert @c (Double/parseDouble (str ticker-price)) stock-currency "USD"))) ;; No target currency defaults to USD
  )

;; Yahoo quirks the raw bars carry: all-nil OHLC bars for days Yahoo has not filled in
;; (converting nil to USD would NPE), and an occasional repeat of the last datapoint.
;; Keep only priced bars, and where consecutive bars share a date keep the last one.
(defn- clean-price-bars [yf-response]
  (->> yf-response
       (filter #(and (:open %) (:close %)))
       (partition-by #(.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp %)) ZoneOffset/UTC)))
       (map last)))

(defn get-ticker-price-all [ticker date]
  ;; On this branch orders execute the trading day AFTER submission, so fetching
  ;; starts at date + 1 day (the web-application branch instead executes on the
  ;; first trading day >= the order date — a deliberate design difference)
  (let [yf-response (yf/fetch-historical ticker :start (.toEpochSecond (.atStartOfDay (.plusDays (LocalDate/parse date) 1) ZoneOffset/UTC)) ;; Convert date string, e.g. 2026-01-31, to Epoch Seconds
                                         :interval "1d"
                                         :auto-adjust true) ;; Prices are adjusted for dividends and splits (Yahoo's total-return series)
        stock-currency (:currency (yf/fetch-info ticker))]
    (mapv #(vector (str (.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp %)) ZoneOffset/UTC))) ;; Convert Seconds since Epoch to a Date String, e.g. 2026-01-31
                   (fx/convert @c (:open %) stock-currency "USD")
                   (fx/convert @c (:close %) stock-currency "USD"))
          (clean-price-bars yf-response))))

(defn get-ticker-price-with-end [ticker start-date end-date]
  ;; Note that one day is not added to the start date here
  (let [yf-response (yf/fetch-historical ticker :start (.toEpochSecond (.atStartOfDay (LocalDate/parse start-date) ZoneOffset/UTC)) ;; Convert date string, e.g. 2026-01-31, to Epoch Seconds
                                         :end (.toEpochSecond (.atStartOfDay (LocalDate/parse end-date) ZoneOffset/UTC))
                                         :interval "1d"
                                         :auto-adjust true) ;; Prices are adjusted for dividends and splits (Yahoo's total-return series)
        stock-currency (:currency (yf/fetch-info ticker))]
    (mapv #(vector (str (.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp %)) ZoneOffset/UTC))) ;; Convert Seconds since Epoch to a Date String, e.g. 2026-01-31
                   (fx/convert @c (:open %) stock-currency "USD")
                   (fx/convert @c (:close %) stock-currency "USD"))
          (clean-price-bars yf-response))))

;; Test if function is working + price is converted to USD

(get-ticker-price-all "0700.HK" "2025-01-25")

(get-ticker-price-with-end "0700.HK" "2025-01-25" (.toString (java.time.LocalDate/now)))
