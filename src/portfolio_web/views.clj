(ns portfolio-web.views
  (:require [hiccup2.core :as h]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [news-llm.core :as news])
  (:import java.text.NumberFormat
           java.util.Locale
           java.util.Currency))

;; ─── Shared helpers ───────────────────────────────────────────────────────────

(def currency-formatter
  (doto (NumberFormat/getCurrencyInstance Locale/US)
    (.setCurrency (Currency/getInstance "USD"))
    (.setMinimumFractionDigits 0)
    (.setMaximumFractionDigits 0)))

(def github-icon
  [:svg.github-icon {:width "20" :height "20" :viewBox "0 0 98 96"
                     :xmlns "http://www.w3.org/2000/svg"
                     :aria-hidden "true" :focusable "false"}
   [:g {:clip-path "url(#clip0_730_27126)"}
    [:path {:d "M41.4395 69.3848C28.8066 67.8535 19.9062 58.7617 19.9062 46.9902C19.9062 42.2051 21.6289 37.0371 24.5 33.5918C23.2559 30.4336 23.4473 23.7344 24.8828 20.959C28.7109 20.4805 33.8789 22.4902 36.9414 25.2656C40.5781 24.1172 44.4062 23.543 49.0957 23.543C53.7852 23.543 57.6133 24.1172 61.0586 25.1699C64.0254 22.4902 69.2891 20.4805 73.1172 20.959C74.457 23.543 74.6484 30.2422 73.4043 33.4961C76.4668 37.1328 78.0937 42.0137 78.0937 46.9902C78.0937 58.7617 69.1934 67.6621 56.3691 69.2891C59.623 71.3945 61.8242 75.9883 61.8242 81.252L61.8242 91.2051C61.8242 94.0762 64.2168 95.7031 67.0879 94.5547C84.4102 87.9512 98 70.6289 98 49.1914C98 22.1074 75.9883 6.69539e-07 48.9043 4.309e-07C21.8203 1.92261e-07 -1.9479e-07 22.1074 -4.3343e-07 49.1914C-6.20631e-07 70.4375 13.4941 88.0469 31.6777 94.6504C34.2617 95.6074 36.75 93.8848 36.75 91.3008L36.75 83.6445C35.4102 84.2188 33.6875 84.6016 32.1562 84.6016C25.8398 84.6016 22.1074 81.1563 19.4277 74.7441C18.375 72.1602 17.2266 70.6289 15.0254 70.3418C13.877 70.2461 13.4941 69.7676 13.4941 69.1934C13.4941 68.0449 15.4082 67.1836 17.3223 67.1836C20.0977 67.1836 22.4902 68.9063 24.9785 72.4473C26.8926 75.2227 28.9023 76.4668 31.2949 76.4668C33.6875 76.4668 35.2187 75.6055 37.4199 73.4043C39.0469 71.7773 40.291 70.3418 41.4395 69.3848Z"
           :fill "currentColor"}]]
   [:defs [:clipPath {:id "clip0_730_27126"} [:rect {:width "98" :height "96" :fill "white"}]]]])

(defn page-header [& nav-items]
  [:header
   [:h1 [:a.title-link {:href "/"} "CIM Portfolio Analysis Tool"]]
   (into [:nav.top-nav] nav-items)
   [:a.github-link {:href "https://github.com/clojure-finance/cim-portfolio-clojure/tree/web-application"
                    :target "_blank" :rel "noopener noreferrer"
                    :aria-label "View project on GitHub"}
    github-icon " View on GitHub"]])

;; ─── Home page ────────────────────────────────────────────────────────────────

(def html-home-body
  [:body
   (page-header [:a.nav-link {:href "/news"} "News Analysis"])
   [:div.container
    [:h2 "Provide Your Trades"]

    [:div.input-choice
     [:label [:input.input-radio-button {:type "radio" :name "input-mode" :value "manual" :checked true}] "Enter manually"]
     [:label [:input.input-radio-button {:type "radio" :name "input-mode" :value "file"}] "Upload CSV"]]

    [:form {:method "post" :action "/analyze-portfolio" :enctype "multipart/form-data" :id "trades-submission-form"}
     [:div.manual-input
      [:div.example-controls
       [:label.example-label "Need an example? "]
       [:button.example-button {:type "button" :id "fill-example-button"} "Fill with example trades"]
       [:pre.example-data.hidden
        "2024-12-01,buy,55000,NVDA\n2025-01-16,buy,29600,GOOG\n2025-03-14,buy,10000,MSFT\n2025-04-19,buy,7500,MSFT\n2025-05-05,buy,23000,AAPL\n2025-05-06,buy,17400,AMZN\n2025-08-11,buy,11500,META\n2025-10-26,sell,5500,NVDA\n2025-12-08,buy,5500,TSLA"]]
      [:textarea {:name "trades" :rows "5" :placeholder "YYYY-MM-DD,action,amount,ticker,price"}]]

     [:div.file-input
      [:input {:type "file" :name "trades-file" :accept ".csv,.txt"}]]

     [:label.label-h3 {:for "starting-cash"} "Starting Cash"]
     [:input {:type "text" :id "starting-cash-input" :name "starting-cash" :value "50000000"}]

     [:div.checkbox-container
      [:label [:input {:type "checkbox" :name "show-capm-metrics" :value "true"}] "Show CAPM Alpha and Beta of Stocks"]
      [:label [:input {:type "checkbox" :name "show-stock-performances" :value "true"}] "Show Individual Stock Performances"]]

     [:button {:type "submit" :id "submit-trades-button"} "Analyze Portfolio"]]

    [:script {:src "/js/home_page.js"}]]])

(defn home-page []
  (str (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:title "CIM Portfolio Analysis Tool"]
          [:link {:rel "stylesheet" :href "/styles.css"}]]
         html-home-body])))

;; ─── Portfolio results page ───────────────────────────────────────────────────

(defn portfolio-summary-section [data]
  [:div#summary.container
   [:div.card
    [:h2 "Portfolio Summary"]
    [:div.metrics
     [:div.metric [:span "Current Portfolio Value:"] [:span#portfolioValue.num (.format currency-formatter (data :current-portfolio-value))]]
     [:div.metric [:span "Cash in Portfolio:"] [:span#cashValue.num (.format currency-formatter (data :cash))]]
     [:div.metric [:span "Stock Holdings Value:"] [:span#stockValue.num (.format currency-formatter (data :stocks))]]]]

   [:div.card
    [:h2 "Performance Metrics"]
    [:div.metrics
     [:div.metric [:span "Annualized Return of Portfolio:"] [:span#annualReturn (format "%.2f%%" (data :annualized-portfolio-return))]]
     [:div.metric [:span "Annualized Volatility of Portfolio:"] [:span#annualVolatility (format "%.2f%%" (data :annualized-portfolio-volatility))]]
     [:div.metric [:span "1-Year Cumulative Portfolio Return:"]
      [:span#cumulativeReturn (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-today (data :past-five-weeks-1y-cumulative-return-incl-cash))))]]]]

   [:div.card
    [:h2 "Portfolio Allocation"]
    [:div.allocation-columns
     [:div.allocation-column [:h3 "Ticker"]
      [:ul#cashByStockTickers (for [ticker (data :unique-tickers)] [:li ticker])]]
     [:div.allocation-column [:h3 "Nominal Value"]
      [:ul#cashByStockValues
       (for [ticker (data :unique-tickers)]
         [:li (str (if (neg? (get (:values (data :current-stock-holdings-and-weights)) ticker)) "-" "")
                   (.format currency-formatter (abs (get (:values (data :current-stock-holdings-and-weights)) ticker))))])]]
     [:div.allocation-column [:h3 "Weight"]
      [:ul#cashByStockWeights
       (for [ticker (data :unique-tickers)]
         [:li (format "%.2f%%" (* 100 (get (:weights (data :current-stock-holdings-and-weights)) ticker)))])]]]]

   [:div.card.return-comparison-card
    [:h2 "1-Year Cumulative Portfolio Return"]
    [:div.return-comparison
     [:div.return-column [:h3 "Including Cash"]
      [:ul#returnByWeekIncl
       (for [k [:one-year-cumulative-return-from-today
                :one-year-cumulative-return-from-one-week-ago
                :one-year-cumulative-return-from-two-weeks-ago
                :one-year-cumulative-return-from-three-weeks-ago
                :one-year-cumulative-return-from-four-weeks-ago
                :one-year-cumulative-return-from-five-weeks-ago]]
         [:li (format "%.2f%%" (* 100 (get (data :past-five-weeks-1y-cumulative-return-incl-cash) k)))])]]
     [:div.return-column [:h3 "Excluding Cash"]
      [:ul#returnByWeekExcl
       (for [k [:one-year-cumulative-return-from-today
                :one-year-cumulative-return-from-one-week-ago
                :one-year-cumulative-return-from-two-weeks-ago
                :one-year-cumulative-return-from-three-weeks-ago
                :one-year-cumulative-return-from-four-weeks-ago
                :one-year-cumulative-return-from-five-weeks-ago]]
         [:li (format "%.2f%%" (* 100 (get (data :past-five-weeks-1y-cumulative-return-excl-cash) k)))])]]]]])

(defn portfolio-analytics-section [data]
  [:div#analytics.container
   [:div.card.full-width
    [:h2 "Portfolio Value by Day"]
    [:div.graph [:div {:id "portfolio-value-by-day" :class "miscChart" :data-plot (json/write-str (data :portfolio-value-figs))}]]]

   [:div.card.full-width
    [:h2 "One Dollar Invested in Portfolio at Time Zero (Log(x) scale)"]
    [:div.graph [:div {:id "portfolio-performance-by-day" :class "miscChart" :data-plot (json/write-str (data :portfolio-one-dollar-performance-graph))}]]]

   [:div.card.full-width
    [:h2 "30-Day Annualized EWMA Rolling Volatility of Portfolio (λ = 0.94)"]
    [:div.graph [:div {:id "rolling-ewma-volatility" :class "miscChart"
                       :data-plot (json/write-str (data :default-rolling-ewma-volatility-figs))
                       :data-alt-plot (json/write-str (data :alternative-rolling-ewma-volatility-figs))}]]
    [:button {:id "lambdaSwitch"} "Switch to λ = 0.97"]]

   [:div.card.full-width
    [:h2 "30-Day Annualized Rolling Sharpe Ratio (EWMA λ = 0.94)"]
    [:div.graph [:div {:id "rolling-sharpe-ratio" :class "miscChart"
                       :data-plot (json/write-str (data :default-rolling-sharpe-ratio-figs))
                       :data-alt-plot (json/write-str (data :alternative-rolling-sharpe-ratio-figs))}]]
    [:button {:id "sharpeLambdaSwitch"} "Switch to λ = 0.97"]]

   (if (= (data :alpha-beta-figs) "")
     [:div.card.full-width [:h2 "Alpha & Beta (per stock) (Omitted)"]]
     [:div.card.full-width
      [:h2 "Alpha & Beta (per stock)"]
      [:div#alphaBetaContainer
       (for [ticker (data :unique-tickers)]
         [:details.stockAccordion
          [:summary [:strong ticker]]
          [:div.graphRow
           [:div.graph [:h4.rolling-alpha-header "Rolling Alpha"]
            [:div {:id (str ticker "-alpha") :class "alphaChart"
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " α")) %) (data :alpha-beta-figs)))}]]
           [:div.graph [:h4.rolling-beta-header "Rolling Beta"]
            [:div {:id (str ticker "-beta") :class "betaChart"
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " β")) %) (data :alpha-beta-figs)))}]]]])]])

   (if (= (data :stock-performances-graphs) "")
     [:div.card.full-width [:h2 "Stock Performances (Omitted)"]]
     [:div.card.full-width
      [:h2 "Stock Performances"]
      [:div#stockDetailsContainer
       (for [ticker (data :unique-tickers)]
         [:details.stockAccordion
          [:summary [:strong ticker]]
          [:div.graphRow
           [:div.graph [:h4.stock-performance-header "One Dollar Invested at Time Zero (Log(x) scale)"]
            [:div {:id (str ticker "-performance") :class "performanceChart"
                   :data-plot (json/write-str (get (data :stock-performances-graphs) ticker))}]]]])]])])

(defn portfolio-page [data]
  (str (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:title "CIM Portfolio Analysis Tool"]
          [:link {:rel "stylesheet" :href "/portfolio_styles.css"}]
          [:script {:src "https://cdn.plot.ly/plotly-2.32.0.min.js"}]]
         [:body
          (page-header
           [:a.back-button {:href "/"} "← Back to Home"]
           [:a.nav-link {:href "/news"} "News Analysis"])
          [:main
           (portfolio-summary-section data)
           (portfolio-analytics-section data)]
          [:script {:src "/js/portfolio.js"}]]])))

;; ─── News Analysis pages ──────────────────────────────────────────────────────

(defn news-page
  ([] (news-page nil))
  ([error-msg]
   (str (h/html
         [:html {:lang "en"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:title "News Analysis — CIM Portfolio Tool"]
           [:link {:rel "stylesheet" :href "/styles.css"}]
           [:style "
.news-form-card { background: #fff; border-radius: 8px; padding: 28px; box-shadow: 0 1px 4px rgba(0,0,0,.1); max-width: 640px; }
.provider-group { display: flex; gap: 20px; margin: 8px 0 16px; }
.provider-group label { display: flex; align-items: center; gap: 6px; font-weight: 500; cursor: pointer; }
.field-group { margin-bottom: 16px; }
.field-group label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 0.9rem; }
.field-group input, .field-group select { width: 100%; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 0.95rem; box-sizing: border-box; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.error-banner { background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 10px 14px; border-radius: 6px; margin-bottom: 16px; }
.hint { font-size: 0.8rem; color: #6b7280; margin-top: 3px; }
.submit-btn { background: #1d4ed8; color: #fff; border: none; padding: 11px 24px; border-radius: 6px; font-size: 1rem; font-weight: 600; cursor: pointer; width: 100%; margin-top: 8px; }
.submit-btn:hover { background: #1e40af; }
.loading-msg { display: none; text-align: center; color: #6b7280; margin-top: 10px; font-style: italic; }
"]]
          [:body
           (page-header [:a.nav-link {:href "/"} "← Portfolio Analysis"])
           [:div.container
            [:h2 "AI News Analysis"]
            [:p "Enter your API keys and search query to fetch and analyze news articles using an LLM."]
            (when error-msg
              [:div.error-banner error-msg])
            [:div.news-form-card
             [:form {:method "post" :action "/analyze-news"
                     :id "news-form"
                     :onsubmit "document.getElementById('loading-msg').style.display='block'"}

              ;; LLM Provider
              [:div.field-group
               [:label "LLM Provider"]
               [:div.provider-group
                [:label [:input {:type "radio" :name "llm-provider" :value "openrouter" :checked true}] "OpenRouter"]
                [:label [:input {:type "radio" :name "llm-provider" :value "deepseek"}] "DeepSeek (direct)"]]]

              ;; API Keys
              [:div.field-group
               [:label "Newsdata.io API Key"]
               [:input {:type "password" :name "newsdata-api-key" :required true :placeholder "pub_..."}]
               [:p.hint "Get a free key at newsdata.io (200 requests/day)"]]

              [:div.field-group
               [:label "LLM API Key"]
               [:input {:type "password" :name "llm-api-key" :required true :placeholder "sk-..."}]
               [:p.hint "OpenRouter key: openrouter.ai  |  DeepSeek key: platform.deepseek.com"]]

              ;; Model
              [:div.field-group
               [:label "Model"]
               [:select {:name "model"}
                [:optgroup {:label "OpenRouter (free)"}
                 (for [m news/free-models]
                   [:option {:value m} m])]
                [:optgroup {:label "DeepSeek (direct)"}
                 (for [m news/deepseek-models]
                   [:option {:value m} m])]]]

              ;; Search parameters
              [:div.field-row
               [:div.field-group
                [:label "Search Query / Ticker"]
                [:input {:type "text" :name "query" :placeholder "e.g. Apple, NVDA earnings"}]
                [:p.hint "Leave blank to use country/language defaults"]]
               [:div.field-group
                [:label "Max Articles (1–10)"]
                [:input {:type "number" :name "max-articles" :value "3" :min "1" :max "10"}]]]

              [:div.field-row
               [:div.field-group
                [:label "Country Code"]
                [:input {:type "text" :name "country" :value "us" :placeholder "us"}]]
               [:div.field-group
                [:label "Language Code"]
                [:input {:type "text" :name "language" :value "en" :placeholder "en"}]]]

              ;; Delay between requests
              [:div.field-group
               [:label "Delay Between Articles (ms)"]
               [:input {:type "number" :name "delay" :value "1000" :min "500" :max "10000"}]
               [:p.hint "Increase if hitting rate limits"]]

              [:button.submit-btn {:type "submit"} "Analyze News"]
              [:p#loading-msg.loading-msg "Analyzing... this may take 1–3 minutes. Please wait."]]]]]]))))

(defn sentiment-color [sentiment]
  (case (str/lower-case (or sentiment ""))
    "positive" "#d1fae5"
    "negative" "#fee2e2"
    "neutral"  "#f3f4f6"
    "#f3f4f6"))

(defn news-results-page [results error-msg]
  (str (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:title "News Results — CIM Portfolio Tool"]
          [:link {:rel "stylesheet" :href "/styles.css"}]
          [:style "
.results-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.article-card { background: #fff; border-radius: 8px; padding: 20px 24px; margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,.1); border-left: 5px solid #e5e7eb; }
.article-card.positive { border-left-color: #10b981; }
.article-card.negative { border-left-color: #ef4444; }
.article-card.neutral  { border-left-color: #6b7280; }
.article-card.failed   { border-left-color: #f59e0b; opacity: .8; }
.article-title { font-size: 1.1rem; font-weight: 700; margin: 0 0 6px; }
.article-title a { color: #1d4ed8; text-decoration: none; }
.article-title a:hover { text-decoration: underline; }
.article-meta { font-size: 0.82rem; color: #6b7280; margin-bottom: 12px; display: flex; gap: 14px; flex-wrap: wrap; }
.badge { display: inline-block; padding: 2px 10px; border-radius: 9999px; font-size: 0.78rem; font-weight: 600; }
.badge-positive { background: #d1fae5; color: #065f46; }
.badge-negative { background: #fee2e2; color: #991b1b; }
.badge-neutral  { background: #f3f4f6; color: #374151; }
.badge-failed   { background: #fef3c7; color: #92400e; }
.tldr { font-size: 0.95rem; font-style: italic; color: #374151; margin-bottom: 10px; border-left: 3px solid #d1d5db; padding-left: 12px; }
.summary { font-size: 0.93rem; color: #374151; line-height: 1.6; margin-bottom: 10px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.tag { background: #eff6ff; color: #1d4ed8; border-radius: 4px; padding: 2px 8px; font-size: 0.78rem; }
.entity-tag { background: #f5f3ff; color: #6d28d9; border-radius: 4px; padding: 2px 8px; font-size: 0.78rem; }
.stats-bar { display: flex; gap: 20px; flex-wrap: wrap; margin-bottom: 24px; }
.stat-box { background: #fff; border-radius: 8px; padding: 14px 20px; box-shadow: 0 1px 3px rgba(0,0,0,.08); text-align: center; }
.stat-box .num { font-size: 1.6rem; font-weight: 700; }
.stat-box .label { font-size: 0.8rem; color: #6b7280; }
.back-link { display: inline-block; margin-bottom: 20px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.back-link:hover { text-decoration: underline; }
.error-banner { background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 12px 16px; border-radius: 6px; }
"]]
         [:body
          (page-header [:a.nav-link {:href "/news"} "← New Analysis"] [:a.nav-link {:href "/"} "Portfolio"])
          [:div.container
           (if error-msg
             [:div.error-banner error-msg]
             (let [total      (count results)
                   successful (count (filter :success results))
                   pos        (count (filter #(= "Positive" (:sentiment %)) results))
                   neg        (count (filter #(= "Negative" (:sentiment %)) results))]
               [:div
                ;; Summary stats
                [:div.stats-bar
                 [:div.stat-box [:div.num total]      [:div.label "Articles"]]
                 [:div.stat-box [:div.num successful]  [:div.label "Analysed"]]
                 [:div.stat-box [:div.num pos]          [:div.label "Positive"]]
                 [:div.stat-box [:div.num neg]          [:div.label "Negative"]]]

                ;; Article cards
                (for [r results]
                  (let [sentiment-lc (str/lower-case (or (:sentiment r) "neutral"))
                        card-class   (if (:success r) sentiment-lc "failed")]
                    [:div.article-card {:class card-class}
                     [:h3.article-title
                      (if (:link r)
                        [:a {:href (:link r) :target "_blank" :rel "noopener"} (:title r)]
                        (:title r))]
                     [:div.article-meta
                      (when (:published r) [:span (:published r)])
                      (when (:category r)  [:span (:category r)])
                      (when (:bias r)      [:span (str "Bias: " (:bias r))])
                      (when (:target-audience r) [:span (str "For: " (:target-audience r))])]
                     [:span.badge {:class (str "badge-" (if (:success r) sentiment-lc "failed"))}
                      (or (:sentiment r) "Unknown")]
                     (when (and (:success r) (:tldr r))
                       [:p.tldr (:tldr r)])
                     (when (:summary r)
                       [:p.summary (:summary r)])
                     (when (seq (:keywords r))
                       [:div.tags (for [k (:keywords r)] [:span.tag k])])
                     (when (seq (:entities r))
                       [:div.tags (for [e (:entities r)] [:span.entity-tag e])])]))]))]]])))
