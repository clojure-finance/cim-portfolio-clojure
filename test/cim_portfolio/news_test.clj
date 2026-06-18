(ns cim-portfolio.news-test
  (:require [clojure.test :refer :all]
            [cim-portfolio.news.core :as core]))

(deftest api-format-test
  (testing "Format JSON output"
    (let [sample-result [{:title "Test" :summary "Summary" :success true}]]
      (is (string? (core/format-json sample-result))))))

(deftest prompt-test
  (testing "Prompt contains title"
    (let [article {:title "Breaking News" :full-content "Content here..."}]
      (is (clojure.string/includes? (core/build-prompt article) "Breaking News")))))
