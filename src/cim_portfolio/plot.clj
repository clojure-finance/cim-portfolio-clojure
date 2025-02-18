(ns cim_portfolio.plot
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :as str]
            [clojure.data.json :as json]))

;; Generate a UUID
(defn- uuid [] (str (java.util.UUID/randomUUID)))

;; Add indices to data
(defn add-indices [d] (map vector (range (count d)) d))

(defn list-plot
  "Function for plotting list data using Clerk's Plotly integration."
  [data & {:keys [x-title y-title]
           :or   {x-title "X"
                  y-title "Y"}}]
  (let [plot-data (if (sequential? (first data))
                    data
                    (map-indexed vector data))
        trace {:x (map first plot-data)
               :y (map second plot-data)
               :type "scatter"
               :mode "lines"}]
    (clerk/plotly {:data [trace]
                   :layout {:xaxis {:title x-title
                                     :title_standoff 40} ; Increase space between x-axis title and values
                            :yaxis {:title y-title
                                     :title_standoff 40} ; Increase space between y-axis title and values
                            :width 750
                            :height 375
                            :margin {:l 70 :r 20 :b 70 :t 20} ; Further increase left and bottom margins
                            :paper_bgcolor "transparent"
                            :plot_bgcolor "transparent"}
                   :config {:displayModeBar false
                            :displayLogo false}})))

;; Example usage
(list-plot [1 2 3 4 5 6 7 8 9 10 11] :x-title "Time" :y-title "Value")