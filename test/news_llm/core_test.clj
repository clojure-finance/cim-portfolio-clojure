(ns news-llm.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [news-llm.core :as news]
            [clj-http.client]
            [cheshire.core]))

(deftest build-prompt-test
  (testing "the prompt carries the title and prefers the full text over the feed's description"
    (let [prompt (news/build-prompt {:title "Breaking News" :full-content "Full text" :description "Short"})]
      (is (str/includes? prompt "Title: Breaking News"))
      (is (str/includes? prompt "Content: Full text"))))
  (testing "long content is truncated"
    (is (str/includes? (news/build-prompt {:title "T" :description (apply str (repeat 5000 "x"))})
                       "[Content Truncated]"))))

(deftest extract-llm-content-test
  (testing "the JSON object is pulled out of a fenced or chatty reply"
    (is (= "{\"a\": 1}"
           (news/extract-llm-content {:body {:choices [{:message {:content "Here:\n```json\n{\"a\": 1}\n```"}}]}})))))

(deftest api-error-message-test
  (testing "provider error bodies arrive as maps or as raw JSON strings"
    (is (= "Insufficient Balance" (news/api-error-message {:error {:message "Insufficient Balance"}})))
    (is (= "Insufficient Balance" (news/api-error-message "{\"error\":{\"message\":\"Insufficient Balance\"}}")))
    (is (nil? (news/api-error-message "<html>Bad Gateway</html>")))))

(deftest llm-request-test
  (let [sent (atom [])
        reply {:status 200 :body {:choices [{:message {:content "{\"summary\": \"ok\"}"}}]}}]
    (with-redefs [clj-http.client/post (fn [url req] (swap! sent conj [url (cheshire.core/decode (:body req) true)]) reply)]
      (news/call-llm "key" "prompt" "deepseek-flash" news/deepseek-url 0)
      (news/call-llm "key" "prompt" "openrouter/free" news/openrouter-url 0))
    (testing "DeepSeek requests turn thinking off (it is on by default)"
      (is (= {:type "disabled"} (:thinking (second (first @sent))))))
    (testing "OpenRouter requests carry no DeepSeek-specific field"
      (is (not (contains? (second (second @sent)) :thinking))))
    (testing "the default DeepSeek model is a current one"
      (is (= "deepseek-flash" (first news/deepseek-models))))))
