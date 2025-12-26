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

;; OLD

;; (defn old-parse-trades [raw-body] ;; Returns a vector
;;   (let [decoded (codec/url-decode raw-body) ;; Converts all of the url-encoded characters into normal characters
;;         trades-str (subs decoded (count "trades=")) ;; Removes the prefix "trades="
;;         lines (str/split-lines trades-str) ;; Turns it into ["2024-10-15,buy,100,NVDA" "2024-11-25,buy,50,GOOG" "2024-12-22,sell,30,TSLA" "2025-01-08,sell,30,NVDA"]
;;         trades (into [header]
;;                      (map #(str/split % #",") lines))] ;; Hashtag strings are regex literals in CLojure
;;     ;; Turns it into [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
;;     ;; ["2024-10-15" "buy" "100" "NVDA"] ["2024-11-25" "buy" "50" "GOOG"] ["2024-12-22" "sell" "30" "TSLA"] ["2025-01-08" "sell" "30" "NVDA"]]
;;     trades ;; Vector of Trades
;;     ))


