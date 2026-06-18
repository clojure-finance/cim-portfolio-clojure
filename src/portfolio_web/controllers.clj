(ns portfolio-web.controllers
  (:require [ring.util.response :as res]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.codec :as codec]
            [portfolio-web.views :as views]
            [portfolio-web.validator :as validator]
            [portfolio-web.model :as model]
            [news-llm.core :as news]
            [clojure.string :as str]))

;; ─── Portfolio handlers ────────────────────────────────────────────────────────

(defn home-handler [request]
  (res/content-type (res/response (views/home-page)) "text/html"))

(defn analysis-handler [request]
  (res/content-type
   (res/response
    (let [params (:params request)
          parsed-trades
          (if (= (get params "trades") "")
            (validator/parse-file-input
             {:trades (slurp (:tempfile (get params "trades-file")))
              :starting-cash (str/replace (get params "starting-cash") #"[$,]" "")
              :show-capm-metrics (get params "show-capm-metrics" "false")
              :show-stock-performances (get params "show-stock-performances" "false")})
            (validator/parse-manual-input
             {:trades (get params "trades")
              :starting-cash (str/replace (get params "starting-cash") #"[$,]" "")
              :show-capm-metrics (get params "show-capm-metrics" "false")
              :show-stock-performances (get params "show-stock-performances" "false")}))]
      (-> parsed-trades
          (model/process-trades)
          (views/portfolio-page))))
   "text/html"))

;; ─── News Analysis handlers ────────────────────────────────────────────────────

(defn news-page-handler [request]
  (res/content-type
   (res/response (views/news-page nil))
   "text/html"))

(defn analyze-news-handler [request]
  (res/content-type
   (res/response
    (let [params (:params request)
          newsdata-key (get params "newsdata-api-key")
          llm-key      (get params "llm-api-key")
          provider     (get params "llm-provider" "deepseek")
          llm-url      (if (= provider "deepseek") news/deepseek-url news/openrouter-url)
          query        (let [q (str/trim (get params "query" ""))]
                         (when-not (str/blank? q) q))
          country      (get params "country" "us")
          language     (get params "language" "en")
          max-articles (try (Integer/parseInt (get params "max-articles" "3"))
                            (catch Exception _ 3))
          model        (get params "model" (first news/deepseek-models))
          delay-ms     (try (Long/parseLong (get params "delay" "1000"))
                            (catch Exception _ 1000))]
      (cond
        (str/blank? newsdata-key)
        (views/news-page "Newsdata.io API key is required.")

        (str/blank? llm-key)
        (views/news-page "LLM API key is required.")

        :else
        (let [articles (news/fetch-news newsdata-key country language max-articles query)]
          (if (empty? articles)
            (views/news-results-page [] "No articles found for your query. Try a different search term.")
            (let [results (mapv (fn [article]
                                  (news/analyze-article
                                   llm-key article model nil delay-ms 2 nil 0.7 llm-url))
                                articles)]
              (views/news-results-page results nil)))))))
   "text/html"))

;; ─── Router ───────────────────────────────────────────────────────────────────

(defn base-app [request]
  (let [method (:request-method request)
        uri    (:uri request)]
    (case [method uri]
      [:get  "/"]                (home-handler request)
      [:post "/analyze-portfolio"] (analysis-handler request)
      [:get  "/news"]            (news-page-handler request)
      [:post "/analyze-news"]    (analyze-news-handler request)
      {:status 404 :body "Not Found"})))

(def app
  (-> base-app
      (resource/wrap-resource "public")
      (wrap-multipart-params)
      (wrap-params)))
