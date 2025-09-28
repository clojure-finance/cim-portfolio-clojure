(ns cim_portfolio.httpclient
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clj-http.client :as client]))


;; Get API Key (Environment Variables) from config.edn
(defn load-config []
    (with-open [r (io/reader "config.edn")]
        (edn/read (java.io.PushbackReader. r))))

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


;; Creating a function to handle client requests to DeepSeek V3.1 and returns the LLM Response

(defn ask-chatbot [client-message]
    (let [deepseek-api-key (:deepseek-api-key (load-config))
          payload {:model "deepseek-chat" ;; Non-reasoning model
                   :max-tokens 500 ;; ~350 words
                   :messages [
                        {:role "system" :content "You are a helpful assistant."}
                        {:role "user" :content client-message}]
                   :stream false}]

        ; Send request to API Endpoint and deserialize response
        (let [response-body (json/read-str (
            (client/post "https://api.deepseek.com/chat/completions"
                {:content-type :json
                :headers {"Authorization" (format "Bearer %s" deepseek-api-key)}
                :body (json/write-str payload)
                }) :body) :key-fn keyword)]

            ; Deserialize the response and obtain LLM response
            {:message (((first (response-body :choices)) :message) :content)
             :token-usage (response-body :usage)})))

;; Creating a function to display text neatly with newlines inserted every n words

(defn wrap-text
    [n text]
    (->> (str/split text #"\s+")
         (partition-all n)
         (map #(str/join " " %))
         (str/join "\n")
    ))

;; Send user prompt to LLM and obtain response
(def chatbot-response (ask-chatbot "Can you tell me the current market conditions that may affect one's portfolio?"))

;; Chatbot response message
(->> (chatbot-response :message) 
     (wrap-text 10)
)

;; Output Token Usage
(let [token-usage (chatbot-response :token-usage)]
    (format "Input Tokens: %s\nOutput Tokens: %s\nTotal Tokens Used: %s" 
        (token-usage :prompt_tokens)
        (token-usage :completion_tokens)
        (token-usage :total_tokens)))

;; Try getting market news based on tickers and date (maybe get market news from wsj.com for now)
; keyword should be a string
(defn get-market-news-json [query date]
    (let [newsapi-api-key (:newsapi-api-key (load-config))
          response (client/get "https://newsapi.org/v2/everything" 
            {:accept :json
             :query-params {"apiKey" newsapi-api-key
                            "q" query 
                            "from" (.minusWeeks date 2) ;; For now, earliest article date is two weeks before the specified date
                            "to" date
                            "language" "en"
                            "sortBy" "popularity"
                            "sources" "the-wall-street-journal,bloomberg"}})] ;;This would return 50 articles for now
        (json/read-str (response :body) :key-fn keyword)))

(def market-news-raw (get-market-news-json "" (java.time.LocalDate/now)))
;; Make a call to the LLM asking which 5 article titles are most relevant to the portfolio. We take their contents and use it for RAG.

;; RAG Example (for demonstration)

(def enhanced-response (ask-chatbot "According to the WSJ, Stocks closed the week at record highs, propelled by investor optimism that the Federal Reserve 
                                                will keep cutting interest rates—and keep the market rally going. All three major indexes posted weekly gains of more than 1%. 
                                                The Fed cut the benchmark borrowing rate by 0.25% on Wednesday, and traders are now anticipating additional rate reductions in October and December. 
                                                The median projection of the Fed’s interest-rate setting committee also penciled in two additional cuts in 2025.
                                                
                                                The S&P 500 rose 0.5% on Friday, while the tech-heavy Nasdaq gained 0.7%. The Dow Jones Industrial Average added 0.4%, or 173 points.
                                                
                                                Based on this information, can you tell me the current market conditions that may affect one's portfolio?"))

;; Enhanced Response
(->> (enhanced-response :message)
     (wrap-text 10))











