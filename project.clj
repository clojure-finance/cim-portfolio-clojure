(defproject cim_portfolio "0.1.1"
  :description "A portfolio analysis program written in Clojure"
  :url "https://github.com/clojure-finance/cim-portfolio-clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/data.csv "1.0.1"]
                 [clj-time "0.15.2"]
                 [clj-http "3.12.3"]
                 [cheshire "5.12.0"]
                 [gorilla-plot "0.1.4"]
                 [clojure.java-time "1.4.2"]
                 [clj-python/libpython-clj "2.025"]]
  :main ^:skip-aot cim-portfolio.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[org.clojars.benfb/lein-gorilla "0.6.0"]]
)