(ns news-llm.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
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
   :output-dir "reports"
   :request-delay 1000
   :max-retries 1
   :model "google/gemini-2.0-flash-exp:free"})

;; ANSI color codes
(def colors
  {:reset "\u001b[0m"
   :red "\u001b[31m"
   :green "\u001b[32m"
   :yellow "\u001b[33m"
   :blue "\u001b[34m"
   :magenta "\u001b[35m"
   :cyan "\u001b[36m"})

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
(defn fetch-news [api-key country language max-articles]
  (try
    (log-info (format "Fetching news: country=%s, language=%s, max=%d" country language max-articles))
    (let [resp (http/get newsdata-url
                         {:query-params {:apikey api-key
                                         :country country
                                         :language language}
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
    
    ;; 1. Try Jina Reader
    (log-info "Strategy 1: Jina Reader")
    (let [jina-url (str "https://r.jina.ai/" url)
          resp (try 
                 (http/get jina-url
                           {:headers {"User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"}
                            :timeout 20000
                            :throw-exceptions false})
                 (catch Exception e 
                   (log-warn (str "Jina request failed: " (.getMessage e)))
                   {:status 500}))]
      
      (if (and (= 200 (:status resp)) 
               (not (str/blank? (:body resp)))
               (> (count (:body resp)) 200))
        (do 
          (log-success (format "Jina success! Extracted %d chars" (count (:body resp))))
          (:body resp))
        
        (do
          (log-warn (format "Jina failed (Status: %s, Length: %d). Switching to Strategy 2..." 
                            (:status resp) (count (str (:body resp)))))
          
          ;; 2. Fallback: Direct Jsoup with Browser Headers
          (log-info "Strategy 2: Direct Jsoup Scraping")
          (try
            (let [conn (-> (Jsoup/connect url)
                           (.userAgent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                           (.header "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                           (.timeout 15000)
                           (.followRedirects true)
                           (.ignoreHttpErrors true))
                  resp (.execute conn)
                  doc (.parse resp)
                  ;; Remove clutter
                  _ (.remove (.select doc "script, style, nav, header, footer, iframe, .ad, .advertisement, .social-share, .menu, .cookie-banner"))
                  ;; Extract text
                  text (-> (.body doc) .text)]
              
              (if (> (count text) 300)
                (do 
                  (log-success (format "Jsoup success! Extracted %d chars" (count text)))
                  text)
                (do 
                  (log-warn "Jsoup content too short.") 
                  nil)))
            (catch Exception e
              (log-warn (format "Jsoup failed: %s" (.getMessage e)))
              nil)))))
    (catch Exception e
      (log-warn (format "Global fetch error: %s" (.getMessage e)))
      nil)))

;; Build prompt for LLM
(defn build-prompt [article]
  (let [raw-content (or (:full-content article) (:description article) (:content article) "No content available")
        ;; Truncate content to ~6000 chars to avoid 429/Context Limit on free models
        content (if (> (count raw-content) 6000)
                  (str (subs raw-content 0 6000) "\n...[Content Truncated]...")
                  raw-content)]
    (str "Analyze the following news based on the title and description provided. Return ONLY valid JSON with these exact keys: summary, sentiment, keywords, category.\n\n"
         "Title: " (:title article) "\n"
         "Description: " content
         "\n\nFormat:\n{\"summary\": \"...\", \"sentiment\": \"Positive/Negative/Neutral\", \"keywords\": [\"...\"], \"category\": \"...\"}")))
;; Call OpenRouter LLM with retry logic
(defn call-llm [api-key prompt model retries]
  (try
    (let [body {:model model
                :messages [{:role "system"
                            :content "You are a disciplined news analyst. Return ONLY valid JSON with keys: summary, sentiment, keywords, category. Do not use Markdown code blocks."}
                           {:role "user"
                            :content prompt}]}
          resp (http/post openrouter-url
                          {:headers {"Authorization" (str "Bearer " api-key)
                                     "Content-Type" "application/json"}
                           :body (json/encode body)
                           :as :json
                           :throw-exceptions true})
          text (-> resp :body :choices first :message :content)]
      ;; Improved cleaning
      (let [cleaned (-> text
                        (str/replace #"```json" "")
                        (str/replace #"```" "")
                        str/trim)]
        (or (re-find #"(?s)\{.*\}" cleaned) cleaned)))
    (catch Exception e
      (if (> retries 0)
        (do
          (log-warn (format "LLM call failed, retrying... (%d left)" retries))
          (Thread/sleep 2000)
          (call-llm api-key prompt model (dec retries)))
        (do
          (log-error (format "LLM call failed: %s" (.getMessage e)))
          "{\"summary\":\"Analysis failed\",\"sentiment\":\"Neutral\",\"keywords\":[],\"category\":\"Unknown\"}")))))

;; Analyze one article with rate limiting
(defn analyze-article [api-key article model delay max-retries]
  (Thread/sleep delay)
  (let [full-content (when (:link article) (fetch-full-content (:link article)))
        article-with-content (if full-content
                               (assoc article :full-content full-content)
                               article)
        raw (call-llm api-key (build-prompt article-with-content) model max-retries)]
    (try
      (let [parsed (json/decode raw true)]
        {:title (:title article)
         :link (:link article)
         :published (:pubDate article)
         :summary (:summary parsed)
         :sentiment (:sentiment parsed)
         :keywords (or (:keywords parsed) [])
         :category (or (:category parsed) "General")
         :success true})
      (catch Exception e
        (log-error (format "Failed to parse JSON for article: %s" (:title article)))
        (log-error (format "Raw LLM response: %s" raw))
        {:title (:title article)
         :link (:link article)
         :published (:pubDate article)
         :summary "Parse error"
         :sentiment "Neutral"
         :keywords []
         :category "Unknown"
         :success false}))))

;; Output formatters
(defn format-txt [results]
  (apply str
         (for [{:keys [title link published summary sentiment keywords category success]} results]
           (format "Title: %s\nLink: %s\nPublished: %s\nCategory: %s\nSentiment: %s\nKeywords: %s\nSummary: %s\nStatus: %s\n%s\n"
                   title (or link "N/A") (or published "N/A") category sentiment
                   (str/join ", " keywords) summary
                   (if success "✓" "✗")
                   (apply str (repeat 80 "-"))))))

(defn format-json [results]
  (json/encode results {:pretty true}))

(defn format-csv [results]
  (let [header "Title,Link,Published,Category,Sentiment,Keywords,Summary,Success\n"
        rows (for [{:keys [title link published summary sentiment keywords category success]} results]
               (format "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s"
                       (str/escape title {\" "\"\""})
                       (or link "")
                       (or published "")
                       category
                       sentiment
                       (str/join "; " keywords)
                       (str/escape summary {\" "\"\""})
                       success))]
    (str header (str/join "\n" rows))))

(defn format-markdown [results]
  (let [header "# News Analysis Report\n\n"
        date (format "Generated: %s\n\n" (timestamp))
        table-header "| Title | Sentiment | Category | Keywords |\n|-------|-----------|----------|----------|\n"
        rows (for [{:keys [title sentiment category keywords]} results]
               (format "| %s | %s | %s | %s |"
                       title sentiment category (str/join ", " keywords)))
        details (apply str
                       (for [{:keys [title link summary]} results]
                         (format "\n## %s\n\n**Link:** %s\n\n**Summary:** %s\n\n"
                                 title (or link "N/A") summary)))]
    (str header date table-header (str/join "\n" rows) "\n" details)))

(defn save-results [results output-dir format]
  (ensure-dir output-dir)
  (let [ext (name format)
        filename (str output-dir "/analysis-" (timestamp) "." ext)
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
   ["-m" "--model MODEL" "LLM model to use (e.g., minimax/minimax-01, deepseek/deepseek-r1:free)"
    :default (:model default-config)]
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
          {:keys [country language max-articles output-dir delay model]} options
          output-format (:format options)
          start-time (System/currentTimeMillis)]
      
      (log-info "Starting news analysis...")
      (log-info (clojure.core/format "Using model: %s" model))
      
      ;; Fetch news
      (let [news (fetch-news (:newsdata api-keys) country language max-articles)]
        (if (empty? news)
          (log-error "No articles to analyze. Exiting.")
          (do
            ;; Analyze articles with progress
            (log-info (clojure.core/format "Analyzing %d articles..." (count news)))
            (let [results (doall
                           (map-indexed
                            (fn [idx article]
                              (show-progress (inc idx) (count news))
                              (analyze-article (:openrouter api-keys) article model delay (:max-retries default-config)))
                            news))
                  elapsed (- (System/currentTimeMillis) start-time)
                  filename (save-results results output-dir output-format)]
              
              (println) ; New line after progress bar
              (print-stats results elapsed)
              (log-success (clojure.core/format "Results saved to: %s" filename))
              (log-success "Done!"))))))))
