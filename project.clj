(defproject cim_portfolio "0.1.1"
  :description "A portfolio analysis program written in Clojure"
  :url "https://github.com/clojure-finance/cim-portfolio-clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/data.csv "1.0.1"]
                 [org.clojure/data.json "2.5.1"]
                 [org.clojure/tools.cli "1.1.230"]
                 [clj-time "0.15.2"]
                 [nrepl "1.0.0"]
                 [io.github.nextjournal/clerk "0.18.1150" :exclusions [hiccup]]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [org.slf4j/slf4j-simple "2.0.9"]
                 [generateme/fastmath "3.0.0-alpha3"]
                 ;; Web framework
                 [ring/ring-core "1.15.3"]
                 [ring/ring-jetty-adapter "1.15.3"]
                 [ring/ring-codec "1.3.0"]
                 [hiccup "1.0.5"]
                 ;; Portfolio data sources
                 [com.github.clojure-finance/clj-yfinance "0.1.6"]
                 [com.github.clojure-finance/ecbjure "0.1.4"]
                 ;; News analysis
                 [clj-http/clj-http "3.12.3"]
                 [cheshire/cheshire "5.11.0"]
                 [org.jsoup/jsoup "1.17.2"]]

  :main cim-portfolio.core
  :aot [cim-portfolio.core]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
