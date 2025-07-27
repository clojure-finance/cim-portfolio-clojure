(ns user
  (:require [nextjournal.clerk :as clerk]))

;; (clerk/serve! {:browse? true
;;                :watch-paths ["src"]               
;;                })

;; Start Clerk's built-in webserver on the port 8990, opening the browser when done (only if :browse? true and running outside docker)
;; Watch for changes in the src directory and bind the application to 0.0.0.0 and port 8990
(clerk/serve! {:browse? true
               :watch-paths ["src"]
               :host "0.0.0.0"               
               :port 8990})