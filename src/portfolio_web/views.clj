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

;; The portfolio analysis page
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
          [:main.container

           ;; General Portfolio Summary
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
                                                                                     (* 100 (:one-year-cumulative-return-from-today (data :past-five-weeks-1y-cumulative-return))))]]]]

           ;; Cash Invested by Stock
           [:div.card
            [:h2 "Portfolio Allocation"]
            [:ul#cashByStock
             (for [ticker (data :unique-tickers)]
               [:li (str (format "%s: " ticker) (if (neg? (get (:values (data :current-stock-holdings)) ticker)) "-" "") ;; Add ticker name, and negative sign if value is negative 
                         (.format currency-formatter (abs (get (:values (data :current-stock-holdings)) ticker))) ;; Get the stock ho 
                         (format " (%.2f%%)" (* 100 (get (:weights (data :current-stock-holdings)) ticker))))])]]
           
           [:div.card
            [:h2 "1-Year Cumulative Portfolio Return"]
            [:ul#returnByWeek
            ;;  (for [[date value] (data :portfolio-returns-by-date)]
            ;;    [:li (str date ": " (format "%.2f%%" (* 100 (value :portfolio-cumulative-return))) 
            ;;              " | Beginning Value: " (.format currency-formatter (value :initial-portfolio-value))
            ;;              " | Ending Value: " (.format currency-formatter (value :current-portfolio-value)))]) 
             [:li (str (:one-week-ago (data :past-five-weeks-1y-cumulative-return)) ": " 
                       (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-one-week-ago (data :past-five-weeks-1y-cumulative-return)))))]
             [:li (str (:two-weeks-ago (data :past-five-weeks-1y-cumulative-return)) ": " 
                       (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-two-weeks-ago (data :past-five-weeks-1y-cumulative-return)))))]
             [:li (str (:three-weeks-ago (data :past-five-weeks-1y-cumulative-return)) ": " 
                       (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-three-weeks-ago (data :past-five-weeks-1y-cumulative-return)))))]
             [:li (str (:four-weeks-ago (data :past-five-weeks-1y-cumulative-return)) ": " 
                       (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-four-weeks-ago (data :past-five-weeks-1y-cumulative-return)))))]
             [:li (str (:five-weeks-ago (data :past-five-weeks-1y-cumulative-return)) ": " 
                       (format "%.2f%%" (* 100 (:one-year-cumulative-return-from-five-weeks-ago (data :past-five-weeks-1y-cumulative-return)))))]
             ]]
           
           ;; Old Alpha & Beta Design
           ;;  [:div.card
           ;;   [:h2 "Alpha & Beta (per stock)"]
           ;;   [:ul#alphaBeta
           ;;    [:li 
           ;;     [:canvas.alphaBetaChart]] ;; Make this part dynamic
           ;;    ]]
           
           ;; Other Graphs  
           
           [:div.card
            [:h2 "Portfolio Value by Day"]
            [:div.graph 
             [:div {:id "portfolio-value-by-day" :class "miscChart"
                    :data-plot (json/write-str (data :portfolio-value-figs))}]]
            ]

           [:div.card
            [:h2 "30-Day Rolling Annualized Volatility of Portfolio"]
            [:div.graph
             [:div {:id "rolling-annualized-volatility" :class "miscChart"
                    :data-plot (json/write-str (data :rolling-annualized-volatility-figs))}]]]

           ;; Alpha & Beta Graphs
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
                         :data-plot (json/write-str (some #(when (= (% :name) (str ticker " α")) %) (data :alpha-beta-figs)))
                         }]]
                 [:div.graph
                  [:h4.rolling-beta-header "Rolling Beta"]
                  [:div {:id (str ticker "-beta") :class "betaChart"
                         :data-plot (json/write-str (some #(when (= (% :name) (str ticker " β")) %) (data :alpha-beta-figs)))}]]]])
             ]]
           
           ;; Stock Performances
           [:div.card.full-width
            [:h2 "Stock Performances"]
            [:div#stockDetailsContainer
             (for [ticker (data :unique-tickers)]
               [:details.stockAccordion
                [:summary [:strong ticker]]
                [:div.graphRow
                 [:div.graph
                  [:h4.stock-performance-header "Stock Performance (Log(1+x) scale)"]
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
           {:src "/js/portfolio.js"}
           ]
          ]])))

(home-page)