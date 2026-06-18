(ns news-llm.embeddings
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn infer-vector [service-url text]
  "Call embedding service at service-url (e.g., http://localhost:8000/infer) and return vector as a vector of doubles. Returns nil on error."
  (try
    (let [resp (http/post (str service-url "/infer")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/encode {:text text})
                           :as :json
                           :throw-exceptions false})]
      (if (= 200 (:status resp))
        (vec (get-in resp [:body :vector]))
        (do (println "Embedding service error:" (:status resp) (:body resp)) nil)))
    (catch Exception e
      (println "Embedding call failed:" (.getMessage e))
      nil)))
