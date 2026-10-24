(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
;; Same path and name as the former Leiningen uberjar, so deploy steps don't change
(def uber-file "target/uberjar/cim_portfolio-standalone.jar")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
  ;; AOT-compile every namespace with direct linking, as the old uberjar profile did
  (b/compile-clj {:basis @basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :compile-opts {:direct-linking true}})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'cim-portfolio.core})
  (println "Built:" uber-file))
