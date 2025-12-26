(ns portfolio-web.controllers 
  (:require [ring.util.response :as res]
            [ring.util.request :as req]
            [ring.middleware.resource :as resource]
            [portfolio-web.views :as views]
            [portfolio-web.validator :as validator]
            [portfolio-web.model :as model]))

;; Home Webpage Handler
(defn home-handler [request]
  (res/content-type (res/response (views/home-page)) "text/html"))

;; Portfolio Analysis Page
;; (defn analysis-handler [request]
;;   (res/content-type (res/response (-> request
;;                                       (:body)
;;                                       ; Convert URL-Encoded to raw string
;;                                       (slurp)
;;                                       ; Validate and parse the trades format
;;                                       (validator/parse-trades)
;;                                       ; Send result to model to be processed
;;                                       (model/process-trades)
;;                                       ; Send processed data to view to display
;;                                       ;; (views/test-page)
;;                                       (views/portfolio-page)
;;                                       ))
;;                     "text/html"))

;; For testing purposes only
(defn analysis-handler [request]
  (res/content-type (res/response (-> request
                                      (:body)
                                      ; Convert URL-Encoded to raw string
                                      (slurp)
                                      ; Validate and parse the input
                                      (validator/parse-trades) 
                                      ; Send result to model to be processed
                                      (model/process-trades)
                                      ; Send processed data to view to display
                                      ;; (views/test-page)
                                      (views/portfolio-page)
                                      ))
                    "text/html"))

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
      (resource/wrap-resource "public")))





