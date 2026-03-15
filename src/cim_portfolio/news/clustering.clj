(ns cim-portfolio.news.clustering
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
      (doall (map #(json/decode % true) (line-seq r))))
    []))

(defn save-cluster! [cluster]
  (ensure-data-dir)
  (spit clusters-file (str (json/encode cluster) "\n") :append true)
  cluster)

(defn cos-sim [v1 v2]
  (let [dot (reduce + (map * v1 v2))
        m1 (Math/sqrt (reduce + (map * v1 v1)))
        m2 (Math/sqrt (reduce + (map * v2 v2)))]
    (if (or (zero? m1) (zero? m2)) 0.0 (/ dot (* m1 m2)))))

(defn assign-or-create [embedding]
  (let [threshold 0.85
        clusters (load-clusters)
        best-match (->> clusters
                        (map (fn [c] {:cluster c :score (cos-sim embedding (:centroid c))}))
                        (filter #(>= (:score %) threshold))
                        (sort-by :score >)
                        first)]
    (if best-match
      (:id (:cluster best-match))
      (let [new-id (str "story-" (java.util.UUID/randomUUID))]
        (save-cluster! {:id new-id :centroid embedding :articles 1 :created_at (System/currentTimeMillis)})
        new-id))))