(ns portfolio-web.validator
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [ring.util.codec :as codec]
            [cim_portfolio.util :as util]))

;; %0D%0A = newline
;; %2C = comma

(def header
  ["Date of trade submitted (YYYY-MM-DD)"
   "Action"
   "Amount Bought/Sold"
   "Ticker"
   "Price"])

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
     :starting-cash starting-cash}))

;; ─── Input validation ──────────────────────────────────────────────────────────
;; Every trade row a user submits is checked BEFORE it reaches the model: bad
;; rows used to surface as raw 500s (NPE on unknown tickers, joda "Invalid
;; format" on pasted header rows, etc.). Validation failures throw ex-info
;; whose message is shown to the user by the controller.

(defn- valid-date? [s]
  (try (util/parse-date s) true
       (catch Exception _ false)))

(defn- parseable-number? [s]
  (try (Double/parseDouble s) true
       (catch Exception _ false)))

(defn- split-row
  "Splits a raw CSV line into trimmed cells, dropping a trailing empty price
   cell (manual rows often end with a dangling comma: \"...,GOOG,\")."
  [line]
  (let [cells (mapv str/trim (str/split line #","))]
    (if (and (= (count cells) 5) (str/blank? (peek cells)))
      (subvec cells 0 4)
      cells)))

(defn- header-cell? [s]
  (str/starts-with? (str/lower-case s) "date"))

(defn- header-line?
  "True for a column-header row rather than a trade: its first cell is not a
   date but starts with \"date\" (e.g. \"date\" or \"Date of trade submitted
   (YYYY-MM-DD)\"). Lets users paste or upload a CSV with its header intact."
  [line]
  (let [[first-cell] (split-row line)]
    (and first-cell
         (not (valid-date? first-cell))
         (header-cell? first-cell))))

(defn- row-error
  "Error message for one parsed trade row, or nil when the row is valid."
  [line-no [date action amount ticker price :as row]]
  (let [prefix (str "Line " line-no " (\"" (str/join "," row) "\"): ")]
    (cond
      (< (count row) 4)
      (str prefix "expected date,action,amount,ticker[,price] — got only " (count row) " value(s).")

      (not (valid-date? date))
      (str prefix "\"" date "\" is not a valid date (expected YYYY-MM-DD)"
           (if (header-cell? date)
             " — this looks like a header row; only the first line may be a header."
             "."))

      (not (contains? #{"buy" "sell"} (str/lower-case action)))
      (str prefix "action must be \"buy\" or \"sell\", got \"" action "\".")

      (not (and (parseable-number? amount) (pos? (Double/parseDouble amount))))
      (str prefix "amount must be a positive number, got \"" amount "\".")

      (str/blank? ticker)
      (str prefix "ticker is missing.")

      (and price (not (parseable-number? price)))
      (str prefix "price must be a number, got \"" price "\"."))))

(defn- parse-trade-lines
  "Turns raw trade text (manual textarea or uploaded CSV) into the trades
   table [header row1 row2 ...] expected by the model. Skips blank lines and
   a leading column-header line; throws ex-info listing the invalid lines
   (numbered as in the input) if any row fails validation."
  [text]
  (let [numbered (map-indexed (fn [i line] [(inc i) line]) (str/split-lines (or text "")))
        numbered (remove (fn [[_ line]] (str/blank? line)) numbered)
        numbered (if (and (seq numbered) (header-line? (second (first numbered))))
                   (rest numbered)
                   numbered)
        rows (mapv (fn [[n line]] [n (split-row line)]) numbered)
        errors (keep (fn [[n row]] (row-error n row)) rows)]
    (when (empty? rows)
      (throw (ex-info "No trades provided — enter at least one line like: 2024-10-15,buy,100,NVDA" {})))
    (when (seq errors)
      (throw (ex-info (str/join "\n" (take 5 errors)) {:errors (vec errors)})))
    (into [header] (map second) rows)))

(defn- parse-starting-cash
  "Parses the starting-cash form field (the controller has already stripped $
   and commas). Throws ex-info with a user-facing message when non-numeric.
   Replaces the previous read-string call, which crashed downstream on
   non-numeric input (and evaluated arbitrary reader forms)."
  [s]
  (let [s (str/trim (or s ""))]
    (when-not (parseable-number? s)
      (throw (ex-info (str "Starting cash must be a number, got \"" s "\".") {})))
    (Double/parseDouble s)))

;; The below functions are used when the form data is encoded in multipart/form-data
;; With any function below, the final data should be in the following format:
;; {:trades [["Date of trade submitted (YYYY-MM-DD)" "Action" "Amount Bought/Sold" "Ticker"] 
;;           ["2024-10-15" "buy" "100" "NVDA"] ["2024-11-25" "buy" "50" "GOOG"]
;;           ["2024-12-22" "sell" "30" "TSLA"] ["2025-01-08" "sell" "30" "NVDA"]], 
;;  :starting-cash 50000000,
;;  :show-capm-metrics true,
;;  :show-stock-performances true}

;; For Manual Input
(defn parse-manual-input [trades-map] ;; Returns a map
  ;; trades-map looks like
  ;; {:trades "2024-10-15,buy,100,NVDA,130\r\n2024-11-25,buy,50,GOOG,\r\n..."
  ;;  :starting-cash "50000000"
  ;;  :show-capm-metrics "true",
  ;;  :show-stock-performances "true"}
  {:trades (parse-trade-lines (:trades trades-map))
   :starting-cash (parse-starting-cash (:starting-cash trades-map))
   :show-capm-metrics (= (get trades-map :show-capm-metrics) "true") ;; Convert checkbox value to boolean
   :show-stock-performances (= (get trades-map :show-stock-performances) "true")})

;; For File Input — identical to manual input: parse-trade-lines recognizes and
;; drops a leading header line itself, so (unlike before) a headerless CSV no
;; longer silently loses its first trade.
(defn parse-file-input [trades-map] ;; Returns a map
  (parse-manual-input trades-map))
