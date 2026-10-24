(ns portfolio-web.views-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [portfolio-web.views :as views]))

(deftest escaping-test
  (testing "error messages echo user input, so they are HTML-escaped"
    (let [page (views/home-page "Line 1 (\"<script>alert(1)</script>\"): bad")]
      (is (not (str/includes? page "<script>alert(1)")))
      (is (str/includes? page "&lt;script&gt;alert(1)"))))
  (testing "news content is escaped, and only http(s) links become links"
    (let [page (views/news-results-page [{:success true :title "<b>Title</b>" :link "javascript:alert(1)"
                                          :summary "<img src=x onerror=alert(1)>" :sentiment "Positive"}
                                         {:success true :title "Safe" :link "https://example.com/a" :sentiment "Neutral"}]
                                        nil)]
      (is (not (str/includes? page "<b>Title</b>")))
      (is (not (str/includes? page "<img src=x")))
      (is (not (str/includes? page "javascript:alert")))
      (is (str/includes? page "href=\"https://example.com/a\""))))
  (testing "the news page's own inline script is not escaped"
    (is (str/includes? (views/news-page) "input[name=\"llm-provider\"]:checked"))))
