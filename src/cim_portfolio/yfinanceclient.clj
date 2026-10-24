;;; # Clojure client for Yahoo Finance price data
;;; ### Fetches prices via clj-yfinance and converts currencies with ECB rates (ecbjure)
(ns cim_portfolio.yfinanceclient
  (:require [clj-yfinance.core :as yf]
            [clojure-finance.ecbjure.fx :as fx])
  (:import (java.time Instant LocalDate ZoneOffset)))

;; ECB rates are fetched on first use, not at namespace load (no network I/O during AOT compile),
;; and reloaded once a day so a long-running server doesn't keep converting at stale rates.
;; A failed first fetch is not cached (unlike a delay, which would rethrow it until restart);
;; a failed reload keeps the rates already loaded.
(def ^:private converter (atom nil)) ;; {:fx converter :loaded-at epoch-ms}
(def ^:private converter-max-age-ms (* 24 60 60 1000))

(defn- fx-converter []
  (let [fresh (fn [{:keys [fx loaded-at]}]
                (when (and fx (< (- (System/currentTimeMillis) loaded-at) converter-max-age-ms))
                  fx))]
    (or (fresh @converter)
        (locking converter
          (or (fresh @converter)
              (let [old (:fx @converter)]
                (try
                  (:fx (reset! converter {:fx (fx/make-converter) :loaded-at (System/currentTimeMillis)}))
                  (catch Exception e
                    (if old
                      (do (println "ECB rate reload failed, keeping the rates loaded earlier:" (.getMessage e))
                          (:fx (reset! converter {:fx old :loaded-at (System/currentTimeMillis)})))
                      (throw e))))))))))

(defn convert-currency
  "Converts a price in `ticker`'s trading currency to `target-currency` (default USD).
   Accepts the price as a number or a string (trade files carry strings)."
  ([ticker ticker-price] (convert-currency ticker ticker-price "USD"))
  ([ticker ticker-price target-currency]
   (let [stock-currency (:currency (yf/fetch-info ticker))]
     (fx/convert (fx-converter) (Double/parseDouble (str ticker-price)) stock-currency target-currency))))

(defn- bar-date
  "UTC calendar date (yyyy-MM-dd) of a clj-yfinance bar."
  [bar]
  (str (.toLocalDate (.atZone (Instant/ofEpochSecond (:timestamp bar)) ZoneOffset/UTC))))

;; Yahoo quirks the raw bars carry: all-nil OHLC bars for days Yahoo has not filled in
;; (converting nil to USD would NPE), and an occasional repeat of the last datapoint.
;; Keep only priced bars, and where consecutive bars share a date keep the last one.
(defn- clean-price-bars [yf-response]
  (->> yf-response
       (filter #(and (:open %) (:close %)))
       (partition-by bar-date)
       (map last)))

(defn get-ticker-price-all
  "Daily [date open close] rows for `ticker` from `date` (yyyy-MM-dd) until today, with
   prices in USD. Prices are adjusted for dividends and splits (Yahoo's total-return series)."
  [ticker date]
  (let [yf-response (yf/fetch-historical ticker
                                         :start (.toEpochSecond (.atStartOfDay (LocalDate/parse date) ZoneOffset/UTC))
                                         :interval "1d"
                                         :auto-adjust true)
        stock-currency (:currency (yf/fetch-info ticker))
        fx (fx-converter)]
    (mapv #(vector (bar-date %)
                   (fx/convert fx (:open %) stock-currency "USD")
                   (fx/convert fx (:close %) stock-currency "USD"))
          (clean-price-bars yf-response))))

;; Fetches live data
(comment
  (get-ticker-price-all "0700.HK" "2025-01-25")
  (convert-currency "6758.T" 3000))
