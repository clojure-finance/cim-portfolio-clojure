;; gorilla-repl.fileformat = 1

;; **
;;; # Portfolio Plotting Functions
;; **

;; @@
(ns cim_portfolio.plot
  (:require [gorilla-plot.core :as plot]
            [gorilla-plot.vega :as vega]
            [gorilla-plot.util :as util]
            [gorilla-repl.vega :as v]
            
            
            [clojure.string :as str]
            #_[clojure.data.csv :as csv]
            [clojure.data.json :as json]
   ))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(defn- uuid [] (str (java.util.UUID/randomUUID)))

(defn add-indices [d] (map vector (range (count d)) d))

(defn list-plot
  "Function for plotting list data."
  [data & {:keys [joined plot-size aspect-ratio colour color plot-range #_symbol symbol-size opacity x-title y-title]
           :or   {joined       false
                  plot-size    400
                  aspect-ratio 1.618
                  plot-range   [:all :all]
                  symbol-size  70
                  opacity      1
                  }}]
  (let [series-name (uuid)
        plot-data (if (sequential? (first data))
                    data
                    (add-indices data))]
     (v/vega-view (merge
                   		
                      (vega/container plot-size aspect-ratio)
      				  {:padding {:top 10, :left 80, :bottom 50, :right 10}}
                      (vega/data-from-list series-name plot-data)
                      (if joined
                        (vega/line-plot-marks series-name (or colour color) opacity)
                        (vega/list-plot-marks series-name (or colour color) #_symbol symbol-size opacity))
                      (vega/default-list-plot-scales series-name plot-range)
       
                      {:axes [
                              {:type "x", :scale "x", :title x-title, :titleOffset 40, :grid true}
                              {:type "y", :scale "y", :title y-title, :titleOffset 65, :grid true}
                              ]
                       }
                   
                       {:marks [
                                {:type "line", :from {:data series-name},  
                                 :properties {:enter 
                                              {:x {:scale "x", :field "data.x"},  
                                               :y {:scale "y", :field "data.y"},
                                               :stroke {:value color}, 
                                               :strokeWidth {:value 3},
                                               :strokeOpacity {:value 1},
                                               }
                                             }
                                 }
                                ]
                        }
      ))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.plot/list-plot</span>","value":"#'cim_portfolio.plot/list-plot"}
;; <=

;; @@
(list-plot [1000 2000 3000 2000 4000] :joined true :plot-size 800 :x-title "x-axis" :y-title "y-axis" :color "black")
;; @@
;; =>
;;; {"type":"vega","content":{"width":800,"height":494.4376,"padding":{"top":10,"left":80,"bottom":50,"right":10},"data":[{"name":"a71be385-91d1-4544-b8d1-30c9405f4797","values":[{"x":0,"y":1000},{"x":1,"y":2000},{"x":2,"y":3000},{"x":3,"y":2000},{"x":4,"y":4000}]}],"marks":[{"type":"line","from":{"data":"a71be385-91d1-4544-b8d1-30c9405f4797"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"stroke":{"value":"black"},"strokeWidth":{"value":3},"strokeOpacity":{"value":1}}}}],"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":{"data":"a71be385-91d1-4544-b8d1-30c9405f4797","field":"data.x"}},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":{"data":"a71be385-91d1-4544-b8d1-30c9405f4797","field":"data.y"}}],"axes":[{"type":"x","scale":"x","title":"x-axis","titleOffset":40,"grid":true},{"type":"y","scale":"y","title":"y-axis","titleOffset":65,"grid":true}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 800, :height 494.4376, :padding {:top 10, :left 80, :bottom 50, :right 10}, :data [{:name \"a71be385-91d1-4544-b8d1-30c9405f4797\", :values ({:x 0, :y 1000} {:x 1, :y 2000} {:x 2, :y 3000} {:x 3, :y 2000} {:x 4, :y 4000})}], :marks [{:type \"line\", :from {:data \"a71be385-91d1-4544-b8d1-30c9405f4797\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :stroke {:value \"black\"}, :strokeWidth {:value 3}, :strokeOpacity {:value 1}}}}], :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain {:data \"a71be385-91d1-4544-b8d1-30c9405f4797\", :field \"data.x\"}} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain {:data \"a71be385-91d1-4544-b8d1-30c9405f4797\", :field \"data.y\"}}], :axes [{:type \"x\", :scale \"x\", :title \"x-axis\", :titleOffset 40, :grid true} {:type \"y\", :scale \"y\", :title \"y-axis\", :titleOffset 65, :grid true}]}}"}
;; <=
