(ns cim-portfolio.news-clerk
  (:require [nextjournal.clerk :as clerk]
            [cim-portfolio.news.core :as news]
            [clojure.string :as str]))

;; # 📰 AI Market News Analysis
;; Enter your API keys and stock symbol below, then click "Analyze" to fetch and analyze news.

(defonce app-state 
  (atom {:newsdata-key "" 
         :llm-key "" 
         :stock-symbol "NVDA" 
         :max-articles 3 
         :analyze-trigger 0}))

(clerk/with-viewer
  '(fn [state _]
     [:div {:style {:padding "20px" :border "1px solid #eee" :border-radius "8px" :background-color "#f9f9f9"}}
      [:h3 {:style {:margin-top "0px"}} "Configuration"]
      
      [:div {:style {:margin-bottom "15px"}}
       [:label {:style {:display "block" :font-weight "bold" :margin-bottom "5px"}} "Newsdata.io API Key"]
       [:input {:type "password"
                :style {:width "100%" :padding "8px" :border "1px solid #ccc" :border-radius "4px"}
                :placeholder "Enter Newsdata.io API Key..."
                :value (:newsdata-key @state)
                :on-change #(swap! state assoc :newsdata-key (.. % -target -value))}]]
         
      [:div {:style {:margin-bottom "15px"}}
       [:label {:style {:display "block" :font-weight "bold" :margin-bottom "5px"}} "LLM API Key (DeepSeek/OpenAI)"]
       [:input {:type "password"
                :style {:width "100%" :padding "8px" :border "1px solid #ccc" :border-radius "4px"}
                :placeholder "Enter LLM API Key (sk-...)"
                :value (:llm-key @state)
                :on-change #(swap! state assoc :llm-key (.. % -target -value))}]]

      [:div {:style {:margin-bottom "20px"}}
       [:label {:style {:display "block" :font-weight "bold" :margin-bottom "5px"}} "Stock Symbol"]
       [:input {:type "text"
                :style {:width "100%" :padding "8px" :border "1px solid #ccc" :border-radius "4px"}
                :placeholder "e.g. NVDA, AAPL"
                :value (:stock-symbol @state)
                :on-change #(swap! state assoc :stock-symbol (.. % -target -value))}]]
         
      [:button {:style {:background-color "#3b82f6" :color "white" :font-weight "bold" 
                        :padding "10px 20px" :border "none" :border-radius "4px" :cursor "pointer"}
                :on-click #(swap! state update :analyze-trigger inc)}
       "Analyze News"]])
  app-state)

(def results
  (let [state @app-state
        news-key (:newsdata-key state)
        llm-key (:llm-key state)
        sym (:stock-symbol state)
        max-arts (:max-articles state)
        trigger (:analyze-trigger state)]
    (if (> trigger 0)
      (if (and (not-empty news-key) (not-empty llm-key))
        (let [articles (news/fetch-news news-key "us" "en" max-arts sym)]
          (if (seq articles)
            (doall (map #(news/analyze-article llm-key % 
                                               (:model news/default-config) 
                                               nil 
                                               (:request-delay news/default-config) 
                                               (:max-retries news/default-config) 
                                               nil 
                                               (:similarity-threshold news/default-config))
                        articles))
            [{:title "No articles found" :success false :summary "Try a different query."}]))
        [{:title "Error: Missing API Keys" :success false :summary "Please provide both Newsdata.io and LLM API keys in the input boxes above."}])
      nil)))

;; ## Analysis Results
(when (seq results)
  (clerk/table
   (map #(select-keys % [:title :sentiment :category :tldr :success]) results)))

(when (seq results)
  (clerk/html
   [:div
    (for [r results]
      (when (:success r)
        [:div {:style {:border "1px solid #ddd" :padding "15px" :margin-bottom "20px" :border-radius "5px"}}
         [:h3 (:title r)]
         [:p [:strong "Sentiment: "] (:sentiment r)]
         [:p [:strong "Summary: "] (:summary r)]
         (when (:link r) [:p [:a {:href (:link r) :target "_blank"} "Read Original"]])]))]))
