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

;; Keep the model consistent with the selected provider: a DeepSeek model name
;; sent to OpenRouter (or vice versa) is rejected with HTTP 400.
(defn- normalize-model [provider model]
  (let [deepseek? (some #{model} news/deepseek-models)]
    (cond
      (and (= provider "deepseek") (not deepseek?)) (first news/deepseek-models)
      (and (= provider "openrouter") deepseek?)     (first news/free-models)
      :else model)))

(defn- llm-failure-message [provider {:keys [error-status error-message]}]
  (let [provider-name (if (= provider "deepseek") "DeepSeek" "OpenRouter")
        hint (case error-status
               402 (str "Your " provider-name " account has insufficient balance — top up at "
                        (if (= provider "deepseek") "platform.deepseek.com" "openrouter.ai")
                        " or switch provider.")
               401 (str provider-name " rejected the API key. Double-check the LLM API key.")
               429 (str provider-name " is rate-limiting requests. Wait a minute and retry, or pick another model.")
               400 (str provider-name " rejected the request — often a model that doesn't exist on this provider.")
               -1  (str "Could not reach " provider-name " — network error from the server.")
               nil)]
    (str "LLM analysis failed for all articles"
         (when (and error-status (pos? error-status)) (str " (HTTP " error-status ")"))
         (when (seq error-message) (str ": " error-message))
         (if hint (str " — " hint) ". Please check your LLM API key."))))

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
          model        (normalize-model provider (get params "model" (first news/deepseek-models)))
          delay-ms     (try (Long/parseLong (get params "delay" "1000"))
                            (catch Exception _ 1000))]
      (cond
        (str/blank? newsdata-key)
        (views/news-page "Newsdata.io API key is required.")

        (str/blank? llm-key)
        (views/news-page "LLM API key is required.")

        :else
        (try
          (let [articles (news/fetch-news newsdata-key country language max-articles query)]
            (if (empty? articles)
              (views/news-results-page [] "No articles found for your query. Try a different search term.")
              (let [results (->> articles
                                 (mapv (fn [article]
                                         (try
                                           (news/analyze-article
                                            llm-key article model nil delay-ms 2 nil 0.7 llm-url)
                                           (catch Exception _
                                             nil))))
                                 (filterv some?))]
                (cond
                  (empty? results)
                  (views/news-results-page [] "LLM analysis failed for all articles. Please check your LLM API key.")

                  (not-any? :success results)
                  (views/news-results-page
                   [] (llm-failure-message provider (or (some #(when (:error-status %) %) results)
                                                        (first results))))

                  :else
                  (views/news-results-page results nil)))))
          (catch Exception e
            (views/news-page (str "Error: " (.getMessage e))))))))
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
