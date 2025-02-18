(ns user
  (:require [nextjournal.clerk :as clerk]))

;; Start Clerk's built-in webserver on the default port 7777, opening the browser when done
;; Watch for changes in the notebooks and src directories

(clerk/serve! {:browse? true
               :paths ["notebooks", "src/user.clj", "src/cim_portfolio/*.clj"]
               :watch-paths ["notebooks", "src/user.clj", "src/cim_portfolio/*.clj"]               
               })