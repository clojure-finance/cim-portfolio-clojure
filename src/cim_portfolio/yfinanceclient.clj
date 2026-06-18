(ns cim_portfolio.yfinanceclient
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import [java.time LocalDate ZoneId Instant]
           [java.time.format DateTimeFormatter]))

(def user-agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

(defn- to-epoch [date-str]
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")
        date (LocalDate/parse date-str formatter)
        zone (ZoneId/of "America/New_York")]
    (-> date (.atStartOfDay zone) (.toEpochSecond))))

(defn- epoch-to-date [seconds]
  (let [instant (Instant/ofEpochSecond seconds)
        zone (ZoneId/of "America/New_York")
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (-> instant (.atZone zone) (.format formatter))))

(defn- fetch-chart [ticker start-epoch end-epoch]
  (let [url (str "https://query2.finance.yahoo.com/v8/finance/chart/" ticker)
        params {:period1 start-epoch
                :period2 end-epoch
                :interval "1d"
                :events "history"}
        resp (http/get url {:query-params params 
                            :headers {"User-Agent" user-agent} 
                            :as :json
                            :throw-exceptions false})]
    (if (= 200 (:status resp))
      (get-in resp [:body :chart :result 0])
      (throw (Exception. (str "Failed to fetch data for " ticker " status: " (:status resp)))))))

(defn- get-exchange-rate [from-curr]
  (if (or (nil? from-curr) (= "USD" from-curr))
    1.0
    (try
      (let [pair (str from-curr "USD=X")
            end (quot (System/currentTimeMillis) 1000)
            start (- end 864000) ;; Look back 10 days to be safe
            data (fetch-chart pair start end)
            price (get-in data [:meta :regularMarketPrice])]
         (double (or price 1.0)))
      (catch Exception e
        (println "Error fetching exchange rate for" from-curr ":" (.getMessage e))
        1.0))))

;; Main functions
(defn get-ticker-price-with-end [ticker start-date end-date]
  (try
    (let [start (to-epoch start-date)
          end (to-epoch end-date)
          data (fetch-chart ticker start end)
          timestamps (get-in data [:timestamp])
          quotes (get-in data [:indicators :quote 0])
          adj-closes (get-in data [:indicators :adjclose 0 :adjclose])
          opens (:open quotes)
          closes (:close quotes)
          
          ;; Detect currency and rate
          meta (:meta data)
          currency (:currency meta)
          rate (get-exchange-rate currency)]
      
      (if (or (empty? timestamps) (empty? opens))
        []
        (vec
         (keep (fn [idx]
                 (let [ts (nth timestamps idx)
                       o (nth opens idx nil)
                       c (nth closes idx nil)
                       adj (nth adj-closes idx nil)]
                   (when (and o c adj)
                     (let [date-str (epoch-to-date ts)
                           ;; Auto-adjust wrapper logic:
                           ;; We want Adjusted Open and Adjusted Close in USD.
                           ;; factor = adj-close / close
                           factor (if (zero? c) 1.0 (/ adj c))
                           final-open (* o factor rate)
                           final-close (* adj rate)] ;; adj is already adjusted
                       [date-str (double final-open) (double final-close)]))))
               (range (count timestamps))))))
    (catch Exception e
      (println "Error fetching" ticker ":" (.getMessage e))
      [])))
      
(defn get-ticker-price-all [ticker date]
  (let [now-str (epoch-to-date (quot (System/currentTimeMillis) 1000))]
    (get-ticker-price-with-end ticker date now-str)))

(defn convert-currency
  "Returns the price unchanged (assumes USD input). Full FX conversion not available in pure-Clojure build."
  ([_ticker ticker-price] ticker-price)
  ([_ticker ticker-price _target-currency] ticker-price))


