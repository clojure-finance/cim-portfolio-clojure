(ns cim-portfolio.core
  (:require [cim-portfolio.news.server :as server])
  (:gen-class))

(defn -main [& args]
  (apply server/-main args))