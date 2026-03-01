(ns news-llm.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [news-llm.clustering]
            [news-llm.embeddings :as embed])
  (:import [org.jsoup Jsoup])
  (:gen-class))

;; API endpoints
(def newsdata-url "https://newsdata.io/api/1/news")
(def openrouter-url "https://openrouter.ai/api/v1/chat/completions")

;; Configuration constants
(def default-config
  {:country "us"
   :language "en"
   :max-articles 1
   :output-format "txt"
   :output-dir "/mnt/c/Users/53419/Desktop/RA Documents/news_llm_newsdata/dynamic report"
   :request-delay 1000
   :max-retries 2
   :model "meta-llama/llama-3.2-3b-instruct:free"
   :fallback-model "openai/gpt-4o-mini"
   :embedding-url "http://localhost:8000"
   :similarity-threshold 0.7
   :symbol nil
   :search-query nil})

;; Available free models for reference
(def free-models
  ["google/gemini-2.0-flash-exp:free"
   "meta-llama/llama-3.3-70b-instruct:free"
   "deepseek/deepseek-r1:free"
   "qwen/qwen-2.5-72b-instruct:free"])

;; ANSI color codes
(def colors
  {:reset "\u001b[0m"
   :red "\u001b[31m"
   :green "\u001b[32m"
   :yellow "\u001b[33m"
   :blue "\u001b[34m"
   :magenta "\u001b[35m"
   :cyan "\u001b[36m"})

(def embedding-index-file "data/embeddings.jsonl")

;; Helper to read environment variables
(defn env [k] (System/getenv k))

;; Colored output helpers
(defn colorize [color text]
  (str (colors color) text (colors :reset)))

(defn log-info [msg]
  (println (colorize :blue (str "ℹ " msg))))

(defn log-success [msg]
  (println (colorize :green (str "✓ " msg))))

(defn log-error [msg]
  (println (colorize :red (str "✗ " msg))))

(defn log-warn [msg]
  (println (colorize :yellow (str "⚠ " msg))))

;; Validate API keys
(defn validate-api-keys []
  (let [newsdata-key (env "NEWSDATA_API_KEY")
        openrouter-key (env "OPENROUTER_API_KEY")]
    (when-not newsdata-key
      (log-error "NEWSDATA_API_KEY environment variable not set")
      (System/exit 1))
    (when-not openrouter-key
      (log-error "OPENROUTER_API_KEY environment variable not set")
      (System/exit 1))
    {:newsdata newsdata-key :openrouter openrouter-key}))

;; Ensure the output directory exists
(defn ensure-dir [dir-path]
  (let [dir (io/file dir-path)]
    (.mkdirs dir)))

(defn load-embedding-index []
  (ensure-dir "data")
  (if (.exists (io/file embedding-index-file))
    (with-open [r (io/reader embedding-index-file)]
      (doall (map #(json/decode % true) (line-seq r))))
    []))

(defn save-embedding-record! [record]
  (ensure-dir "data")
  (spit embedding-index-file (str (json/encode record) "\n") :append true)
  record)

(defn infer-embedding [service-url text]
  (when (and service-url (not (str/blank? service-url)) (not (str/blank? text)))
    (embed/infer-vector service-url text)))

(defn find-similar [vec existing threshold top-k]
  (->> existing
       (filter :vector)
       (map (fn [row]
              (let [sim (news-llm.clustering/cos-sim vec (:vector row))]
                (assoc row :similarity sim))))
       (filter #(>= (:similarity %) threshold))
       (sort-by :similarity >)
       (take top-k)
       (map #(select-keys % [:id :title :link :published :similarity]))))

(defn format-similar [similar]
  (if (seq similar)
    (str/join "; " (map (fn [{:keys [title similarity]}]
                           (format "%s (%.2f)" (or title "N/A") similarity))
                         similar))
    "None"))

;; Generate timestamp string for unique filenames
(defn timestamp []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd-HHmmss")]
    (.format fmt (java.util.Date.))))

;; Progress tracker
(defn show-progress [current total]
  (let [percent (int (* 100 (/ current total)))
        bar-length 30
        filled (int (* bar-length (/ current total)))
        bar (str (apply str (repeat filled "█"))
                 (apply str (repeat (- bar-length filled) "░")))]
    (print (str "\r" (colorize :cyan (format "[%s] %d%% (%d/%d)" bar percent current total))))
    (flush)))

;; Fetch news articles from Newsdata.io
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
          results (-> resp :body :results)]
      (if (empty? results)
        (do
          (log-warn "No articles found")
          [])
        (do
          (log-success (format "Fetched %d articles" (count results)))
          (take max-articles results))))
    (catch Exception e
      (log-error (format "Failed to fetch news: %s" (.getMessage e)))
      [])))

;; Fetch full content from URL using Jina Reader (LLM-friendly)
(defn fetch-full-content [url]
  (try
    (log-info (format "Attempting to fetch content from: %s" url))
    
    ;; 1. Try Jina Reader (Updated Headers & Robustness)
    (log-info "Strategy 1: Jina Reader")
    (let [jina-url (str "https://r.jina.ai/" url)
          resp (try 
                 (http/get jina-url
                           {:headers {"User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                      "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"}
                            :timeout 30000
                            :throw-exceptions false
                            :insecure? true}) ;; Ignore SSL certificate issues
                 (catch Exception _ 
                   (log-warn (str "Jina request failed"))
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
          
          ;; 2. Fallback: clj-http -> Jsoup (Better HTTP client than Jsoup native)
          (log-info "Strategy 2: Simulating Browser Request (clj-http)")
          (try
            (let [resp (http/get url
                                {:headers {"User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                                           "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                                           "Accept-Language" "en-US,en;q=0.9"
                                           "Upgrade-Insecure-Requests" "1"}
                                 :timeout 30000
                                 :throw-exceptions false
                                 :insecure? true
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
              (log-warn (format "Strategy 2 exception: %s" "Unable to fetch content"))
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
        ;; Escape/Clean content to ensure valid JSON string construction (though json/encode handles most)
        clean-content (str/replace content #"[^\x20-\x7E\n\r\t]" "")] 
    (str "Analyze the following news article and return ONLY valid JSON:\n"
         "{\"summary\": \"brief summary\", \"tldr\": \"one sentence\", \"sentiment\": \"Positive/Negative/Neutral\", "
         "\"bias\": \"assessment\", \"keywords\": [\"topic1\", \"topic2\"], \"entities\": [\"name/org\"], "
         "\"category\": \"category\", \"target_audience\": \"audience\"}\n\n"
         "Title: " (:title article) "\n"
         "Content: " clean-content)))

;; Extract text content from various OpenRouter response formats
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

;; Call single LLM model (internal helper)
(defn call-llm-single [api-key prompt model]
  (try
    (let [body {:model model
                :messages [{:role "system"
                            :content "You are a concise news analyst. Return ONLY valid JSON with these exact fields: summary, tldr, sentiment, bias, keywords, entities, category, target_audience. No markdown."}
                           {:role "user"
                            :content prompt}]}
          resp (http/post openrouter-url
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

;; Call LLM with model fallback: try primary model first, switch to fallback on 429
(defn call-llm [api-key prompt model fallback-model retries]
  (loop [current-model model
         remaining-models (if fallback-model [fallback-model] [])
         attempts retries]
    (let [{:keys [status text error body]} (call-llm-single api-key prompt current-model)]
      (cond
        ;; Success
        (and (= status 200) text (str/includes? text "{"))
        (do (log-success (format "LLM response received [%s]" current-model))
            text)
        
        ;; Rate limited (429) - try fallback model immediately
        (and (= status 429) (seq remaining-models))
        (do (log-warn (format "Model %s rate limited (429), switching to fallback: %s" 
                              current-model (first remaining-models)))
            (recur (first remaining-models) (rest remaining-models) retries))
        
        ;; Rate limited but no fallback - retry with delay
        (and (= status 429) (> attempts 0))
        (do (log-warn (format "Rate limited, waiting 5s before retry... (%d left)" attempts))
            (Thread/sleep 5000)
            (recur current-model remaining-models (dec attempts)))
        
        ;; Other error with retries
        (and (> attempts 0) (not= status 200))
        (do (log-warn (format "LLM error (status %d), retrying... (%d left)" status attempts))
            (Thread/sleep 2000)
            (recur current-model remaining-models (dec attempts)))
        
        ;; Exception - try fallback
        error
        (do (log-error (format "LLM Exception: %s" error))
            (if (seq remaining-models)
              (do (log-warn (format "Trying fallback model: %s" (first remaining-models)))
                  (recur (first remaining-models) (rest remaining-models) retries))
              "{\"summary\":\"Exception\",\"sentiment\":\"Neutral\",\"keywords\":[],\"category\":\"Unknown\",\"tldr\":\"Error occurred\",\"bias\":\"N/A\",\"entities\":[],\"target_audience\":\"N/A\"}"))
        
        ;; Final failure
        :else
        (do (log-error (format "All models failed. Last status: %d" status))
            "{\"summary\":\"API Error\",\"sentiment\":\"Neutral\",\"keywords\":[],\"category\":\"Unknown\",\"tldr\":\"Unable to analyze\",\"bias\":\"N/A\",\"entities\":[],\"target_audience\":\"N/A\"}")))))

;; Local TF-IDF based embedding for text 
(defonce vocabulary (atom {}))
(defonce doc-count (atom 0))

;; Build vocabulary from text
(defn build-vocab [text]
  (let [words (str/split (str/lower-case text) #"\W+")
        filtered (filter #(> (count %) 2) words)]
    (doseq [w filtered]
      (swap! vocabulary update w (fn [c] (inc (or c 0)))))))

(defn get-tfidf-embedding [text]
  (try
    (let [words (-> text
                    (str/lower-case)
                    (str/split #"\W+")
                    (->> (filter #(> (count %) 2)))
                    (vec))
          word-freq (frequencies words) 
          embedding (into [] 
                          (take 100 
                            (concat 
                              (map (fn [w] 
                                     (let [tf (/ (get word-freq w 0) (count words))
                                           idf (Math/log (/ @doc-count (inc (get @vocabulary w 1))))]
                                       (* tf idf))) 
                                   (take 50 words))
                              (map #(Math/abs (double (hash %))) words))))]
      embedding)
    (catch Exception e
      (log-warn (format "TF-IDF embedding failed: %s" (.getMessage e)))
      (into [] (take 100 (repeatedly #(rand 0.5)))))))

;; Analyze one article with rate limiting
(defn analyze-article [api-key article model fallback-model delay max-retries embedding-url similar-threshold]
  (Thread/sleep (long delay))
  (let [full-content (when (:link article) (fetch-full-content (:link article)))
        article-with-content (if full-content
                               (assoc article :full-content full-content)
                               article)
        text (str (:title article-with-content) " " (or (:full-content article-with-content) (:description article-with-content) ""))
        ;; Persist raw article for later offline processing
        _ (do (ensure-dir "data")
              (spit "data/articles.jsonl" (str (json/encode {:title (:title article)
                                                              :content (or (:full-content article-with-content) (:description article-with-content) "")
                                                              :link (:link article)
                                                              :published (:pubDate article)}) "\n") :append true))
        ;; Build vocab for TF-IDF fallback
        _ (build-vocab text)
        doc2vec-vec (infer-embedding embedding-url text)
        _ (when-not doc2vec-vec (swap! doc-count inc))
        embedding (or doc2vec-vec (get-tfidf-embedding text))
        embedding-source (if doc2vec-vec "doc2vec" "tfidf-fallback") 
        existing-index (when embedding (load-embedding-index))
        similar (when embedding (find-similar embedding existing-index similar-threshold 5))
        story-id (when embedding (try (news-llm.clustering/assign-or-create embedding) (catch Exception _ nil)))
        record-id (or story-id (str "story-" (java.util.UUID/randomUUID)))
        _ (when embedding
            (save-embedding-record! {:id record-id
                                     :title (:title article)
                                     :link (:link article)
                                     :published (:pubDate article)
                                     :vector embedding
                                     :embedding_source embedding-source
                                     :created_at (System/currentTimeMillis)}))
        raw (call-llm api-key (build-prompt article-with-content) model fallback-model max-retries)]
    (try
      (let [parsed (json/decode raw true)
            ;; Validate essential fields
            valid-summary (and (:summary parsed) 
                               (not (contains? #{"Invalid" "API Error" "Exception" "Empty" "Failed"} (:summary parsed))))]            
        {:title (:title article)
         :link (:link article)
         :published (:pubDate article)
         ;; Core 8 fields from prompt
         :summary (or (:summary parsed) "No summary available")
         :tldr (or (:tldr parsed) "N/A")
         :sentiment (or (:sentiment parsed) "Neutral")
         :bias (or (:bias parsed) "Unknown")
         :keywords (or (:keywords parsed) [])
         :entities (or (:entities parsed) [])
         :category (or (:category parsed) "General")
         :target-audience (or (:target_audience parsed) "General")
         ;; System fields
         :story-id story-id
         :similar similar 
         :embedding-source embedding-source
         :success valid-summary})
      (catch Exception e
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
         :similar similar
         :embedding-source embedding-source
         :success false}))))

;; Output formatters
(defn format-txt [results]
  (apply str
         (for [result results]
           (let [{:keys [title link published summary tldr sentiment bias keywords entities category target-audience
                        similar embedding-source success]} result]
             (str "═══════════════════════════════════════════════════════════════════════════════\n"
                  "TITLE: " title "\n"
                  "LINK: " (or link "N/A") "\n"
                  "PUBLISHED: " (or published "N/A") "\n"
                  "STATUS: " (if success "✓ SUCCESS" "✗ FAILED") "\n\n"
                  "━━━ ANALYSIS ━━━\n"
                  "Category: " category "\n"
                  "Sentiment: " sentiment "\n"
                  "Bias: " (or bias "N/A") "\n"
                  "Target Audience: " (or target-audience "N/A") "\n"
                  "Keywords: " (str/join ", " keywords) "\n"
                  "Entities: " (str/join ", " (or entities [])) "\n\n"
                  "━━━ TL;DR ━━━\n"
                  (or tldr "N/A") "\n\n"
                  "━━━ SUMMARY ━━━\n"
                  summary "\n\n"
                  "━━━ SYSTEM INFO ━━━\n"
                  "Similar Articles: " (format-similar similar) "\n"
                  "Embedding Source: " (or embedding-source "unknown") "\n\n")))))

(defn format-json [results]
  (json/encode results {:pretty true}))

(defn format-csv [results]
  (let [header "Title,Link,Published,Category,Sentiment,Bias,Target Audience,Keywords,Entities,Similar,Embedding Source,TLDR,Summary,Success\n"
        rows (for [{:keys [title link published summary tldr sentiment bias keywords entities category target-audience similar embedding-source success]} results]
               (format "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s"
                       (str/escape title {"\"" "\"\""})
                       (or link "")
                       (or published "")
                       category
                       sentiment
                       (or bias "")
                       (or target-audience "")
                       (str/join "; " keywords) 
                       (str/join "; " (or entities []))
                       (str/escape (format-similar similar) {"\"" "\"\""})
                       (or embedding-source "")
                       (str/escape (or tldr "") {"\"" "\"\""})
                       (str/escape summary {"\"" "\"\""})
                       success))]
    (str header (str/join "\n" rows))))

(defn format-markdown [results]
  (let [header "# News Analysis Report\n\n"
        date (format "Generated: %s\n\n" (timestamp))
        table-header "| Title | Sentiment | Bias | Category | Status |\n|-------|-----------|------|----------|--------|\n"
        rows (for [{:keys [title sentiment bias category success]} results]
               (format "| %s | %s | %s | %s | %s |"
                       title sentiment (or bias "N/A") category (if success "✓" "✗")))
        details (apply str
                       (for [{:keys [title link summary tldr keywords entities target-audience similar embedding-source success]} results]
                         (format "\n## %s\n\n**Status:** %s\n\n**Link:** %s\n\n**Target Audience:** %s\n\n**Keywords:** %s\n\n**Entities:** %s\n\n**TL;DR:** %s\n\n**Summary:** %s\n\n**Similar:** %s | **Embedding:** %s\n\n---\n"
                                 title (if success "Success" "Failed") (or link "N/A") (or target-audience "N/A") (str/join ", " keywords) (str/join ", " (or entities [])) (or tldr "N/A") summary (format-similar similar) (or embedding-source "unknown"))))]
    (str header date table-header (str/join "\n" rows) "\n" details)))

(defn save-results [results output-dir format]
  (ensure-dir output-dir)
  (let [ext (name format)
        filename (str output-dir "/news_analysis_" (timestamp) "." ext)
        content (case format
                  :txt (format-txt results)
                  :json (format-json results)
                  :csv (format-csv results)
                  :md (format-markdown results)
                  (format-txt results))]
    (spit filename content)
    filename))

;; Command line options
(def cli-options
  [["-c" "--country COUNTRY" "Country code (e.g., us, gb, cn)"
    :default (:country default-config)]
   ["-l" "--language LANGUAGE" "Language code (e.g., en, zh, es)"
    :default (:language default-config)]
   ["-n" "--max-articles NUM" "Maximum number of articles to analyze"
    :default (:max-articles default-config)
    :parse-fn #(Integer/parseInt %)
    :validate [#(and (> % 0) (<= % 10)) "Must be between 1 and 10"]]
   ["-f" "--format FORMAT" "Output format: txt, json, csv, md"
    :default (:output-format default-config)
    :parse-fn keyword
    :validate [#{:txt :json :csv :md} "Must be one of: txt, json, csv, md"]]
   ["-o" "--output-dir DIR" "Output directory"
    :default (:output-dir default-config)]
   ["-d" "--delay MS" "Delay between requests (ms)"
    :default (:request-delay default-config)
    :parse-fn #(Integer/parseInt %)]
   ["-m" "--model MODEL" "Primary LLM model (free tier)"
    :default (:model default-config)]
   ["-b" "--fallback-model MODEL" "Fallback LLM model when primary is rate-limited (paid)"
    :default (:fallback-model default-config)]
   ["-e" "--embedding-url URL" "Doc2Vec embedding service base URL"
    :default (:embedding-url default-config)]
   ["-t" "--similarity-threshold THRESHOLD" "Cosine similarity threshold (0-1) for linking news"
    :default (:similarity-threshold default-config)
    :parse-fn #(Double/parseDouble %)
    :validate [#(and (>= % 0.0) (<= % 1.0)) "Must be between 0 and 1"]]
   ["-s" "--symbol SYMBOL" "Stock symbol or search query (e.g., AAPL, Tesla)"
    :default (:symbol default-config)]
   ["-q" "--query QUERY" "Custom search query for news"
    :default (:search-query default-config)]
   ["-h" "--help" "Show help"]])

;; Print statistics
(defn print-stats [results elapsed-ms]
  (let [total (count results)
        successful (count (filter :success results))
        failed (- total successful)
        sentiments (frequencies (map :sentiment results))]
    (println)
    (log-info "═══════════════════ Statistics ═══════════════════")
    (log-success (format "Total articles: %d" total))
    (log-success (format "Successful: %d" successful))
    (when (> failed 0)
      (log-error (format "Failed: %d" failed)))
    (log-info (format "Time elapsed: %.2f seconds" (/ elapsed-ms 1000.0)))
    (log-info "Sentiment distribution:")
    (doseq [[sentiment count] sentiments]
      (println (format "  %s: %d" sentiment count)))
    (log-info "═══════════════════════════════════════════════════")))

;; Main entry point
(defn -main [& args]
  (let [{:keys [options summary errors]} (parse-opts args cli-options)]
    
    ;; Handle help or errors
    (when (:help options)
      (println "News Analysis Tool")
      (println summary)
      (System/exit 0))
    
    (when errors
      (doseq [error errors]
        (log-error error))
      (System/exit 1))
    
    ;; Validate API keys
    (let [api-keys (validate-api-keys)
          {:keys [country language max-articles output-dir delay model fallback-model symbol query embedding-url similarity-threshold]} options
          output-format (:format options)
          search-query (or query symbol)
          start-time (System/currentTimeMillis)]
      
      (log-info "Starting news analysis...")
      (log-info (clojure.core/format "Primary model: %s" model))
      (when fallback-model
        (log-info (clojure.core/format "Fallback model: %s" fallback-model)))
      
      ;; Fetch news
      (let [news (fetch-news (:newsdata api-keys) country language max-articles search-query)]
        (if (empty? news)
          (log-error "No articles to analyze. Exiting.")
          (do
            ;; Analyze articles with progress
            (log-info (clojure.core/format "Analyzing %d articles..." (count news)))
            (let [results (doall
                           (map-indexed
                            (fn [idx article]
                              (show-progress (inc idx) (count news))
                              (analyze-article (:openrouter api-keys) article model fallback-model delay (:max-retries default-config) embedding-url similarity-threshold))
                            news))
                  elapsed (- (System/currentTimeMillis) start-time)
                  filename (save-results results output-dir output-format)]
              
              (println) ; New line after progress bar
              (print-stats results elapsed)
              (log-success (clojure.core/format "Results saved to: %s" filename)))))))))