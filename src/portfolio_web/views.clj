(ns portfolio-web.views
  (:require [hiccup.core :as h]
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
    [:h2 "30-Day Annualized EWMA Rolling Volatility of Portfolio (lambda = 0.94)"]
    [:div.graph [:div {:id "rolling-ewma-volatility" :class "miscChart"
                       :data-plot (json/write-str (data :default-rolling-ewma-volatility-figs))
                       :data-alt-plot (json/write-str (data :alternative-rolling-ewma-volatility-figs))}]]
    [:button {:id "lambdaSwitch"} "Switch to lambda = 0.97"]]

   [:div.card.full-width
    [:h2 "30-Day Annualized Rolling Sharpe Ratio (EWMA lambda = 0.94)"]
    [:div.graph [:div {:id "rolling-sharpe-ratio" :class "miscChart"
                       :data-plot (json/write-str (data :default-rolling-sharpe-ratio-figs))
                       :data-alt-plot (json/write-str (data :alternative-rolling-sharpe-ratio-figs))}]]
    [:button {:id "sharpeLambdaSwitch"} "Switch to lambda = 0.97"]]

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
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " a")) %) (data :alpha-beta-figs)))}]]
           [:div.graph [:h4.rolling-beta-header "Rolling Beta"]
            [:div {:id (str ticker "-beta") :class "betaChart"
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " b")) %) (data :alpha-beta-figs)))}]]]])]])

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
           [:a.back-button {:href "/"} "<- Back to Home"]
           [:a.nav-link {:href "/news"} "News Analysis"])
          [:main
           (portfolio-summary-section data)
           (portfolio-analytics-section data)]
          [:script {:src "/js/portfolio.js"}]]])))

;; ─── News Analysis pages ──────────────────────────────────────────────────────

(defn- stance-badge-class [stance]
  (case (str/lower-case (or stance ""))
    "bullish" "stance-bullish"
    "bearish" "stance-bearish"
    "stance-neutral"))

(defn- risk-badge-class [risk]
  (case (str/lower-case (or risk ""))
    "high"   "risk-high"
    "medium" "risk-medium"
    "low"    "risk-low"
    "risk-medium"))

(defn- time-badge-class [ts]
  (case (str/lower-case (or ts ""))
    "breaking"  "time-breaking"
    "recent"    "time-recent"
    "evergreen" "time-evergreen"
    "time-recent"))

(defn news-page
  ([] (news-page nil))
  ([error-msg]
   (str (h/html
         [:html {:lang "en"}
          [:head
           [:meta {:charset "UTF-8"}]
           [:title "AI News Intelligence -- CIM Portfolio Tool"]
           [:link {:rel "stylesheet" :href "/styles.css"}]]
          [:body
           (page-header [:a.nav-link {:href "/"} "Portfolio Analysis"])

           ;; Hero section -- full width, outside the white container
           [:section.news-hero
            [:h2 "AI News Intelligence"]
            [:p.hero-subtitle "Real-time financial news analysis powered by large language models"]]

           [:div.container
            (when error-msg [:div.news-error error-msg])

            [:form {:method "post" :action "/analyze-news" :id "news-form"
                    :onsubmit "document.getElementById('news-loading').style.display='block';document.getElementById('news-submit-btn').disabled=true;"}

             ;; Provider cards
             [:div.news-section
              [:div.news-section-header "LLM Provider"]
              [:div.provider-cards
               [:label.provider-card.active
                {:id "card-deepseek"
                 :onclick "document.getElementById('card-deepseek').classList.add('active');document.getElementById('card-openrouter').classList.remove('active');"}
                [:input {:type "radio" :name "llm-provider" :value "deepseek" :checked true}]
                [:div
                 [:div.provider-card-name "DeepSeek"]
                 [:div.provider-card-desc "High-performance reasoning, direct API"]]]
               [:label.provider-card
                {:id "card-openrouter"
                 :onclick "document.getElementById('card-openrouter').classList.add('active');document.getElementById('card-deepseek').classList.remove('active');"}
                [:input {:type "radio" :name "llm-provider" :value "openrouter"}]
                [:div
                 [:div.provider-card-name "OpenRouter"]
                 [:div.provider-card-desc "100+ models including free tiers"]]]]]

             ;; API keys
             [:div.news-section
              [:div.news-section-header "API Credentials"]
              [:div.field-group
               [:label "Newsdata.io API Key"]
               [:input {:type "password" :name "newsdata-api-key" :required true :placeholder "pub_..."}]
               [:div.field-hint "Free key at newsdata.io -- 200 requests/day"]]
              [:div.field-group
               [:label "LLM API Key"]
               [:input {:type "password" :name "llm-api-key" :required true :placeholder "sk-..."}]
               [:div.field-hint "OpenRouter: openrouter.ai  |  DeepSeek: platform.deepseek.com"]]]

             ;; Model selection
             [:div.news-section
              [:div.news-section-header "Model"]
              [:div.field-group
               [:label "Select Model"]
               [:select {:name "model"}
                [:optgroup {:label "DeepSeek (direct)"}
                 (for [m news/deepseek-models] [:option {:value m} m])]
                [:optgroup {:label "OpenRouter (free)"}
                 (for [m news/free-models] [:option {:value m} m])]]]]

             ;; Search parameters
             [:div.news-section
              [:div.news-section-header "Search Parameters"]
              [:div.field-group
               [:label "Search Query"]
               [:input {:type "text" :name "query"
                        :placeholder "e.g. AAPL earnings, Federal Reserve, AI stocks"}]
               [:div.field-hint "Company names, tickers, or topics -- leave blank for top headlines"]]
              [:div.field-row-2
               [:div.field-group
                [:label "Country Code"]
                [:input {:type "text" :name "country" :value "us" :placeholder "us"}]
                [:div.field-hint "ISO country code"]]
               [:div.field-group
                [:label "Language Code"]
                [:input {:type "text" :name "language" :value "en" :placeholder "en"}]
                [:div.field-hint "ISO language code"]]]
              [:div.field-group
               [:label "Max Articles (1-10)"]
               [:input {:type "number" :name "max-articles" :value "3" :min "1" :max "10"}]]]

             ;; Submit
             [:div.news-submit-section
              [:button#news-submit-btn.news-submit-btn {:type "submit"} "Run AI Analysis"]
              [:p#news-loading.news-loading
               "Analyzing articles... this may take 1-3 minutes. Please wait."]]]]]]))))

(defn news-results-page [results error-msg]
  (str (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:title "News Intelligence Results -- CIM Portfolio Tool"]
          [:link {:rel "stylesheet" :href "/styles.css"}]]
         [:body
          (page-header [:a.nav-link {:href "/news"} "News Analysis"] [:a.nav-link {:href "/"} "Portfolio"])
          [:div.container
           (if error-msg
             [:div.news-error error-msg]
             (let [total      (count results)
                   successful (count (filter :success results))
                   pos        (count (filter #(= "Positive" (:sentiment %)) results))
                   neg        (count (filter #(= "Negative" (:sentiment %)) results))
                   neu        (count (filter #(= "Neutral"  (:sentiment %)) results))]
               [:div
                [:a.results-back {:href "/news"} "<- Run New Analysis"]
                [:h2.results-page-title "Analysis Complete"]
                [:p.results-page-subtitle
                 (str total " articles fetched -- " successful " successfully analyzed by LLM")]

                ;; 5-stat dashboard
                [:div.dashboard-grid
                 [:div.dash-stat.d-total    [:div.dash-stat-num total]     [:div.dash-stat-label "Articles"]]
                 [:div.dash-stat.d-analyzed [:div.dash-stat-num successful] [:div.dash-stat-label "Analyzed"]]
                 [:div.dash-stat.d-positive [:div.dash-stat-num pos]        [:div.dash-stat-label "Positive"]]
                 [:div.dash-stat.d-negative [:div.dash-stat-num neg]        [:div.dash-stat-label "Negative"]]
                 [:div.dash-stat.d-neutral  [:div.dash-stat-num neu]        [:div.dash-stat-label "Neutral"]]]

                ;; Sentiment distribution bars
                (when (pos? successful)
                  [:div.sentiment-distribution-card
                   [:h4 "Sentiment Distribution"]
                   [:div.sentiment-dist-row
                    [:span.sentiment-dist-label "Positive"]
                    [:div.sentiment-dist-bar
                     [:div.sentiment-dist-fill-pos
                      {:style (str "width:" (int (* 100 (/ pos (max 1 total)))) "%")}]]
                    [:span.sentiment-dist-count pos]]
                   [:div.sentiment-dist-row
                    [:span.sentiment-dist-label "Negative"]
                    [:div.sentiment-dist-bar
                     [:div.sentiment-dist-fill-neg
                      {:style (str "width:" (int (* 100 (/ neg (max 1 total)))) "%")}]]
                    [:span.sentiment-dist-count neg]]
                   [:div.sentiment-dist-row
                    [:span.sentiment-dist-label "Neutral"]
                    [:div.sentiment-dist-bar
                     [:div.sentiment-dist-fill-neu
                      {:style (str "width:" (int (* 100 (/ neu (max 1 total)))) "%")}]]
                    [:span.sentiment-dist-count neu]]])

                ;; Article cards
                (for [r results]
                  (let [sent-lc   (str/lower-case (or (:sentiment r) "neutral"))
                        hdr-class (if (:success r) sent-lc "failed")
                        impact    (or (:market-impact r) 0)
                        stance    (:investment-stance r)
                        risk      (:risk-level r)
                        time-sens (:time-sensitivity r)
                        sectors   (or (:affected-sectors r) [])
                        key-quote (:key-quote r)
                        insight   (:actionable-insight r)]
                    [:div.news-article-card

                     ;; Colored header
                     [:div.article-card-header {:class hdr-class}
                      [:div.article-header-left
                       [:h3.article-header-title
                        (if (:link r)
                          [:a {:href (:link r) :target "_blank" :rel "noopener"} (:title r)]
                          (:title r))]
                       [:div.article-header-meta
                        (when (:published r)       [:span (:published r)])
                        (when (:category r)        [:span (:category r)])
                        (when (seq time-sens)
                          [:span.time-badge {:class (time-badge-class time-sens)} time-sens])
                        (when (:bias r)            [:span (str "Bias: " (:bias r))])
                        (when (:target-audience r) [:span (str "For: " (:target-audience r))])]]
                      [:div.article-header-badges
                       [:span.sentiment-badge {:class (if (:success r) sent-lc "failed")}
                        (or (:sentiment r) "Unknown")]
                       (when (seq stance)
                         [:span.stance-badge {:class (stance-badge-class stance)}
                          (case (str/lower-case stance)
                            "bullish" "Bullish"
                            "bearish" "Bearish"
                            stance)])
                       (when (seq risk)
                         [:span.risk-badge {:class (risk-badge-class risk)}
                          (str "Risk: " risk)])]]

                     ;; Body
                     [:div.article-card-body

                      ;; TL;DR callout
                      (when (and (:success r) (seq (:tldr r)))
                        [:div.tldr-callout (:tldr r)])

                      ;; Summary
                      (when (seq (:summary r))
                        [:p.summary-text (:summary r)])

                      ;; Market impact bar
                      (when (and (:success r) (number? impact) (pos? impact))
                        [:div.impact-row
                         [:span.impact-label "Market Impact"]
                         [:div.impact-bar-track
                          [:div.impact-bar-fill {:style (str "width:" (* impact 10) "%")}]]
                         [:span.impact-score (str impact " / 10")]])

                      ;; Key quote
                      (when (and (:success r) (seq key-quote))
                        [:div.key-quote-block
                         [:div.key-quote-label "Key Quote"]
                         [:div.key-quote-text (str "\"" key-quote "\"")]])

                      ;; Actionable insight
                      (when (and (:success r) (seq insight))
                        [:div.insight-block
                         [:div.insight-label "Actionable Insight"]
                         [:div.insight-text insight]])

                      ;; Affected sectors
                      (when (seq sectors)
                        [:div.sectors-row
                         [:span.sectors-label "Sectors:"]
                         (for [s sectors] [:span.sector-chip s])])

                      ;; Keywords
                      (when (seq (:keywords r))
                        [:div.tags-row
                         [:span.tags-label "Keywords:"]
                         (for [k (:keywords r)] [:span.kw-tag k])])

                      ;; Entities
                      (when (seq (:entities r))
                        [:div.tags-row
                         [:span.tags-label "Entities:"]
                         (for [e (:entities r)] [:span.ent-tag e])])]

                     ;; Footer
                     [:div.article-card-footer
                      (if (seq (:published r))
                        [:span (str "Published: " (:published r))]
                        [:span ""])
                      (when (:link r)
                        [:a {:href (:link r) :target "_blank" :rel "noopener"}
                         "Read Full Article ->"])]]))]))]]])))
