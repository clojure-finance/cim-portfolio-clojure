(ns cim-portfolio.news-clerk
  (:require [nextjournal.clerk :as clerk]
            [cim-portfolio.news.core :as news]
            [clojure.string :as str]))

;; # 📰 AI Market News Analysis
;; Enter a stock symbol below to fetch and analyze the latest news using AI.

(def stock-symbol "NVDA")

(def max-articles 3)

(def results
  (let [api-keys (try (news/validate-api-keys)
                      (catch Exception _ nil))]
    (if api-keys
      (let [articles (news/fetch-news (:newsdata api-keys) "us" "en" max-articles stock-symbol)]
        (if (seq articles)
          (doall (map #(news/analyze-article (:llm api-keys) % 
                                             (:model news/default-config) 
                                             nil 
                                             (:request-delay news/default-config) 
                                             (:max-retries news/default-config) 
                                             nil 
                                             (:similarity-threshold news/default-config))
                      articles))
          [{:title "No articles found" :success false :summary "Try a different query."}]))
      [{:title "Error: API Keys Missing" :summary "Please set NEWSDATA_API_KEY and DEEPSEEK_API_KEY env vars." :success false}])))

;; ## Analysis Results

(clerk/table
 (map #(select-keys % [:title :sentiment :category :tldr :success]) results))

(doseq [r results]
  (when (:success r)
    (clerk/html
     [:div {:style {:border "1px solid #ddd" :padding "15px" :margin-bottom "20px" :border-radius "5px"}}
      [:h3 (:title r)]
      [:p [:strong "Sentiment: "] (:sentiment r)]
      [:p [:strong "Summary: "] (:summary r)]
      (when (:link r) [:p [:a {:href (:link r) :target "_blank"} "Read Original"]])])))
