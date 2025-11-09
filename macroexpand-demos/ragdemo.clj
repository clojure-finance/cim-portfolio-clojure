(ns cim-portfolio.ragdemo
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [datalevin.core :as d]
            [scicloj.kindly.v4.kind :as kind]))

;; Function to get API Keys (Environment Variables) from config.edn

(defn load-config []
  (with-open [r (io/reader "config.edn")]
    (edn/read (java.io.PushbackReader. r))))

;; Creating a function to handle client requests to DeepSeek V3.1 and 
;; returns the LLM Response

(defn ask-chatbot [client-message]
  (let [deepseek-api-key (:deepseek-api-key (load-config))
        payload {:model "deepseek-chat" ;; Non-reasoning model
                 :max-tokens 500 ;; ~350 words
                 :messages [{:role "system"
                             :content "You are a helpful assistant."}
                            {:role "user"
                             :content client-message}]
                 :stream false}
          ; Send request to API Endpoint and deserialize response
        response-body
        (json/read-str ((client/post
                         "https://api.deepseek.com/chat/completions"
                         {:content-type :json
                          :headers {"Authorization"
                                    (format "Bearer %s" deepseek-api-key)}
                          :body (json/write-str payload)}) :body)
                       :key-fn keyword)]

            ; Deserialize the response and obtain LLM response
    {:message (((first (response-body :choices)) :message) :content)
     :token-usage (response-body :usage)}))

;; Without the RAG Pipeline

(def basic-response
  (ask-chatbot "Tell me about Bank of America's performance 
                as of 15 October 2025"))


(kind/md (:message basic-response))

;; Show articles

;; Function to convert text from stored articles into a sequence

(defn txt-to-vec [path]
  (with-open [rdr (io/reader path)]
    (let [file-name (.getName (clojure.java.io/file path)) ;; Get File Name 
          title (subs file-name 0 (.lastIndexOf file-name "."))] ;; Get Title
      (map #(str "Title: " title "\nParagraph: " %)
           (filter #(not= "" %) (vec (line-seq rdr)))))))

(txt-to-vec "articles/Wells Fargo Third-Quarter Profit Rises.txt")

;; We take all the text in articles, join them and clean them.

(def word-chunks (->> (.listFiles (io/file "articles/"))
                      (filter #(.isFile %))
                      (map #(.getName %))
                      (map #(str "articles/" %))
                      (map txt-to-vec)
                      (apply concat)
                      (vec)))

(->> (.listFiles (io/file "articles/"))
     (filter #(.isFile %))
     (map #(.getName %))
     (map #(str "articles/" %))
     (map txt-to-vec)
     (apply concat)
     (vec))

word-chunks

(count word-chunks)

;; Creating a function to create embeddings based on chunks

(defn embed-text [inputs] ;; inputs should be a sequence
  (let [hf-api-key (:huggingface-api-key (load-config))
        payload {:inputs inputs}
        response-body
        (json/read-str ((client/post
                         "https://router.huggingface.co/hf-inference/models/BAAI/bge-large-en-v1.5/pipeline/feature-extraction"
                         {:content-type :json
                          :headers {"Authorization"
                                    (format "Bearer %s" hf-api-key)}
                          :body (json/write-str payload)}) :body)
                       :key-fn keyword)]
    response-body))

(def embeddings (embed-text word-chunks)) ;; A vector of vectors

(kind/md (first word-chunks))

(first embeddings)

(count (first embeddings)) ;; 1024 dimensions

(count embeddings)

;; We will be using datalevin to create a vector database

;; Create a key-value store
(def lmdb (d/open-kv "/tmp/vector-db"))

;; Initialize the Vector Database (using cosine similarity)
(def index (d/new-vector-index lmdb {:dimensions 1024 :metric-type :cosine}))

;; Enumerate the embeddings
(def embedding-map
  (into {} ;; The index will refer to the i-th element in word-chunks
        (map vector (range (count embeddings))
             embeddings)))

(embedding-map 0)
(embedding-map 1)
(embedding-map 50)

;; Feed index and embeddings into the vector database
(doseq [[i vs] embedding-map] (d/add-vec index i vs))

;; Testing out the vector search
(def test-embedding (embed-text "Tell me about Bank of America's Performance"))

test-embedding ;; Embedding for user query

(d/search-vec index test-embedding {:top 5})

(word-chunks 0)
(word-chunks 14)
(word-chunks 4)
(word-chunks 13)
(word-chunks 15)

(def relevant-chunks [(word-chunks 15) (word-chunks 0) (word-chunks 14)
                      (word-chunks 4) (word-chunks 13)])

relevant-chunks

;; With the RAG Pipeline

(def better-prompt
  (format "Based on the following information on 15 October 2025: 
           
           %s
           
           Tell me about Bank of America's performance 
           as of 15 October 2025" relevant-chunks))

(kind/md better-prompt)

(def better-response (ask-chatbot better-prompt))

(kind/md (:message better-response))

;; Now, let's assume our portfolio only contains Bank of America Stocks
;; With the RAG Pipeline and Prompt Engineering

(def enhanced-prompt
  (format
   "You are an AI financial analyst integrated into a portfolio analysis system. 
      Your role is to provide clear, factual, and non-speculative explanations of 
      a portfolio’s performance on a given date based solely on the information 
      provided. You must not invent events, make assumptions beyond the data, 
      or use vague language. 
      Your output must be grounded exclusively in the financial metrics and 
      market news included in the input.  
      
    The input contains two parts:
      1. Portfolio performance metrics for a specific date.
      2. A list of relevant market news articles from that day, retrieved from 
      trusted sources.
   
    Your task is to analyze the relationship between the portfolio’s behavior 
      and the news events. Follow these rules strictly:
      
     - Only reference the news items listed in the input.
       - Do not speculate about causes not mentioned in the news or metrics.
       - If a portfolio asset is mentioned in a news article, explain how that 
      event likely influenced its return.
       - Link changes in volatility to broader market conditions described in the 
      news (e.g., Fed decisions, earnings surprises, geopolitical shifts).
       - Use neutral, professional tone. Avoid words like “likely,” “possibly,” or 
      “may.” Instead, use phrases such as “consistent with,” “aligned with,” 
      or “suggesting.”
       - If no news item clearly explains the performance, state: 
      “No directly relevant market events were identified. 
       The movement may reflect broad market trends, 
      sector rotation, or idiosyncratic factors not captured in available news.”
       - Limit your response to 3–4 sentences.
       - Output only the explanation; do not include headings, summaries, 
      or meta-commentary.
      
     Example format:
      On [DATE], the portfolio’s [positive/negative] return was driven by 
      [specific assets or factors], consistent with [news headline]. 
      The change in volatility aligns with [market condition], 
      particularly affecting [asset/sector].
   
     Now, process the following input:
   
       {
       “date”: “2025-10-15”,
       “portfolio_return”: 2.3,
       “volatility_change”: 2.0,
       “assets”: [\"Bank of America\"],
       “top_asset_return”: 2.3,
       “news”: %s
       }" relevant-chunks))

(kind/md (format "{“date”: “2025-10-15”,\n
                   “portfolio_return”: 2.3,\n
                   “volatility_change”: 2.0,\n
                   “assets”: [\"Bank of America\"],\n
                   “top_asset_return”: 2.3,\n
                   “news”: %s\n
                   }" relevant-chunks))

(kind/md enhanced-prompt)

(def enhanced-response
  (ask-chatbot enhanced-prompt))

(kind/md (:message enhanced-response))

;; Close Vector Database
(d/clear-vector-index index)


;; ;; Testing Simple GET Request
;; (client/get "https://httpbin.org/get" {:accept :json})

;; ;; Parsing the response of said GET Request into a Clojure Map
;; (let [response (client/get "https://httpbin.org/get" {:accept :json})]
;;     (((json/read-str (response :body) :key-fn keyword)
;;         :headers)
;;             :Accept)
;;         )

;; ;; Testing Simple POST Request
;; (client/post "https://httpbin.org/post" {:body "{\"json\": \"test\"}"})

;; Send user prompt to LLM and obtain response
;; (def chatbot-response (ask-chatbot "Can you tell me the current market conditions that may affect one's portfolio?"))

;; Chatbot response message
;; (->> (chatbot-response :message) 
;;      (wrap-text 10)
;; )

;; Output Token Usage
;; (let [token-usage (chatbot-response :token-usage)]
;;     (format "Input Tokens: %s\nOutput Tokens: %s\nTotal Tokens Used: %s" 
;;         (token-usage :prompt_tokens)
;;         (token-usage :completion_tokens)
;;         (token-usage :total_tokens)))

;; Try getting market news based on tickers and date (maybe get market news from wsj.com for now)
; keyword should be a string
;; (defn get-market-news-json [query date]
;;     (let [newsapi-api-key (:newsapi-api-key (load-config))
;;           response (client/get "https://newsapi.org/v2/everything" 
;;             {:accept :json
;;              :query-params {"apiKey" newsapi-api-key
;;                             "q" query 
;;                             "from" (.minusWeeks date 2) ;; For now, earliest article date is two weeks before the specified date
;;                             "to" date
;;                             "language" "en"
;;                             "sortBy" "popularity"
;;                             "sources" "the-wall-street-journal,bloomberg"}})] ;;This would return 50 articles for now
;;         (json/read-str (response :body) :key-fn keyword)))

;; (def market-news-raw (get-market-news-json "" (java.time.LocalDate/now)))

;; Make a call to the LLM asking which 5 article titles are most relevant to the portfolio. We take their contents and use it for RAG.

;; market-news-raw

;; (map :content (:articles market-news-raw))




;; RAG Example (for demonstration)

;; (def enhanced-response (ask-chatbot "According to the WSJ, Stocks closed the week at record highs, propelled by investor optimism that the Federal Reserve 
;;                                                 will keep cutting interest rates—and keep the market rally going. All three major indexes posted weekly gains of more than 1%. 
;;                                                 The Fed cut the benchmark borrowing rate by 0.25% on Wednesday, and traders are now anticipating additional rate reductions in October and December. 
;;                                                 The median projection of the Fed’s interest-rate setting committee also penciled in two additional cuts in 2025.

;;                                                 The S&P 500 rose 0.5% on Friday, while the tech-heavy Nasdaq gained 0.7%. The Dow Jones Industrial Average added 0.4%, or 173 points.

;;                                                 Based on this information, can you tell me the current market conditions that may affect one's portfolio?"))

;; ;; Enhanced Response
;; (->> (enhanced-response :message)
;;      (wrap-text 10))











