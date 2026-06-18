(ns user
  (:require [nextjournal.clerk :as clerk]))

(when (= "true" (System/getenv "START_CLERK"))
  (clerk/serve! {:browse? true
                 :watch-paths ["src"]
                 :host "0.0.0.0"
                 :port 8990}))