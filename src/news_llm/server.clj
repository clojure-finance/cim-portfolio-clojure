(ns news-llm.server
  (:require [news-llm.core :as core]
            [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [hiccup.core :refer [html]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]
            [clojure.string :as str]))

(defn portfolio-app-url []
  (or (System/getenv "PORTFOLIO_APP_URL") "http://localhost:3000"))

(defn layout [title & content]
  (html5
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:style "body { font-family: sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; }
             .article { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; border-radius: 5px; }
             .success { border-left: 5px solid green; }
             .failed { border-left: 5px solid red; }
             label { display: block; margin-top: 10px; font-weight: bold; }
             input[type=text], input[type=number], select { width: 100%; padding: 8px; margin-top: 5px; box-sizing: border-box; }
             input[type=submit] { margin-top: 20px; padding: 10px 20px; background-color: #007bff; color: white; border: none; cursor: pointer; }
             input[type=submit]:hover { background-color: #0056b3; }
             .loading { display: none; color: #666; font-style: italic; margin-top: 10px; }"]]
   [:body
    [:div {:style "margin-bottom: 16px;"}
     [:a {:href (portfolio-app-url)
        :style "display:inline-block;padding:8px 14px;border-radius:6px;background:#111827;color:#fff;text-decoration:none;"}
      "<- Back to Portfolio Home"]]
    [:h1 title]
    content
    [:script "document.querySelector('form').addEventListener('submit', function() { document.querySelector('.loading').style.display = 'block'; });"]]))

(defn home-page []
  (layout "News Analysis Tool"
          [:div 
           (form/form-to [:post "/analyze"]
                         [:label "Newsdata API Key (Required)"]
                         (form/text-field {:required true} "newsdata-api-key")

                         [:label "DeepSeek / OpenRouter API Key (Required)"]
                         (form/text-field {:required true} "llm-api-key")

                         [:label "Search Query (or Symbol)"]
                         (form/text-field "query")

                         [:label "Country (e.g., us, gb, cn)"]
                         (form/text-field {:value "us"} "country")
                         
                         [:label "Language (e.g., en, zh)"]
                         (form/text-field {:value "en"} "language")

                         [:label "Max Articles"]
                         (form/text-field {:type "number" :value "2" :min "1" :max "10"} "max-articles")

                         [:label "Model"]
                         (form/drop-down "model" core/free-models (first core/free-models))
                         
                         [:input {:type "submit" :value "Analyze News"}]
                         [:div.loading "Analyzing news... This may take a minute due to rate limiting."])]))

(defn analyze-handler [params]
  (try
    (let [query (get params "query")
          country (get params "country" "us")
          language (get params "language" "en")
          newsdata-api-key (get params "newsdata-api-key")
          llm-api-key (get params "llm-api-key")
          max-articles (parse-long (get params "max-articles" "1"))
          model (get params "model" (:model core/default-config))
          api-keys {:newsdata newsdata-api-key :openrouter llm-api-key}
          search-query (if (str/blank? query) nil query)]
      
      (if (and (str/blank? search-query) (str/blank? country))
        (layout "Error" [:p.error "Please provide a query or country."])
        
        (let [news (core/fetch-news (:newsdata api-keys) country language max-articles search-query)]
          (if (empty? news)
            (layout "No Results" [:p "No articles found."])
            (let [results (doall
                            (map-indexed
                             (fn [idx article]
                               (core/analyze-article (:openrouter api-keys) article model nil (:request-delay core/default-config) (:max-retries core/default-config) (:embedding-url core/default-config) (:similarity-threshold core/default-config)))
                             news))]
              (layout "Analysis Results"
                      [:div
                       (for [r results]
                         [:div.article {:class (if (:success r) "success" "failed")}
                          [:h3 (:title r)]
                          [:p [:strong "Status: "] (if (:success r) "Success" "Failed")]
                          (when (:link r) [:p [:a {:href (:link r) :target "_blank"} "Read Original"]])
                          [:p [:strong "Sentiment: "] (:sentiment r)]
                          [:p [:strong "Category: "] (:category r)]
                          [:p [:strong "TL;DR: "] (:tldr r)]
                          [:p [:strong "Summary: "] (:summary r)]
                          [:p [:strong "Keywords: "] (str/join ", " (:keywords r))]])]
                      [:p [:a {:href "/"} "Back to Home"]]))))))
    (catch Exception e
      (layout "Error" [:p "An error occurred: " (.getMessage e)]))))

(defroutes app-routes
  (GET "/" [] (home-page))
  (POST "/analyze" {params :params} (analyze-handler params))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main [& args]
  (let [port (or (some-> (System/getenv "PORT") parse-long) 3100)]
    (println (str "Starting server on port " port "..."))
    (http/run-server app {:port port})))
