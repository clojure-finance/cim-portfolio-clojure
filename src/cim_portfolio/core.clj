(ns cim-portfolio.core
  (:require [cim-portfolio.news.server :as server]
            [news-llm.server :as news-server])
  (:gen-class))

(defn -main [& args]
  ;; Start the News LLM Server as a standalone/untouched tool on port 3100
  (future (news-server/-main))
  
  ;; Start the original portfolio tool
  (apply server/-main args))