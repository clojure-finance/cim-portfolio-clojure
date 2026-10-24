;;; # Utility Functions for Portfolio Processing and Analysis

(ns cim_portfolio.util
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter DateTimeParseException ResolverStyle)
           (java.time.temporal ChronoUnit)))

;; FOR DATE PROCESSING

;; Accepted input formats for trade dates; month and day may have one or two digits
(def ^:private input-date-formats
  (mapv #(.withResolverStyle (DateTimeFormatter/ofPattern %) ResolverStyle/STRICT) ;; STRICT rejects 2024-02-30 instead of clamping it
        ["uuuu-M-d" "uuuu/M/d" "uuuuMMdd"]))

(defn parse-date
  "Normalizes a date string in any of the accepted input formats (2024-10-15,
   2024/10/15, 20241015) to yyyy-MM-dd. Throws on anything else, including
   impossible dates such as 2024-02-30."
  [date]
  (or (some #(try (str (LocalDate/parse date %))
                  (catch DateTimeParseException _ nil))
            input-date-formats)
      (throw (IllegalArgumentException. (str "Invalid date: \"" date "\"")))))

(defn number-of-days-between
  "Calendar days from start-date to end-date (both yyyy-MM-dd)."
  [start-date end-date]
  (.between ChronoUnit/DAYS (LocalDate/parse start-date) (LocalDate/parse end-date)))

;; HELPER FUNCTIONS FOR ORDERING A MAP BY THE DATE

(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn jt-parse-date [s]
  (LocalDate/parse s formatter))

(defn jt-compare-dates [d1 d2]
  (.compareTo (jt-parse-date d1) (jt-parse-date d2)))

(defn sort-map-by-date [m]
  (into (sorted-map-by jt-compare-dates) m))

;; STATISTICS

(defn mean [a] (/ (reduce + a) (count a)))

(defn square [n]
  (* n n))

(defn squares [avg prices] (map #(square (- % avg)) prices))

(defn std-dev
  "Sample standard deviation. Returns 0.0 when coll has fewer than two
   elements (the sample std-dev is undefined there; 0.0 keeps downstream
   arithmetic total-safe instead of dividing by zero)."
  [coll]
  (if (< (count coll) 2)
    0.0
    (let [avg (mean coll)
          squares (squares avg coll)
          total (count coll)]
      (Math/sqrt (/ (reduce + squares) (- total 1))))))
