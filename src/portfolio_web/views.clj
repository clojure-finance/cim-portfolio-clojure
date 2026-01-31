(ns portfolio-web.views
  (:require [hiccup2.core :as h]
            [clojure.data.json :as json])
  (:import  java.text.NumberFormat
            java.util.Locale
            java.util.Currency))

;; Test Page
(defn test-page [data]
  (str (data :test-data)))

;; Set Dollar Value Display Settings
(def currency-formatter
  (doto (NumberFormat/getCurrencyInstance Locale/US)
    (.setCurrency (Currency/getInstance "USD"))
    (.setMinimumFractionDigits 2)))

;; The default page body
(def html-home-body 
  [:body 
   [:header
    [:h1 "CIM Portfolio Analysis Tool"]
    [:a.github-link {:href "https://github.com/clojure-finance/cim-portfolio-clojure/tree/web-application" :target "_blank" :rel "noopener noreferrer" :aria-label "View project on GitHub"}
     [:svg.github-icon {:width "20" :height "20" :viewBox "0 0 98 96" :xmlns "http://www.w3.org/2000/svg" :aria-hidden "true" :focusable "false"}
      [:g {:clip-path "url(#clip0_730_27126)"}
       [:path {:d "M41.4395 69.3848C28.8066 67.8535 19.9062 58.7617 19.9062 46.9902C19.9062 42.2051 21.6289 37.0371 24.5 33.5918C23.2559 30.4336 23.4473 23.7344 24.8828 20.959C28.7109 20.4805 33.8789 22.4902 36.9414 25.2656C40.5781 24.1172 44.4062 23.543 49.0957 23.543C53.7852 23.543 57.6133 24.1172 61.0586 25.1699C64.0254 22.4902 69.2891 20.4805 73.1172 20.959C74.457 23.543 74.6484 30.2422 73.4043 33.4961C76.4668 37.1328 78.0937 42.0137 78.0937 46.9902C78.0937 58.7617 69.1934 67.6621 56.3691 69.2891C59.623 71.3945 61.8242 75.9883 61.8242 81.252L61.8242 91.2051C61.8242 94.0762 64.2168 95.7031 67.0879 94.5547C84.4102 87.9512 98 70.6289 98 49.1914C98 22.1074 75.9883 6.69539e-07 48.9043 4.309e-07C21.8203 1.92261e-07 -1.9479e-07 22.1074 -4.3343e-07 49.1914C-6.20631e-07 70.4375 13.4941 88.0469 31.6777 94.6504C34.2617 95.6074 36.75 93.8848 36.75 91.3008L36.75 83.6445C35.4102 84.2188 33.6875 84.6016 32.1562 84.6016C25.8398 84.6016 22.1074 81.1563 19.4277 74.7441C18.375 72.1602 17.2266 70.6289 15.0254 70.3418C13.877 70.2461 13.4941 69.7676 13.4941 69.1934C13.4941 68.0449 15.4082 67.1836 17.3223 67.1836C20.0977 67.1836 22.4902 68.9063 24.9785 72.4473C26.8926 75.2227 28.9023 76.4668 31.2949 76.4668C33.6875 76.4668 35.2187 75.6055 37.4199 73.4043C39.0469 71.7773 40.291 70.3418 41.4395 69.3848Z" :fill "currentColor"}]]
      [:defs [:clipPath {:id "clip0_730_27126"} [:rect {:width "98" :height "96" :fill "white"}]]]
     ] "View on GitHub"]]
    [:div.container
      [:h2 "Provide Your Trades"]

      ;; Radio buttons to choose input mode
      [:div.input-choice
      [:label
        [:input.input-radio-button {:type "radio" :name "input-mode" :value "manual" :checked true}]
        "Enter manually"]
      [:label
        [:input.input-radio-button {:type "radio" :name "input-mode" :value "file"}]
        "Upload CSV"]]

      ;; Form with both inputs available (multipart for file uploads)
      [:form {:method "post" :action "/analyze-portfolio" :enctype "multipart/form-data" :id "trades-submission-form"}
      ;; Manual entry textarea
      [:div.manual-input
        ;; Button and label to populate textarea with an example (from testPortfolio1.csv)
        [:div.example-controls
         [:label.example-label "Need an example? "]
         [:button.example-button {:type "button" :id "fill-example-button"} "Fill with example trades"]
         ;; Hidden pre containing example trades for JS to read
         [:pre.example-data.hidden 
          "2024-12-01,buy,55000,NVDA\n2024-12-01,buy,29600,GOOG\n2024-12-01,buy,17500,MSFT\n2024-12-01,buy,23000,AAPL\n2024-12-01,buy,17400,AMZN\n2024-12-01,buy,12900,META\n2025-06-30,sell,5500,NVDA\n2025-06-30,buy,11700,TSLA"]]

        [:textarea {:name "trades" :rows "5"
                    :placeholder "YYYY-MM-DD,action,amount,ticker"}]]

      ;; File upload input (Accepts .csv or .txt)
      [:div.file-input
        [:input {:type "file" :name "trades-file" :accept ".csv,.txt"}]]

      ;; Starting cash
      [:label.label-h3 {:for "starting-cash"} "Starting Cash"]
      [:input {:type "text" :id "starting-cash-input" :name "starting-cash" :value "50000000"}]

      ;; Show CAPM metrics checkbox
      [:div.checkbox-container
        [:label
          [:input {:type "checkbox" :name "show-capm-metrics" :value "true"}]
          "Show CAPM Alpha and Beta of Stocks"]
        [:label
          [:input {:type "checkbox" :name "show-stock-performances" :value "true"}]
          "Show Individual Stock Performances"]]

      [:button {:type "submit" :id "submit-trades-button"} "Analyze Portfolio"]]
      
      [:script {:src "/js/home_page.js"}]
     ]]
        

  ;;  [:div.container
  ;;   ;; [:h2 "Upload Your Trades"] 
  ;;   ;; [:input {:type "file" :accept ".csv, .txt"}]
  ;;   [:h2 "Enter Your Trades"]
  ;;   [:form {:method "post" :action "/analyze-portfolio"}
  ;;    [:textarea {:name "trades" :rows "5" :placeholder "YYYY-MM-DD,action,amount,ticker"}]
  ;;    [:label.label-h3 {:for "starting-cash"} "Starting Cash"]
  ;;    [:input {:type "number"
  ;;             :name "starting-cash"
  ;;             :value "50000000"}]
  ;;    [:button {:type "submit"} "Analyze Portfolio"]]
  ;;   ]]
  )

;; The default page
(defn home-page []
  (str (h/html
        [:html {:lang "en"} 
         [:head 
          [:meta {:charset "UTF-8"}]
          [:title "CIM Portfolio Analysis Tool"]
          [:link {:rel "stylesheet" :href "/styles.css"}]]
         html-home-body])))

;; Portfolio summary section (top cards)
(defn portfolio-summary-section [data]
   ;; General Portfolio Summary
  [:div#summary.container
   [:div.card
    [:h2 "Portfolio Summary"]
    [:div.metrics 
     [:div.metric 
      [:span "Current Portfolio Value:"] [:span#portfolioValue.num
                                          (.format currency-formatter (data :current-portfolio-value))]]
     [:div.metric
      [:span "Cash in Portfolio:"] [:span#cashValue.num (.format currency-formatter (data :cash))]]
     [:div.metric
      [:span "Stock Holdings Value:"] [:span#stockValue.num (.format currency-formatter (data :stocks))]]]]
   
   ;; Portfolio Performance Metrics
   [:div.card
    [:h2 "Performance Metrics"]
    [:div.metrics
     [:div.metric
      [:span "Annualized Return of Portfolio:"] [:span#annualReturn (format "%.2f%%" (data :annualized-portfolio-return))]]
     [:div.metric
      [:span "Annualized Volatility of Portfolio:"] [:span#annualVolatility (format "%.2f%%" (data :annualized-portfolio-volatility))]]
     [:div.metric
      [:span "1-Year Cumulative Portfolio Return:"] [:span#cumulativeReturn (format "%.2f%%"
                                                                                    (* 100 (:one-year-cumulative-return-from-today (data :past-five-weeks-1y-cumulative-return-incl-cash))))]]]]
   
   ;; Cash Invested by Stock
   [:div.card
    [:h2 "Portfolio Allocation"]
    [:ul#cashByStock
     (for [ticker (data :unique-tickers)]
       [:li (str (format "%s: " ticker) (if (neg? (get (:values (data :current-stock-holdings-and-weights)) ticker)) "-" "")
                 (.format currency-formatter (abs (get (:values (data :current-stock-holdings-and-weights)) ticker)))
                 (format " (%.2f%%)" (* 100 (get (:weights (data :current-stock-holdings-and-weights)) ticker))))])]]
   
   [:div.card.return-comparison-card
    [:h2 "1-Year Cumulative Portfolio Return"]
    [:div.return-comparison
     ;; Incl. Cash Column
     [:div.return-column
      [:h3 "Including Cash"]
      [:ul#returnByWeekIncl
       [:li (str (:one-year-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - "
                 (:today (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-today (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]
       [:li (str (:one-year-from-one-week-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - " 
                 (:one-week-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-one-week-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]
       [:li (str (:one-year-from-two-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - "
                 (:two-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-two-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]
       [:li (str (:one-year-from-three-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - "
                 (:three-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-three-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]
       [:li (str (:one-year-from-four-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - "
                 (:four-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-four-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]
       [:li (str (:one-year-from-five-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) " - "
                 (:five-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-five-weeks-ago (data :past-five-weeks-1y-cumulative-return-incl-cash)))))]]]
     ;; Excl. Cash Column
     [:div.return-column
      [:h3 "Excluding Cash"]
      [:ul#returnByWeekExcl
       [:li (str (:one-year-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:today (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-today (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]
       [:li (str (:one-year-from-one-week-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:one-week-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-one-week-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]
       [:li (str (:one-year-from-two-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:two-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-two-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]
       [:li (str (:one-year-from-three-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:three-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-three-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]
       [:li (str (:one-year-from-four-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:four-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-four-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]
       [:li (str (:one-year-from-five-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) " - "
                 (:five-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)) ": "
                 (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-five-weeks-ago (data :past-five-weeks-1y-cumulative-return-excl-cash)))))]]]]]
   ]
   )

;; Portfolio analytics section (charts and visualizations)
(defn portfolio-analytics-section [data]
  [:div#analytics.container
   [:div.card.full-width 
    [:h2 "Portfolio Value by Day"]
    [:div.graph
     [:div {:id "portfolio-value-by-day" :class "miscChart"
            :data-plot (json/write-str (data :portfolio-value-figs))}]]]
   
   [:div.card.full-width 
    [:h2 "One Dollar Invested in Portfolio at Time Zero (Log(x) scale)"]
    [:div.graph 
     [:div {:id "portfolio-performance-by-day" :class "miscChart"
            :data-plot (json/write-str (data :portfolio-one-dollar-performance-graph))}]]]
   
   [:div.card.full-width
    [:h2 "30-Day Annualized EWMA Rolling Volatility of Portfolio (λ = 0.94)"]
    [:div.graph
     [:div {:id "rolling-ewma-volatility" :class "miscChart"
            :data-plot (json/write-str (data :default-rolling-ewma-volatility-figs))
            :data-alt-plot (json/write-str (data :alternative-rolling-ewma-volatility-figs))}]]
    [:button {:id "lambdaSwitch"} "Switch to λ = 0.97"]]  ;; This button is a temporary feature (before user input) to change lambda to 0.97 (via Javascript)
   
   
   ;; Alpha & Beta Graphs (Conidtional on whether :show-capm-metrics = true)
   (if (= (data :alpha-beta-figs) "")
     ;; :show-capm-metrics = false
     [:div.card.full-width
      [:h2 "Alpha & Beta (per stock) (Omitted)"]]

     ;; :show-capm-metrics = true
     [:div.card.full-width
      [:h2 "Alpha & Beta (per stock)"]
      [:div#alphaBetaContainer
       (for [ticker (data :unique-tickers)]
         [:details.stockAccordion
          [:summary [:strong ticker]]
          [:div.graphRow
           [:div.graph
            [:h4.rolling-alpha-header "Rolling Alpha"]
            [:div {:id (str ticker "-alpha") :class "alphaChart"
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " α")) %) (data :alpha-beta-figs)))}]]
           [:div.graph
            [:h4.rolling-beta-header "Rolling Beta"]
            [:div {:id (str ticker "-beta") :class "betaChart"
                   :data-plot (json/write-str (some #(when (= (% :name) (str ticker " β")) %) (data :alpha-beta-figs)))}]]]])]]
     )
   
   ;; Stock Performances
   (if (= (data :stock-performances-graphs) "")
     ;; :show-stock-performances = false
     [:div.card.full-width
      [:h2 "Stock Performances (Omitted)"]]
      
     ;; :show-stock-performances = true
     [:div.card.full-width
      [:h2 "Stock Performances"]
      [:div#stockDetailsContainer
       (for [ticker (data :unique-tickers)]
         [:details.stockAccordion
          [:summary [:strong ticker]]
          [:div.graphRow
           [:div.graph
            [:h4.stock-performance-header "One Dollar Invested at Time Zero (Log(x) scale)"]
            [:div {:id (str ticker "-performance") :class "performanceChart"
                   :data-plot (json/write-str (get (data :stock-performances-graphs) ticker))}]]
           ;; Wait for Tanvi's research for this part
           ;;  [:div.stockMetrics
           ;;   [:h4.stock-performance-header "Key Metrics"]
           ;;   [:ul#stockMetricList
           ;;    ;; Will have to replace the following with real data, if we can get them consistently somewhere (ask Dr. B)
           ;;    [:li#EPS (str "EPS: 1.0")]
           ;;    [:li#PERatio (str "P/E: 1.0")]
           ;;    [:li#Sharpe (str "Sharpe: 1.0")]
           ;;    ;;  [:tr [:td "EPS"]    [:td (get (data :eps) ticker)]]
           ;;    ;;  [:tr [:td "P/E"]    [:td (get (data :pe) ticker)]]
           ;;    ;;  [:tr [:td "Sharpe"] [:td (get (data :sharpe) ticker)]] 
           ;;    ]]
           ]])]]
     ) 
   ] 
  )

(defn portfolio-page [data]
  (str (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "UTF-8"}]
          [:title "CIM Portfolio Analysis Tool"]
          [:link {:rel "stylesheet" :href "/portfolio_styles.css"}]
          [:script {:src "https://cdn.plot.ly/plotly-2.32.0.min.js"}]]

         [:body
          [:header
           [:h1 "CIM Portfolio Analysis Tool"]
           [:a.github-link {:href "https://github.com/clojure-finance/cim-portfolio-clojure/tree/web-application" :target "_blank" :rel "noopener noreferrer" :aria-label "View project on GitHub"}
            [:svg.github-icon {:width "20" :height "20" :viewBox "0 0 98 96" :xmlns "http://www.w3.org/2000/svg" :aria-hidden "true" :focusable "false"}
             [:g {:clip-path "url(#clip0_730_27126)"}
              [:path {:d "M41.4395 69.3848C28.8066 67.8535 19.9062 58.7617 19.9062 46.9902C19.9062 42.2051 21.6289 37.0371 24.5 33.5918C23.2559 30.4336 23.4473 23.7344 24.8828 20.959C28.7109 20.4805 33.8789 22.4902 36.9414 25.2656C40.5781 24.1172 44.4062 23.543 49.0957 23.543C53.7852 23.543 57.6133 24.1172 61.0586 25.1699C64.0254 22.4902 69.2891 20.4805 73.1172 20.959C74.457 23.543 74.6484 30.2422 73.4043 33.4961C76.4668 37.1328 78.0937 42.0137 78.0937 46.9902C78.0937 58.7617 69.1934 67.6621 56.3691 69.2891C59.623 71.3945 61.8242 75.9883 61.8242 81.252L61.8242 91.2051C61.8242 94.0762 64.2168 95.7031 67.0879 94.5547C84.4102 87.9512 98 70.6289 98 49.1914C98 22.1074 75.9883 6.69539e-07 48.9043 4.309e-07C21.8203 1.92261e-07 -1.9479e-07 22.1074 -4.3343e-07 49.1914C-6.20631e-07 70.4375 13.4941 88.0469 31.6777 94.6504C34.2617 95.6074 36.75 93.8848 36.75 91.3008L36.75 83.6445C35.4102 84.2188 33.6875 84.6016 32.1562 84.6016C25.8398 84.6016 22.1074 81.1563 19.4277 74.7441C18.375 72.1602 17.2266 70.6289 15.0254 70.3418C13.877 70.2461 13.4941 69.7676 13.4941 69.1934C13.4941 68.0449 15.4082 67.1836 17.3223 67.1836C20.0977 67.1836 22.4902 68.9063 24.9785 72.4473C26.8926 75.2227 28.9023 76.4668 31.2949 76.4668C33.6875 76.4668 35.2187 75.6055 37.4199 73.4043C39.0469 71.7773 40.291 70.3418 41.4395 69.3848Z" :fill "currentColor"}]]
             [:defs [:clipPath {:id "clip0_730_27126"} [:rect {:width "98" :height "96" :fill "white"}]]]
            ] "View on GitHub"]]
          [:main
           (portfolio-summary-section data)
           (portfolio-analytics-section data)
           ]
          [:script
           ;; (str
           ;;  "document.addEventListener('DOMContentLoaded', function() {"
           ;;  (apply str (
           ;;              for [ticker (data :unique-tickers)]
           ;;              (str
           ;;                "var " ticker "Alpha = " (json/write-str (some #(when (= (% :name) (str ticker " α"))
           ;;                                                                  %) (data :alpha-beta-figs))) ";"
           ;;                "var " ticker "Beta = " (json/write-str (some #(when (= (% :name) (str ticker " β"))
           ;;                                                                 %) (data :alpha-beta-figs))) ";"
           ;;                "Plotly.newPlot('" ticker "-alpha', " ticker "Alpha);"
           ;;                "Plotly.newPlot('" ticker "-beta', " ticker "Beta);")))
           ;;  "});"
           ;;  )
           {:src "/js/portfolio.js"}]]])))

(home-page)