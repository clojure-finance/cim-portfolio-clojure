(ns cim-portfolio.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [portfolio-web.controllers :refer [app]])
  (:gen-class))

(defn -main [& args]
  (let [port (or (when (seq args) (Integer/parseInt (first args)))
                 (when-let [p (System/getenv "PORT")] (Integer/parseInt p))
                 3000)]
    (println (str "Starting CIM Portfolio + News Analysis on port " port "..."))
    (run-jetty app {:port port :join? false})))
