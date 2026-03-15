(ns cim-portfolio.news.server
  (:require [cim-portfolio.news.core :as core]
            [org.httpkit.server :as http]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.form :as form]
            [hiccup.page :refer [html5]]
            [clojure.string :as str]))

;; ──────────────────────────────────────────────────────────────
;; CSS
;; ──────────────────────────────────────────────────────────────
(def app-css
  "
  :root {
    --bg: #0f172a; --surface: #1e293b; --card: #334155; --border: #475569;
    --text: #e2e8f0; --text-dim: #94a3b8; --accent: #38bdf8; --green: #4ade80;
    --red: #f87171; --yellow: #fbbf24; --purple: #a78bfa; --grad-start: #06b6d4; --grad-end: #8b5cf6;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }

  /* Header */
  .header { background: linear-gradient(135deg, var(--grad-start), var(--grad-end)); padding: 28px 0; text-align: center; }
  .header h1 { font-size: 1.8rem; font-weight: 700; letter-spacing: .5px; }
  .header p { color: rgba(255,255,255,.8); font-size: .85rem; margin-top: 4px; }

  .container { max-width: 1100px; margin: 0 auto; padding: 24px 16px; }

  /* Form card */
  .form-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 24px; margin-bottom: 28px; }
  .form-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
  .field label { display: block; font-size: .78rem; text-transform: uppercase; letter-spacing: .8px; color: var(--text-dim); margin-bottom: 6px; }
  .field input, .field select { width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: .9rem; transition: border .2s; }
  .field input:focus, .field select:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px rgba(56,189,248,.15); }
  .section-title { font-size: .85rem; font-weight: 600; color: var(--accent); margin: 20px 0 10px; padding-bottom: 6px; border-bottom: 1px solid var(--border); }
  .btn { display: inline-block; padding: 12px 32px; border-radius: 8px; border: none; font-weight: 600; cursor: pointer; font-size: .95rem; transition: all .2s; }
  .btn-primary { background: linear-gradient(135deg, var(--grad-start), var(--grad-end)); color: #fff; margin-top: 20px; }
  .btn-primary:hover { opacity: .9; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(56,189,248,.3); }
  .loading { display: none; color: var(--accent); margin-top: 12px; font-size: .85rem; }
  .loading::before { content: '⏳ '; }

  /* Stats bar */
  .stats-bar { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
  .stat { flex: 1; min-width: 140px; background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 16px; text-align: center; }
  .stat .val { font-size: 1.8rem; font-weight: 700; }
  .stat .lbl { font-size: .72rem; text-transform: uppercase; letter-spacing: .8px; color: var(--text-dim); margin-top: 2px; }

  /* Article card */
  .article { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-bottom: 20px; transition: transform .2s; }
  .article:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,.3); }
  .article-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
  .article-header h3 { font-size: 1rem; flex: 1; }
  .badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: .72rem; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; }
  .badge-green { background: rgba(74,222,128,.15); color: var(--green); }
  .badge-red { background: rgba(248,113,113,.15); color: var(--red); }
  .badge-yellow { background: rgba(251,191,36,.15); color: var(--yellow); }
  .badge-purple { background: rgba(167,139,250,.15); color: var(--purple); }
  .badge-blue { background: rgba(56,189,248,.15); color: var(--accent); }

  .article-body { padding: 0 20px 20px; }

  /* Score gauge */
  .scores { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
  .score-item { flex: 1; min-width: 90px; background: var(--card); border-radius: 8px; padding: 10px; text-align: center; }
  .score-item .s-val { font-size: 1.3rem; font-weight: 700; }
  .score-item .s-lbl { font-size: .68rem; text-transform: uppercase; color: var(--text-dim); }
  .bar-bg { height: 5px; background: var(--bg); border-radius: 3px; margin-top: 4px; overflow: hidden; }
  .bar-fill { height: 100%; border-radius: 3px; transition: width .4s; }

  /* Tags */
  .tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0; }
  .tag { padding: 2px 8px; border-radius: 4px; font-size: .72rem; background: rgba(56,189,248,.1); color: var(--accent); border: 1px solid rgba(56,189,248,.2); }

  .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: .85rem; }
  .meta-grid dt { color: var(--text-dim); }
  .meta-grid dd { font-weight: 500; }

  .quotes { border-left: 3px solid var(--purple); padding: 8px 14px; margin: 10px 0; background: rgba(167,139,250,.05); border-radius: 0 6px 6px 0; font-style: italic; font-size: .85rem; color: var(--text-dim); }

  .risk-list { list-style: none; }
  .risk-list li::before { content: '⚠ '; color: var(--yellow); }
  .risk-list li { font-size: .85rem; margin: 2px 0; }

  .back-link { display: inline-block; margin-top: 20px; color: var(--accent); text-decoration: none; font-weight: 500; }
  .back-link:hover { text-decoration: underline; }

  .error-box { background: rgba(248,113,113,.1); border: 1px solid var(--red); border-radius: 10px; padding: 20px; text-align: center; color: var(--red); }

  @media (max-width: 600px) {
    .form-grid { grid-template-columns: 1fr; }
    .scores { flex-direction: column; }
  }
  ")

;; ──────────────────────────────────────────────────────────────
;; Layout
;; ──────────────────────────────────────────────────────────────
(defn layout [title & content]
  (html5
   [:head
    [:title (str title " | CIM Portfolio News")]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:script {:src "https://unpkg.com/htmx.org@1.9.10"}]
    [:style app-css]]
   [:body
    [:div.header
     [:h1 "CIM Portfolio — Financial News Analyzer"]
     [:p "AI-Powered Sentiment Analysis & Market Intelligence"]]
    [:div.container content]
    [:script "
     document.querySelectorAll('form').forEach(f => {
       f.addEventListener('submit', function(){ 
         var l = document.querySelector('.loading');
         if(l) l.style.display='block';
         var b = f.querySelector('.btn-primary');
         if(b){ b.disabled=true; b.textContent='Analyzing…'; }
       });
     });
     "]]))

;; ──────────────────────────────────────────────────────────────
;; Home page
;; ──────────────────────────────────────────────────────────────
(defn home-page []
  (layout "Home"
    [:div.form-card
     [:form {:method "post" :action "/analyze"
             :hx-post "/analyze" 
             :hx-target "#results" 
             :hx-indicator ".loading"
             :hx-swap "innerHTML"}
       ;; Primary params
       [:div.section-title "Search Parameters"]
       [:div.form-grid
        [:div.field
         [:label "Stock Symbol / Query"]
         (form/text-field {:placeholder "AAPL, Tesla, Crypto…"} "query")]
        [:div.field
         [:label "Country"]
         (form/drop-down "country"
           [["United States" "us"] ["United Kingdom" "gb"] ["China" "cn"]
            ["Japan" "jp"] ["Germany" "de"] ["France" "fr"]
            ["India" "in"] ["Canada" "ca"] ["Australia" "au"]]
           "us")]
        [:div.field
         [:label "Language"]
         (form/drop-down "language"
           [["English" "en"] ["Chinese" "zh"] ["Japanese" "ja"]
            ["German" "de"] ["French" "fr"] ["Spanish" "es"]]
           "en")]
        [:div.field
         [:label "Max Articles"]
         [:input {:type "number" :name "max-articles" :value "3" :min "1" :max "10"}]]]

       ;; Advanced params
       [:div.section-title "Advanced Settings"]
       [:div.form-grid
        [:div.field
         [:label "LLM Model"]
         (form/drop-down "model" core/free-models (first core/free-models))]
        [:div.field
         [:label "Analysis Depth"]
         (form/drop-down "depth"
           [["Standard" "standard"] ["Quick" "quick"] ["Deep" "deep"]]
           "standard")]
        [:div.field
         [:label "Output Language"]
         (form/drop-down "output-lang"
           [["English" "English"] ["Chinese" "Chinese"] ["Japanese" "Japanese"]
            ["German" "German"] ["Match Article" "Same as article"]]
           "English")]
        [:div.field
         [:label "Similarity Threshold"]
         [:input {:type "number" :name "sim-threshold" :value "0.7" :min "0" :max "1" :step "0.05"}]]]

       [:button.btn.btn-primary {:type "submit"} "Run Analysis"]
       [:div.loading "Fetching news & running AI analysis… this may take 30-60 seconds."]]]
    
    [:div#results]))

;; ──────────────────────────────────────────────────────────────
;; Helpers
;; ──────────────────────────────────────────────────────────────
(defn score-color [score]
  (cond (>= score 8) "var(--green)"
        (>= score 5) "var(--accent)"
        (>= score 3) "var(--yellow)"
        :else        "var(--red)"))

(defn sentiment-badge [s]
  (let [cls (cond
              (str/includes? (str/lower-case (str s)) "positive") "badge-green"
              (str/includes? (str/lower-case (str s)) "negative") "badge-red"
              :else "badge-yellow")]
    [:span.badge {:class cls} s]))

(defn signal-badge [s]
  (let [ls (str/lower-case (str s))
        cls (cond
              (str/includes? ls "strong buy") "badge-green"
              (str/includes? ls "buy")        "badge-green"
              (str/includes? ls "strong sell") "badge-red"
              (str/includes? ls "sell")        "badge-red"
              :else                            "badge-yellow")]
    [:span.badge {:class cls} s]))

(defn score-gauge [label value]
  (let [v (if (number? value) value 5)]
    [:div.score-item
     [:div.s-val {:style (str "color:" (score-color v))} v]
     [:div.s-lbl label]
     [:div.bar-bg [:div.bar-fill {:style (str "width:" (* v 10) "%; background:" (score-color v))}]]]))

;; ──────────────────────────────────────────────────────────────
;; Results rendering
;; ──────────────────────────────────────────────────────────────
(defn render-article [r]
  [:div.article
   [:div.article-header
    [:h3 (:title r)]
    [:div {:style "display:flex; gap:6px; flex-wrap:wrap; align-items:center;"}
     (sentiment-badge (:sentiment r))
     (signal-badge (:action-signal r))
     [:span.badge.badge-blue (:time-sensitivity r)]]]
   [:div.article-body
    ;; Score gauges
    [:div.scores
     (score-gauge "Sentiment" (:sentiment-score r))
     (score-gauge "Impact" (:impact-score r))
     (score-gauge "Credibility" (:credibility r))]

    ;; TL;DR & Summary
    [:p {:style "margin-bottom:8px;"} [:strong "TL;DR: "] (:tldr r)]
    [:p {:style "color: var(--text-dim); font-size:.88rem; margin-bottom:12px;"} (:summary r)]

    ;; Key Quotes
    (when (seq (:key-quotes r))
      [:div
       (for [q (:key-quotes r)]
         [:div.quotes (str "\"" q "\"")])])

    ;; Meta grid
    [:div {:style "margin-top:12px;"}
     [:dl.meta-grid
      [:dt "Category"] [:dd (:category r)]
      [:dt "Source"]    [:dd (or (:source r) "—")]
      [:dt "Published"] [:dd (or (:published r) "—")]
      [:dt "Bias"]      [:dd (:bias r)]
      [:dt "Audience"]  [:dd (:target-audience r)]
      [:dt "Embedding"] [:dd (or (:embedding-source r) "tfidf")]]]

    ;; Sectors
    (when (seq (:sectors r))
      [:div {:style "margin-top:10px;"}
       [:strong {:style "font-size:.78rem; color:var(--text-dim);"} "SECTORS"]
       [:div.tags (for [s (:sectors r)] [:span.tag s])]])

    ;; Keywords & Entities
    (when (seq (:keywords r))
      [:div {:style "margin-top:8px;"}
       [:strong {:style "font-size:.78rem; color:var(--text-dim);"} "KEYWORDS"]
       [:div.tags (for [k (:keywords r)] [:span.tag k])]])

    (when (seq (:entities r))
      [:div {:style "margin-top:8px;"}
       [:strong {:style "font-size:.78rem; color:var(--text-dim);"} "ENTITIES"]
       [:div.tags (for [e (:entities r)] [:span.tag {:style "background:rgba(167,139,250,.1); color:var(--purple); border-color:rgba(167,139,250,.2);"} e])]])

    ;; Risk Factors
    (when (seq (:risk-factors r))
      [:div {:style "margin-top:10px;"}
       [:strong {:style "font-size:.78rem; color:var(--yellow);"} "RISK FACTORS"]
       [:ul.risk-list (for [rf (:risk-factors r)] [:li rf])]])

    ;; Link
    (when (:link r)
      [:p {:style "margin-top:12px;"}
       [:a {:href (:link r) :target "_blank" :style "color:var(--accent); text-decoration:none;"} "Read Original Article →"]])]])

(defn compute-stats [results]
  (let [ok (filter :success results)
        sentiments (map :sentiment-score ok)
        impacts (map :impact-score ok)
        avg (fn [xs] (if (seq xs) (double (/ (reduce + xs) (count xs))) 0))
        signal-counts (frequencies (map :action-signal ok))
        dominant-signal (if (seq signal-counts)
                          (key (apply max-key val signal-counts))
                          "N/A")]
    {:total (count results)
     :success (count ok)
     :avg-sentiment (format "%.1f" (avg sentiments))
     :avg-impact (format "%.1f" (avg impacts))
     :dominant-signal dominant-signal}))

;; ──────────────────────────────────────────────────────────────
;; Handlers
;; ──────────────────────────────────────────────────────────────
(defn analyze-handler [request]
  (let [params (:params request)
        htmx? (get-in (:headers request) "hx-request")
        query (get params "query")
        country (get params "country" "us")
        language (get params "language" "en")
        max-articles (try (Long/parseLong (get params "max-articles" "3")) (catch Exception _ 3))
        model (get params "model" (:model core/default-config))
        depth (keyword (get params "depth" "standard"))
        output-lang (get params "output-lang" "English")
        sim-threshold (try (Double/parseDouble (get params "sim-threshold" "0.7")) (catch Exception _ 0.7))
        
        respond (fn [title content]
                  (if htmx?
                    (html5 [:div (when (not= title "Error") [:h2 {:style "margin-bottom:20px"} title]) content])
                    (layout title content)))]

  (try
    (let [api-keys (core/validate-api-keys)
          search-query (if (str/blank? query) nil query)]

      (if (and (str/blank? search-query) (str/blank? country))
        (respond "Error" [:div.error-box [:p "Please provide a query or country."]])

        (let [news (core/fetch-news (:newsdata api-keys) country language max-articles search-query)]
          (if (empty? news)
            (respond "No Results" [:div.error-box [:p "No articles found for your query."]])
            (let [;; Inject extra params into each article map so build-prompt can use them
                  enriched-news (map #(assoc % :depth depth :output-lang output-lang) news)
                  results (doall
                            (map-indexed
                             (fn [idx article]
                               (println (str "[" (inc idx) "/" (count news) "] Analyzing: " (:title article)))
                               (core/analyze-article (:llm api-keys) article model nil
                                                     (:request-delay core/default-config)
                                                     (:max-retries core/default-config)
                                                     (:embedding-url core/default-config)
                                                     sim-threshold))
                             enriched-news))
                  stats (compute-stats results)]
              (respond (str "Results — " (or search-query "Latest News"))
                [:div
                  ;; Stats bar
                  [:div.stats-bar
                   [:div.stat [:div.val {:style "color:var(--accent)"} (:total stats)] [:div.lbl "Articles"]]
                   [:div.stat [:div.val {:style "color:var(--green)"}  (:success stats)] [:div.lbl "Analyzed"]]
                   [:div.stat [:div.val {:style "color:var(--yellow)"} (:avg-sentiment stats)] [:div.lbl "Avg Sentiment"]]
                   [:div.stat [:div.val {:style "color:var(--purple)"} (:avg-impact stats)] [:div.lbl "Avg Impact"]]
                   [:div.stat [:div.val (signal-badge (:dominant-signal stats))] [:div.lbl "Market Signal"]]]

                  ;; Article cards
                  [:div (for [r results] (render-article r))]

                  [:a.back-link {:href "/"} "← New Analysis"]]))))))
    (catch Exception e
      (respond "Error"
        [:div.error-box
         [:p "An error occurred"]
         [:p {:style "font-size:.85rem; margin-top:8px;"} (.getMessage e)]])))))

;; ──────────────────────────────────────────────────────────────
;; Routes & Middleware
;; ──────────────────────────────────────────────────────────────
(defroutes app-routes
  (GET "/" [] (home-page))
  (POST "/analyze" request (analyze-handler request))
  (route/not-found (layout "404" [:div.error-box [:p "Page not found."]])))

(defn wrap-params [handler]
  (fn [request]
    (let [query-string (:query-string request)
          body (when (:body request)
                 (try (slurp (:body request)) (catch Exception _ nil)))
          parse-params (fn [s]
                         (when s
                           (into {} (for [pair (str/split s #"&")
                                         :let [[k v] (str/split pair #"=" 2)]
                                         :when k]
                                     [(java.net.URLDecoder/decode k "UTF-8")
                                      (if v (java.net.URLDecoder/decode v "UTF-8") "")]))))
          query-params (parse-params query-string)
          form-params (parse-params body)
          all-params (merge query-params form-params)]
      (handler (assoc request :params all-params)))))

(def app (wrap-params app-routes))

(defn -main [& args]
  (let [port (or (some-> (System/getenv "PORT") parse-long) 3000)]
    (println "")
    (println "╔══════════════════════════════════════════════════╗")
    (println "║  CIM Portfolio – Financial News Analyzer        ║")
    (println "║  AI-Powered Sentiment & Market Intelligence     ║")
    (println (str "║  Server running: http://localhost:" port (apply str (repeat (- 22 (count (str port))) " ")) "║"))
    (println "╚══════════════════════════════════════════════════╝")
    (println "")
    (http/run-server app {:port port})
    @(promise)))
