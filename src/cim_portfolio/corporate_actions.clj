;;; # Corporate-Action Adjustment of Trade Data
;;;
;;; Yahoo Finance back-adjusts its entire quote history for stock splits: after
;;; a 5:1 split, all pre-split prices are reported divided by 5. Trade files,
;;; however, record the share counts (and optionally the prices) that were
;;; actual at the time of the trade. Without adjustment, a pre-split trade is
;;; valued with post-split prices on a pre-split share count, understating the
;;; position 5x — or, when the trade file carries the actually-paid price,
;;; producing a large phantom loss against the adjusted market series.
;;;
;;; This namespace normalizes the parsed trades table so that all share counts
;;; and user-supplied prices are in TODAY'S (post-split) units, consistent with
;;; the split-adjusted prices fetched from Yahoo.
(ns cim_portfolio.corporate-actions
  (:require [cim_portfolio.util :as util]
            [clj-yfinance.core :as yf]
            [clojure.string :as str])
  (:import (java.time Instant LocalDate ZoneOffset)))

(defn- trade-day-end
  "Epoch seconds of the last second of the trade date (UTC). `date-str` accepts
   any format understood by util/parse-date. Split timestamps mark the market
   open of the effective date, so comparing against the END of the trade day
   excludes splits effective on the trade date itself — trades executed that
   day are already in post-split units."
  [date-str]
  (dec (.toEpochSecond (.atStartOfDay (.plusDays (LocalDate/parse (util/parse-date date-str)) 1) ZoneOffset/UTC))))

(defn fetch-splits
  "Fetch split events for every distinct ticker in the trades table (header row
   + data rows). Returns {ticker splits-map} with the splits-map as returned by
   clj-yfinance's fetch-dividends-splits (empty map when the ticker has no
   splits or the fetch fails)."
  [trades]
  (into {}
        (map (fn [ticker]
               [ticker (:splits (yf/fetch-dividends-splits ticker :period "max"))]))
        (distinct (keep #(when (<= 4 (count %)) (nth % 3)) (rest trades))))) ;; Skip malformed rows, like adjust-trade does

;;; ## Spin-offs that Yahoo books as splits
;;;
;;; Yahoo sometimes records a spin-off weeks later as a "split" of the parent
;;; whose ratio is the pre/post-spin price ratio, and divides all pre-spin
;;; prices by it. Treating that as a real split multiplies pre-spin share
;;; counts, which keeps the spun-off value inside the parent (tracking the
;;; parent's price, not the new company's) and double-counts it when the
;;; received shares are also recorded as a price-0 buy — the documented way to
;;; record a spin-off. For the known cases below the fake split is dropped and
;;; the pre-spin prices restored, so the parent shows its real ex-date drop and
;;; the price-0 buy offsets it.

(def spin-offs-booked-as-splits
  "{ticker #{ex-date}} of spin-offs that Yahoo reports as a split of the parent."
  {"CTVA" #{"2026-10-01"}}) ;; Corteva spun off VYLR, 1 share per CTVA share

(defn- split-date
  "UTC calendar date (yyyy-MM-dd) of a clj-yfinance split event."
  [split]
  (str (.toLocalDate (.atZone (Instant/ofEpochSecond (long (:date split))) ZoneOffset/UTC))))

(defn separate-spin-offs
  "Split {ticker splits-map} (see fetch-splits) into real splits and spin-offs
   booked as splits. Returns {:splits {ticker splits-map}, :spin-offs {ticker
   [{:date \"yyyy-MM-dd\" :ratio r}]}}, where r is Yahoo's price divisor for
   the pre-spin history. A known spin-off only appears under :spin-offs while
   Yahoo actually reports it as a split."
  ([splits-by-ticker] (separate-spin-offs splits-by-ticker spin-offs-booked-as-splits))
  ([splits-by-ticker known]
   (reduce-kv
    (fn [acc ticker splits]
      (let [ex-dates (get known (str/upper-case ticker) #{})
            spin-off? (fn [[_ split]] (contains? ex-dates (split-date split)))
            fake (filter spin-off? splits)]
        (cond-> (assoc-in acc [:splits ticker] (into {} (remove spin-off?) splits))
          (seq fake) (assoc-in [:spin-offs ticker]
                               (mapv (fn [[_ split]]
                                       {:date (split-date split)
                                        :ratio (/ (double (:numerator split)) (:denominator split))})
                                     fake)))))
    {:splits {} :spin-offs {}}
    splits-by-ticker)))

(defn undo-spin-off-price-adjustment
  "Restore the actual pre-spin prices in [date open close] rows (as returned by
   the yfinanceclient fetchers) by multiplying every row dated before a
   spin-off's ex-date by Yahoo's ratio. `spin-offs` is one ticker's vector from
   separate-spin-offs; nil or empty leaves the rows unchanged."
  [price-rows spin-offs]
  (if (empty? spin-offs)
    price-rows
    (mapv (fn [[d open close :as row]]
            (let [factor (transduce (comp (filter #(neg? (compare d (:date %)))) (map :ratio))
                                    * 1.0 spin-offs)]
              (if (== factor 1.0) row [d (* open factor) (* close factor)])))
          price-rows)))

(defn adjust-trade
  "Adjust a single trade row [date action amount ticker & [price]] (strings, as
   produced by the input parsers) for splits that happened after the trade
   date: the share amount is multiplied by the cumulative split factor and the
   user-supplied price (when present) divided by it. Rows unaffected by splits
   are returned unchanged.

   Note: the factor is computed from the submitted trade date. In the rare case
   where an order is submitted on a non-trading day and a split takes effect
   before the next trading day, the trade is treated as pre-split although it
   executes post-split."
  [splits-by-ticker [date action amount ticker price :as row]]
  (if (< (count row) 4)
    row ;; Malformed row: leave for downstream handling
    (let [factor (yf/cumulative-split-factor (get splits-by-ticker ticker {})
                                             (trade-day-end date))]
      (if (== factor 1.0)
        row
        (cond-> (assoc (vec row) 2 (str (* factor (Double/parseDouble amount))))
          price (assoc 4 (str (/ (Double/parseDouble price) factor))))))))

(defn adjust-trades-for-splits
  "Adjust the parsed trades table (header row + data rows of
   [date action amount ticker & [price]], all strings) so that share amounts
   and user-supplied prices are in post-split units, consistent with Yahoo's
   split-adjusted quote history. The table shape is preserved.

   The 1-arity fetches split events per ticker from Yahoo; the 2-arity accepts
   a pre-fetched {ticker splits-map} (see fetch-splits) and is pure."
  ([trades] (adjust-trades-for-splits trades (fetch-splits trades)))
  ([trades splits-by-ticker]
   (into [(first trades)]
         (map #(adjust-trade splits-by-ticker %))
         (rest trades))))
