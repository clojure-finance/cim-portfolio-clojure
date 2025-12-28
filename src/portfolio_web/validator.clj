(ns portfolio-web.validator
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

;; In the future, need to add validation for correct input format and logging

;; %0D%0A = newline
;; %2C = comma

(def header
  ["Date of trade submitted (YYYY-MM-DD)"
   "Action"
   "Amount Bought/Sold"
   "Ticker"])

;; DEPRECATED, used when the form data is in encoded in application/x-www-form-urlencoded
(defn parse-trades [raw-body] ;; Returns a map
  (let [decoded (codec/url-decode raw-body) ;; Converts all of the url-encoded characters into normal characters
        ;; Currently it looks like 
        ;; trades=2024-10-15,buy,100,NVDA 2024-11-25,buy,50,GOOG 2024-12-22,sell,30,TSLA 2025-01-08,sell,30,NVDA&starting-cash=50000000
        params (into {} ;; Creates an empty map
                     (map #(str/split % #"=" 2) (str/split decoded #"&"))) ;; Split the string by "&", afterwards split it again into key and value by "=" (hashtag string denotes regex exp)
        trades (-> (params "trades")
                   (str/split-lines)) ;; Turns it into ["2024-10-15,buy,100,NVDA" "2024-11-25,buy,50,GOOG" "2024-12-22,sell,30,TSLA" "2025-01-08,sell,30,NVDA"] 
        trades (into [header]
                     (map #(str/split % #",") trades))
        ;; Turns it into [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
        ;; ["2024-10-15" "buy" "100" "NVDA"] ["2024-11-25" "buy" "50" "GOOG"] ["2024-12-22" "sell" "30" "TSLA"] ["2025-01-08" "sell" "30" "NVDA"]] 
        starting-cash (read-string (params "starting-cash"))] ;; Convert string to integer with read-string
    {:trades trades
     :starting-cash starting-cash}
    ))

;; The below functions are used when the form data is in encoded in multipart/form-data
;; With any function below, the final data should be in the following format:
;; {:trades [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
;;           ["2024-10-15" "buy" "100" "NVDA"] ["2024-11-25" "buy" "50" "GOOG"]
;;           ["2024-12-22" "sell" "30" "TSLA"] ["2025-01-08" "sell" "30" "NVDA"]], 
;;  :starting-cash 50000000}

;; For Manual Input
(defn parse-manual-input [trades-map] ;; Returns a map
  (let [;; Currently trades-map should look like 
        ;; {:trades "2024-10-15,buy,100,NVDA\r\n2024-11-25,buy,50,GOOG\r\n2024-12-22,sell,30,TSLA\r\n2025-01-08,sell,30,NVDA" 
        ;;  :starting-cash "50000000"} 
        
        trades (-> (:trades trades-map)
                   (str/split-lines)) ;; Turns it into ["2024-10-15,buy,100,NVDA" "2024-11-25,buy,50,GOOG" "2024-12-22,sell,30,TSLA" "2025-01-08,sell,30,NVDA"] 
        
        ;; The following turns it into 
        ;; [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
        ;;  ["2024-10-15" "buy" "100" "NVDA"]
        ;;  ["2024-11-25" "buy" "50" "GOOG"]
        ;;  ["2024-12-22" "sell" "30" "TSLA"]
        ;;  ["2025-01-08" "sell" "30" "NVDA"]]
        trades (into [header]
                     (map #(str/split % #",") trades))
        
        starting-cash (read-string (:starting-cash trades-map))] ;; Convert string to integer with read-string
    {:trades trades
     :starting-cash starting-cash}))

;; ;; For File Input
(defn parse-file-input [trades-map] ;; Returns a map
  (let [
        
        ;; Currently trades-map should look like 
        ;; {:trades "Date of trade submitted (YYYY-MM-DD),Action,Amount Bought/Sold,Ticker\r\n2024-10-15,buy,100,NVDA\r\n2024-11-25,buy,50,GOOG\r\n2024-12-22,sell,30,TSLA\r\n2025-01-08,sell,30,NVDA"
        ;;  :starting-cash "50000000"}

        ;; First, we get rid of the string "Date of trade submitted (YYYY-MM-DD),Action,Amount Bought/Sold,Ticker\r\n", using regex (hashtag string denotes regex exp)
        trades (-> (str/split (:trades trades-map) #"\n" 2) ;; There should be 1 newline before the data we want
                   (last)
                   (str/split-lines)) ;; Turns it into ["2024-10-15,buy,100,NVDA" "2024-11-25,buy,50,GOOG" "2024-12-22,sell,30,TSLA" "2025-01-08,sell,30,NVDA"] 

        ;; The following turns it into 
        ;; [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
        ;;  ["2024-10-15" "buy" "100" "NVDA"]
        ;;  ["2024-11-25" "buy" "50" "GOOG"]
        ;;  ["2024-12-22" "sell" "30" "TSLA"]
        ;;  ["2025-01-08" "sell" "30" "NVDA"]]
        trades (into [header]
                     (map #(str/split % #",") trades))

        starting-cash (read-string (:starting-cash trades-map))] ;; Convert string to integer with read-string
    {:trades trades
     :starting-cash starting-cash}))
