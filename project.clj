(defproject cim_portfolio "0.1.1"
  :description "A portfolio analysis program written in Clojure"
  :url "https://github.com/clojure-finance/cim-portfolio-clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/data.csv "1.0.1"]
                 [clj-time "0.15.2"]
                 [clj-http "3.13.1"]
                 [cheshire "5.12.0"]
                 [clj-python/libpython-clj "2.025"]
                 [io.github.nextjournal/clerk "0.18.1150"]
                 [nrepl "1.0.0"]
                 [org.slf4j/slf4j-api "2.0.9"]         ; Add this line for SLF4J API
                 [org.slf4j/slf4j-simple "2.0.9"]
                 [generateme/fastmath "3.0.0-alpha3"]
                 [datalevin "0.9.22"]
                 [com.github.clojure-finance/clj-yfinance "0.1.7"] ; Split events for corporate-action adjustment of trades
]     ; Add this line for SLF4J Simple Logger
  :main ^:skip-aot cim-portfolio.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
