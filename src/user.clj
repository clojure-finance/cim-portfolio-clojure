(ns user
  (:require [nextjournal.clerk :as clerk]))

;; Start Clerk's built-in webserver on the default port 7777, opening the browser when done
;; Watch for changes in the notebooks and src directories

(clerk/serve! {:browse? true
               :watch-paths ["src"]               
               })