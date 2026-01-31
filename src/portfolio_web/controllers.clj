(ns portfolio-web.controllers 
  (:require [ring.util.response :as res]
            [ring.util.request :as req]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.codec :as codec]
            [portfolio-web.views :as views]
            [portfolio-web.validator :as validator]
            [portfolio-web.model :as model]
            [clojure.string :as str]))

;; Home Webpage Handler
(defn home-handler [request]
  (res/content-type (res/response (views/home-page)) "text/html"))

;; Portfolio Analysis Page, DEPRECATED (used to handle form-data encoded in application/x-www-form-urlencoded)
;; (defn analysis-handler [request]
;;   (res/content-type (res/response (-> request 
;;                                       (:params)

;;                                       (str)
;;                                       ; Convert URL-Encoded to raw string
;;                                       ;; (slurp)
;;                                       ;; ; Validate and parse the trades format
;;                                       ;; (validator/parse-trades)
;;                                       ;; ; Send result to model to be processed
;;                                       ;; (model/process-trades)
;;                                       ;; ; Send processed data to view to display
;;                                       ;; ;; (views/test-page)
;;                                       ;; (views/portfolio-page)
;;                                       ))
;;                     "text/html"))

;; Portfolio Analysis Page
;; Below is updated function (used to handle form-data encoded in multipart/form-data to accept file inputs)
(defn analysis-handler [request]
  (res/content-type (res/response
                     (let [params (:params request)
                           
                           ;; Parse the raw request into the desired format (format is shown in validator.clj)
                           parsed-trades 
                           (if (= (get params "trades") "") ;; If the "trades" key has an empty string as a value, then it is not a manual input  
                             ;; File Input
                             (validator/parse-file-input
                              {:trades (slurp (:tempfile (get params "trades-file")))
                               ;; The above variable (:trades) looks like the following: 
                               ;; "Date of trade submitted (YYYY-MM-DD),Action,Amount Bought/Sold,Ticker\r\n2024-10-15,buy,100,NVDA\r\n2024-11-25,buy,50,GOOG\r\n2024-12-22,sell,30,TSLA\r\n2025-01-08,sell,30,NVDA"
                               :starting-cash (str/replace (get params "starting-cash") #"[$,]" "") ;; This may contain '$' and comma, remove them!
                               :show-capm-metrics (get params "show-capm-metrics" "false")
                               :show-stock-performances (get params "show-stock-performances" "false")})
                             
                             ;; Manual Input 
                             (validator/parse-manual-input
                              {:trades (get params "trades")
                               :starting-cash (str/replace (get params "starting-cash") #"[$,]" "")
                               :show-capm-metrics (get params "show-capm-metrics" "false")
                               :show-stock-performances (get params "show-stock-performances" "false")}))
                           ]
                       (-> parsed-trades
                           
                           ; Send result to the model to be processed
                            (model/process-trades)
                           
                           ; Send processed data to view to display
                            ;; (views/test-page)
                            (views/portfolio-page)
                           )
                       )
                       )
                    "text/html")) 

;; GPT-generated code for sample (code is too convoluted)
;; (defn analysis-handler [request]
;;   ;; Support three ways of sending input:
;;   ;; 1) standard urlencoded body: "trades=...&starting-cash=..." (fallback)
;;   ;; 2) textarea named "trades" (form post)
;;   ;; 3) multipart file named "trades-file" (uploaded file)
;;   (let [params (or (:params request) {})
;;         ;; check for textarea value first
;;         trades-text (or (get params "trades") (get params :trades))
;;         file-param (or (get params "trades-file") (get params :trades-file))
;;         starting-cash (or (get params "starting-cash") (get params :starting-cash) "0")
;;         ;; if a file was uploaded, try reading its tempfile; otherwise use textarea
;;         trades-content (cond
;;                          trades-text trades-text
;;                          (and (map? file-param) (:tempfile file-param)) (try (slurp (:tempfile file-param)) (catch Exception _ nil))
;;                          file-param (try (slurp file-param) (catch Exception _ nil))
;;                          :else nil)
;;         ;; build a raw url-encoded body string expected by validator/parse-trades
;;         raw-body (if trades-content
;;                    (str "trades=" (codec/url-encode trades-content) "&starting-cash=" (codec/url-encode (str starting-cash)))
;;                    ;; fallback: use raw request body if present (previous behavior)
;;                    (try (slurp (:body request)) (catch Exception _ "")))]
;;     (res/content-type
;;      (res/response
;;       (-> raw-body
;;           (validator/parse-trades)
;;           (model/process-trades)
;;           (views/portfolio-page)))
;;      "text/html")))


;; Handler not wrapped with middleware
(defn base-app [request]
  (case (:uri request)
    "/" (home-handler request)
    "/analyze-portfolio" (analysis-handler request)
    {:status 404 :body "Not Found"})
  )

;; Wrapped with middleware
(def app
  (-> base-app
      (resource/wrap-resource "public")
      (wrap-multipart-params)))





