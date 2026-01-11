(ns cim-portfolio.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [portfolio-web.controllers :refer [app]])
  (:gen-class))

(defn -main [& args] 
  (run-jetty portfolio-web.controllers/app 
             {:port (Integer/parseInt (or (first args) (System/getenv "PORT") "3000")) :join? false}))