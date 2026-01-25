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
    [:h1 "CIM Portfolio Analysis Tool"]]
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
      [:form {:method "post" :action "/analyze-portfolio" :enctype "multipart/form-data"}
      ;; Manual entry textarea
      [:div.manual-input
        [:textarea {:name "trades" :rows "5"
                    :placeholder "YYYY-MM-DD,action,amount,ticker"}]]

      ;; File upload input (Accepts .csv or .txt)
      [:div.file-input
        [:input {:type "file" :name "trades-file" :accept ".csv,.txt"}]]

      ;; Starting cash
      [:label.label-h3 {:for "starting-cash"} "Starting Cash"]
      [:input {:type "number" :name "starting-cash" :value "50000000"}]

      ;; Show CAPM metrics checkbox
      [:div.checkbox-container
        [:label
          [:input {:type "checkbox" :name "show-capm-metrics" :value "true"}]
          "Show CAPM Alpha and Beta of Stocks"]
        [:label
          [:input {:type "checkbox" :name "show-stock-performances" :value "true"}]
          "Show Individual Stock Performances"]]

      [:button {:type "submit"} "Analyze Portfolio"]]
      
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
       [:li (str (format "%s: " ticker) (if (neg? (get (:values (data :current-stock-holdings)) ticker)) "-" "")
                 (.format currency-formatter (abs (get (:values (data :current-stock-holdings)) ticker)))
                 (format " (%.2f%%)" (* 100 (get (:weights (data :current-stock-holdings)) ticker))))])]]
   
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
           [:h1 "CIM Portfolio Analysis Tool"]]
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