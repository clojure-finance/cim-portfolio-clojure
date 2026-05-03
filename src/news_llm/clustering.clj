(ns news-llm.clustering
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def data-dir "data")
(def clusters-file (str data-dir "/stories.jsonl"))

(defn ensure-data-dir []
  (let [d (io/file data-dir)]
    (.mkdirs d)))

(defn load-clusters []
  (ensure-data-dir)
  (if (.exists (io/file clusters-file))
    (with-open [r (io/reader clusters-file)]
      (doall (map json/decode (line-seq r) false)))
    []))

(defn save-cluster! [cluster]
  (ensure-data-dir)
  (spit clusters-file (str (json/encode cluster) "\n") :append true)
  cluster)

(defn cos-sim [v1 v2]
  (let [dot (reduce + (map * v1 v2))
        n1 (Math/sqrt (reduce + (map #(* % %) v1)))
        n2 (Math/sqrt (reduce + (map #(* % %) v2)))]
    (if (or (zero? n1) (zero? n2)) 0.0 (/ dot (* n1 n2)))))

(defn find-best-cluster [clusters vec threshold]
  (when (and vec (seq clusters))
    (let [sims (map (fn [c] {:cluster c :sim (cos-sim (:centroid c) vec)}) clusters)
          best (apply max-key :sim sims)]
      (when (>= (:sim best) threshold)
        (:cluster best)))))

(defn update-cluster-centroid [cluster vec]
  (let [n (inc (or (:count cluster) 0))
        old-centroid (or (:centroid cluster) vec)
        new-centroid (map #(/ (+ (* %1 %2) %3) n) (map (fn [x y] (* x y)) (repeat 1) old-centroid) (repeat 1) vec)]
    ;; simple incremental centroid (this is illustrative; we use averaging below)
    (assoc cluster :centroid (vec new-centroid) :count n :updated_at (System/currentTimeMillis))))

(defn assign-or-create [vec]
  "Load clusters, try to assign vec to best cluster above threshold, otherwise create a new cluster. Returns cluster id." 
  (ensure-data-dir)
  (let [clusters (load-clusters)
        threshold 0.70
        best (find-best-cluster clusters vec threshold)]
    (if best
      (do
        ;; update cluster on disk by appending a small update record (for simplicity)
        (save-cluster! (assoc best :count (inc (or (:count best) 0)) :centroid (:centroid best) :updated_at (System/currentTimeMillis)))
        (:id best))
      (let [id (str "story-" (java.util.UUID/randomUUID))
            new { :id id :centroid vec :count 1 :created_at (System/currentTimeMillis) }]
        (save-cluster! new)
        id))))
