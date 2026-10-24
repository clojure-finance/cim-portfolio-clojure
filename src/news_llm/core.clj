;;; # News analysis for the /news page
;;; Fetches articles from Newsdata.io, extracts their full text, and has an LLM
;;; (DeepSeek or OpenRouter, both OpenAI-compatible) return a 15-field JSON analysis.
(ns news-llm.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import [org.jsoup Jsoup]))

;; API endpoints
(def newsdata-url "https://newsdata.io/api/1/news")
(def openrouter-url "https://openrouter.ai/api/v1/chat/completions")
(def deepseek-url "https://api.deepseek.com/chat/completions")

;; Models offered on the /news form; the first of each list is the default
;; deepseek-chat and deepseek-reasoner were retired on 2026-07-24
(def deepseek-models ["deepseek-flash" "deepseek-v4-pro"])

;; OpenRouter's free models change often. "openrouter/free" routes to whichever
;; free model is available, so it keeps working when individual ones are retired.
(def free-models
  ["openrouter/free"
   "google/gemma-4-31b-it:free"
   "nvidia/nemotron-3-super-120b-a12b:free"])

;; Server log helpers (stdout ends up in the service journal)
(defn- log-info [msg] (println (str "ℹ " msg)))
(defn- log-success [msg] (println (str "✓ " msg)))
(defn- log-error [msg] (println (str "✗ " msg)))
(defn- log-warn [msg] (println (str "⚠ " msg)))

;; Fetch news articles from Newsdata.io: by search query, or the country's top headlines
(defn fetch-news [api-key country language max-articles query]
  (try
    (if query
      (log-info (format "Fetching news: query=%s, language=%s, max=%d" query language max-articles))
      (log-info (format "Fetching news: country=%s, language=%s, max=%d" country language max-articles)))
    (let [query-params (if query
                         {:apikey api-key
                          :q query
                          :language language}
                         {:apikey api-key
                          :country country
                          :language language})
          resp (http/get newsdata-url
                         {:query-params query-params
                          :as :json
                          :throw-exceptions true})
          body    (:body resp)
          status  (:status body)
          results (:results body)]
      (if (= status "error")
        (throw (ex-info (str "Newsdata.io error: " (:message body)
                             " (code: " (:code body) ")") {}))
        (if (empty? results)
          (do
            (log-warn "No articles found")
            [])
          (do
            (log-success (format "Fetched %d articles" (count results)))
            (take max-articles results)))))
    (catch Exception e
      (log-error (format "Failed to fetch news: %s" (.getMessage e)))
      (throw (ex-info (str "Newsdata.io API error: " (.getMessage e)) {} e)))))

;; Fetch an article's full text: Jina Reader (LLM-friendly) first, then the page itself
;; parsed with Jsoup. Returns nil when both fail; the prompt then uses the feed's description.
(defn fetch-full-content [url]
  (try
    (log-info (format "Attempting to fetch content from: %s" url))

    ;; 1. Jina Reader
    (log-info "Strategy 1: Jina Reader")
    (let [jina-url (str "https://r.jina.ai/" url)
          resp (try
                 (http/get jina-url
                           {:headers {"User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                      "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"}
                            :timeout 30000
                            :throw-exceptions false})
                 (catch Exception _
                   (log-warn "Jina request failed")
                   {:status 500}))]

      (if (and (= 200 (:status resp))
               (not (str/blank? (:body resp)))
               (> (count (:body resp)) 150))
        (do
          (log-success (format "Jina success! Extracted %d chars" (count (:body resp))))
          (:body resp))

        (do
          (log-warn (format "Jina failed (Status: %s). Switching to Strategy 2..."
                            (:status resp)))

          ;; 2. Fallback: fetch the page with browser-like headers and strip it with Jsoup
          (log-info "Strategy 2: Simulating Browser Request (clj-http)")
          (try
            (let [resp (http/get url
                                 {:headers {"User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                                            "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                                            "Accept-Language" "en-US,en;q=0.9"
                                            "Upgrade-Insecure-Requests" "1"}
                                  :timeout 30000
                                  :throw-exceptions false
                                  :follow-redirects true})
                  html (:body resp)]

              (if (and (= 200 (:status resp)) (not (str/blank? html)))
                (let [doc (Jsoup/parse html)
                      ;; Aggressive clutter removal
                      _ (.remove (.select doc "script, style, nav, header, footer, iframe, .ad, .advertisement, .social-share, .menu, .cookie-banner, .sidebar, #comments, form, svg, noscript, .hidden"))
                      text (-> (.body doc) .text)]

                  (if (> (count text) 150)
                    (do
                      (log-success (format "Strategy 2 success! Extracted %d chars" (count text)))
                      text)
                    (do
                      (log-warn "Strategy 2 content too short/empty.")
                      nil)))
                (do
                  (log-warn (format "Strategy 2 HTTP failed (Status: %s)" (:status resp)))
                  nil)))
            (catch Exception _
              (log-warn "Strategy 2 exception: Unable to fetch content")
              nil)))))
    (catch Exception e
      (log-warn (format "Global fetch error: %s" (.getMessage e)))
      nil)))

;; Build prompt for LLM
(defn build-prompt [article]
  (let [raw-content (or (:full-content article) (:description article) (:content article) "No content available")
        ;; Truncate content to ~4000 chars to be safer for free models
        content (if (> (count raw-content) 4000)
                  (str (subs raw-content 0 4000) "\n...[Content Truncated]...")
                  raw-content)
        ;; Keep printable ASCII only
        clean-content (str/replace content #"[^\x20-\x7E\n\r\t]" "")]
    (str "Analyze the following financial news article and return ONLY valid JSON.\n"
         "Required JSON fields (return ALL 15):\n"
         "{\"summary\": \"2-3 sentence detailed summary\", "
         "\"tldr\": \"one sentence\", "
         "\"sentiment\": \"Positive/Negative/Neutral\", "
         "\"bias\": \"left-leaning/right-leaning/centrist/neutral\", "
         "\"keywords\": [\"keyword1\", \"keyword2\"], "
         "\"entities\": [\"company or person name\"], "
         "\"category\": \"Finance/Technology/Economics/Politics/Healthcare/Energy/etc\", "
         "\"target_audience\": \"retail investors/institutional/general public\", "
         "\"market_impact\": 7, "
         "\"investment_stance\": \"Bullish/Bearish/Neutral\", "
         "\"time_sensitivity\": \"Breaking/Recent/Evergreen\", "
         "\"affected_sectors\": [\"Technology\", \"Finance\"], "
         "\"key_quote\": \"most impactful sentence from the article verbatim\", "
         "\"risk_level\": \"High/Medium/Low\", "
         "\"actionable_insight\": \"what investors should watch or consider\"}\n\n"
         "Title: " (:title article) "\n"
         "Content: " clean-content)))

;; Extract the JSON text from an OpenAI-compatible chat response
(defn extract-llm-content [resp]
  (let [body (:body resp)
        ;; Try multiple response structures
        content (or
                 ;; Standard OpenAI format: body.choices[0].message.content
                 (-> body :choices first :message :content)
                 ;; Alternative: body.choices[0].text
                 (-> body :choices first :text)
                 ;; Direct content in body
                 (:content body)
                 ;; If body is a string (some APIs return raw)
                 (when (string? body) body)
                 ;; Try error message for debugging
                 (-> body :error :message))]
    (when content
      (let [cleaned (-> content
                        (str/replace #"```json" "")
                        (str/replace #"```" "")
                        str/trim)]
        (or (re-find #"(?s)\{.*\}" cleaned) cleaned)))))

;; Pull the provider's human-readable error message out of a non-200 response.
;; clj-http leaves error bodies as raw strings (:as :json only coerces 2xx),
;; so handle both map and string bodies.
(defn api-error-message [body]
  (let [m (cond (map? body) body
                (string? body) (try (json/decode body true) (catch Exception _ nil))
                :else nil)]
    (when-let [msg (or (get-in m [:error :message]) (:message m))]
      (str msg))))

;; One chat-completion request; never throws (an exception becomes status -1)
(defn- call-llm-single [api-key prompt model llm-url]
  (try
    (let [body (cond-> {:model model
                        :messages [{:role "system"
                                    :content "You are a senior financial news analyst. Return ONLY valid JSON with these exact 15 fields: summary, tldr, sentiment, bias, keywords, entities, category, target_audience, market_impact (integer 1-10), investment_stance (Bullish/Bearish/Neutral), time_sensitivity (Breaking/Recent/Evergreen), affected_sectors (array of strings), key_quote (verbatim sentence), risk_level (High/Medium/Low), actionable_insight (string). No markdown, no text outside the JSON object."}
                                   {:role "user"
                                    :content prompt}]}
                 ;; DeepSeek models think by default, which is slow and not needed to fill in the fields
                 (= llm-url deepseek-url) (assoc :thinking {:type "disabled"}))
          resp (http/post llm-url
                          {:headers {"Authorization" (str "Bearer " api-key)
                                     "Content-Type" "application/json"}
                           :body (json/encode body)
                           :as :json
                           :timeout 60000
                           :throw-exceptions false})
          status (:status resp)
          text (extract-llm-content resp)]
      {:status status :text text :model model :body (:body resp)})
    (catch Exception e
      {:status -1 :error (.getMessage e) :model model})))

;; Call the LLM with up to `retries` retries. Returns the JSON text on success, otherwise
;; a JSON error stub carrying the HTTP status and the provider's message.
(defn call-llm [api-key prompt model llm-url retries]
  (loop [attempts retries]
    (let [{:keys [status text error body]} (call-llm-single api-key prompt model llm-url)]
      (cond
        ;; Success
        (and (= status 200) text (str/includes? text "{"))
        (do (log-success (format "LLM response received [%s]" model))
            text)

        ;; Rate limited: wait longer before retrying
        (and (= status 429) (pos? attempts))
        (do (log-warn (format "Rate limited, waiting 5s before retry... (%d left)" attempts))
            (Thread/sleep 5000)
            (recur (dec attempts)))

        ;; Other error, or an exception (status -1)
        (pos? attempts)
        (do (log-warn (format "LLM error (status %d), retrying... (%d left)" status attempts))
            (Thread/sleep 2000)
            (recur (dec attempts)))

        :else
        (let [msg (or error (api-error-message body))]
          (log-error (format "LLM call failed. Last status: %d%s"
                             status (if msg (str " — " msg) "")))
          (json/encode {:summary "API Error" :sentiment "Neutral" :keywords [] :category "Unknown"
                        :tldr "Unable to analyze" :bias "N/A" :entities [] :target_audience "N/A"
                        :error_status status :error_message msg}))))))

;; Analyze one article: wait `delay-ms` (spaces out requests to the provider), fetch its
;; full text, and parse the LLM's JSON into the map the results page renders.
(defn analyze-article [api-key article model llm-url delay-ms]
  (Thread/sleep (long delay-ms))
  (let [full-content (when (:link article) (fetch-full-content (:link article)))
        article-with-content (if full-content
                               (assoc article :full-content full-content)
                               article)
        raw (call-llm api-key (build-prompt article-with-content) model llm-url 2)]
    (try
      (let [parsed (json/decode raw true)
            valid-summary (and (:summary parsed)
                               (not (contains? #{"Invalid" "API Error" "Exception" "Empty" "Failed"} (:summary parsed))))]
        {:title (:title article)
         :link (:link article)
         :published (:pubDate article)
         :summary (or (:summary parsed) "No summary available")
         :tldr (or (:tldr parsed) "N/A")
         :sentiment (or (:sentiment parsed) "Neutral")
         :bias (or (:bias parsed) "Unknown")
         :keywords (or (:keywords parsed) [])
         :entities (or (:entities parsed) [])
         :category (or (:category parsed) "General")
         :target-audience (or (:target_audience parsed) "General")
         :market-impact (when-let [v (:market_impact parsed)] (try (int v) (catch Exception _ nil)))
         :investment-stance (or (:investment_stance parsed) "Neutral")
         :time-sensitivity (or (:time_sensitivity parsed) "Recent")
         :affected-sectors (or (:affected_sectors parsed) [])
         :key-quote (or (:key_quote parsed) "")
         :risk-level (or (:risk_level parsed) "Medium")
         :actionable-insight (or (:actionable_insight parsed) "")
         :error-status (:error_status parsed)
         :error-message (:error_message parsed)
         :success (boolean valid-summary)})
      (catch Exception _
        (log-error (format "JSON parse error for: %s" (:title article)))
        (log-warn (format "Raw response (first 200 chars): %.200s" raw))
        {:title (:title article)
         :link (:link article)
         :published (:pubDate article)
         :summary "Parse error"
         :tldr "Unable to parse LLM response"
         :sentiment "Neutral"
         :bias "Unknown"
         :keywords []
         :entities []
         :category "Unknown"
         :target-audience "N/A"
         :market-impact nil
         :investment-stance "Neutral"
         :time-sensitivity "Recent"
         :affected-sectors []
         :key-quote ""
         :risk-level "Medium"
         :actionable-insight ""
         :success false}))))
