(ns portfolio-web.validator
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]))

(def header
  ["Date of trade submitted (YYYY-MM-DD)"
   "Action"
   "Amount Bought/Sold"
   "Ticker"
   "Price"])

;; DEPRECATED: urlencoded form
(defn parse-trades [raw-body]
  (let [decoded (codec/url-decode raw-body)
        params (into {} (map #(str/split % #"=" 2) (str/split decoded #"&")))
        trades (into [header] (map #(str/split % #",") (str/split-lines (params "trades"))))
        starting-cash (read-string (params "starting-cash"))]
    {:trades trades :starting-cash starting-cash}))

;; Manual textarea input
(defn parse-manual-input [trades-map]
  (let [trades (into [header]
                     (map #(str/split % #",")
                          (str/split-lines (:trades trades-map))))
        starting-cash (read-string (:starting-cash trades-map))
        show-capm-metrics (= (get trades-map :show-capm-metrics) "true")
        show-stock-performances (= (get trades-map :show-stock-performances) "true")]
    {:trades trades
     :starting-cash starting-cash
     :show-capm-metrics show-capm-metrics
     :show-stock-performances show-stock-performances}))

;; CSV file upload input
(defn parse-file-input [trades-map]
  (let [trades (into [header]
                     (map #(str/split % #",")
                          (str/split-lines
                           (last (str/split (:trades trades-map) #"\n" 2)))))
        starting-cash (read-string (:starting-cash trades-map))
        show-capm-metrics (= (get trades-map :show-capm-metrics) "true")
        show-stock-performances (= (get trades-map :show-stock-performances) "true")]
    {:trades trades
     :starting-cash starting-cash
     :show-capm-metrics show-capm-metrics
     :show-stock-performances show-stock-performances}))
