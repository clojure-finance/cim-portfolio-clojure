;; gorilla-repl.fileformat = 1

;; **
;;; # Clojure Wrapper over Python's yfinance API
;;; 
;;; ### Requires python, yfinance etc. to be installed on local machine
;; **

;; @@
(ns cim_portfolio.yfinanceclient
  (:require [libpython-clj2.require :refer [require-python]]
            [libpython-clj2.python :refer [py. py.. py.-] :as py]
            [clojure.data.json :as json]
  )
)

(require-python '[yfinance :as yf]
                '[datetime :as dt])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-keyword'>:ok</span>","value":":ok"}
;; <=

;; @@
;; Test if yfinance working through clojure-python wrapper
(yf/download "AAPL" "2025-01-15" :progress false :auto_adjust false)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>                  Open        High  ...   Adj Close     Volume\nDate                                ...                       \n2025-01-15  234.639999  238.960007  ...  237.608749   39832000\n2025-01-16  237.350006  238.009995  ...  228.009308   71759100\n2025-01-17  232.119995  232.289993  ...  229.727417   68488300\n2025-01-21  224.000000  224.419998  ...  222.395477   98070400\n2025-01-22  219.789993  224.119995  ...  223.584167   64126500\n2025-01-23  224.740005  227.029999  ...  223.414368   60234800\n2025-01-24  224.779999  225.630005  ...  222.535324   54697900\n2025-01-27  224.020004  232.149994  ...  229.607544   94863400\n2025-01-28  230.850006  240.190002  ...  237.998322   75707600\n2025-01-29  234.119995  239.860001  ...  239.097122   45486100\n2025-01-30  238.669998  240.789993  ...  237.329056   55658300\n2025-01-31  247.190002  247.190002  ...  235.740814  101075100\n2025-02-03  229.990005  231.830002  ...  227.759583   73063300\n2025-02-04  227.250000  233.130005  ...  232.544327   45067300\n2025-02-05  228.529999  232.669998  ...  232.214691   39620300\n2025-02-06  231.289993  233.800003  ...  232.963867   29925300\n2025-02-07  232.600006  234.000000  ...  227.380005   39707200\n2025-02-10  229.570007  230.589996  ...  227.649994   33115600\n2025-02-11  228.199997  235.229996  ...  232.619995   53718400\n2025-02-12  231.199997  236.960007  ...  236.869995   45243300\n2025-02-13  236.910004  242.339996  ...  241.529999   53543300\n\n[21 rows x 6 columns]</span>","value":"                  Open        High  ...   Adj Close     Volume\nDate                                ...                       \n2025-01-15  234.639999  238.960007  ...  237.608749   39832000\n2025-01-16  237.350006  238.009995  ...  228.009308   71759100\n2025-01-17  232.119995  232.289993  ...  229.727417   68488300\n2025-01-21  224.000000  224.419998  ...  222.395477   98070400\n2025-01-22  219.789993  224.119995  ...  223.584167   64126500\n2025-01-23  224.740005  227.029999  ...  223.414368   60234800\n2025-01-24  224.779999  225.630005  ...  222.535324   54697900\n2025-01-27  224.020004  232.149994  ...  229.607544   94863400\n2025-01-28  230.850006  240.190002  ...  237.998322   75707600\n2025-01-29  234.119995  239.860001  ...  239.097122   45486100\n2025-01-30  238.669998  240.789993  ...  237.329056   55658300\n2025-01-31  247.190002  247.190002  ...  235.740814  101075100\n2025-02-03  229.990005  231.830002  ...  227.759583   73063300\n2025-02-04  227.250000  233.130005  ...  232.544327   45067300\n2025-02-05  228.529999  232.669998  ...  232.214691   39620300\n2025-02-06  231.289993  233.800003  ...  232.963867   29925300\n2025-02-07  232.600006  234.000000  ...  227.380005   39707200\n2025-02-10  229.570007  230.589996  ...  227.649994   33115600\n2025-02-11  228.199997  235.229996  ...  232.619995   53718400\n2025-02-12  231.199997  236.960007  ...  236.869995   45243300\n2025-02-13  236.910004  242.339996  ...  241.529999   53543300\n\n[21 rows x 6 columns]"}
;; <=

;; @@
(def pythonWrapper (py/run-simple-string "from datetime import datetime, timedelta
import yfinance as yf
from currency_converter import CurrencyConverter

def get_ticker_price_all(ticker, date):
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date, progress=False, auto_adjust=False)
        if len(data) > 0:
            break
        date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
        if count > 10:
            return 'ERROR'
    data.reset_index(inplace=True)
    data['Date'] = data['Date'].dt.strftime('%Y-%m-%d')
    stock = yf.Ticker(ticker)

    if stock.info['currency'] != 'USD':
        c = CurrencyConverter()
        fx_to_usd = c.convert(1, stock.info['currency'], 'USD')
    else:
        fx_to_usd = 1
    data['Open'] = data['Open'] * fx_to_usd
    data['Adj Close'] = data['Adj Close'] * fx_to_usd
    return data[['Date', 'Open', 'Adj Close']].to_json(orient = 'values')"))

(def get-ticker-price-all-wrapper (:get_ticker_price_all (:globals pythonWrapper)))

(defn get-ticker-price-all [ticker date]
  (json/read-str (get-ticker-price-all-wrapper ticker date))
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;cim_portfolio.yfinanceclient/get-ticker-price-all</span>","value":"#'cim_portfolio.yfinanceclient/get-ticker-price-all"}
;; <=

;; @@
;; Test if function is working + price is converted to USD

(get-ticker-price-all "3330.HK" "2025-01-15")
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-16&quot;</span>","value":"\"2025-01-16\""},{"type":"html","content":"<span class='clj-double'>0.5007727257</span>","value":"0.5007727257"},{"type":"html","content":"<span class='clj-double'>0.5174224421</span>","value":"0.5174224421"}],"value":"[\"2025-01-16\" 0.5007727257 0.5174224421]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-17&quot;</span>","value":"\"2025-01-17\""},{"type":"html","content":"<span class='clj-double'>0.5212647148</span>","value":"0.5212647148"},{"type":"html","content":"<span class='clj-double'>0.5020534731</span>","value":"0.5020534731"}],"value":"[\"2025-01-17\" 0.5212647148 0.5020534731]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-20&quot;</span>","value":"\"2025-01-20\""},{"type":"html","content":"<span class='clj-double'>0.5033342206</span>","value":"0.5033342206"},{"type":"html","content":"<span class='clj-double'>0.5058957154</span>","value":"0.5058957154"}],"value":"[\"2025-01-20\" 0.5033342206 0.5058957154]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-21&quot;</span>","value":"\"2025-01-21\""},{"type":"html","content":"<span class='clj-double'>0.576336884</span>","value":"0.576336884"},{"type":"html","content":"<span class='clj-double'>0.5532834306</span>","value":"0.5532834306"}],"value":"[\"2025-01-21\" 0.576336884 0.5532834306]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-22&quot;</span>","value":"\"2025-01-22\""},{"type":"html","content":"<span class='clj-double'>0.5609679151</span>","value":"0.5609679151"},{"type":"html","content":"<span class='clj-double'>0.6390935376</span>","value":"0.6390935376"}],"value":"[\"2025-01-22\" 0.5609679151 0.6390935376]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-23&quot;</span>","value":"\"2025-01-23\""},{"type":"html","content":"<span class='clj-double'>0.6390935376</span>","value":"0.6390935376"},{"type":"html","content":"<span class='clj-double'>0.6301283363</span>","value":"0.6301283363"}],"value":"[\"2025-01-23\" 0.6390935376 0.6301283363]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-24&quot;</span>","value":"\"2025-01-24\""},{"type":"html","content":"<span class='clj-double'>0.6314090531</span>","value":"0.6314090531"},{"type":"html","content":"<span class='clj-double'>0.6070748218</span>","value":"0.6070748218"}],"value":"[\"2025-01-24\" 0.6314090531 0.6070748218]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-27&quot;</span>","value":"\"2025-01-27\""},{"type":"html","content":"<span class='clj-double'>0.6134785894</span>","value":"0.6134785894"},{"type":"html","content":"<span class='clj-double'>0.6301283363</span>","value":"0.6301283363"}],"value":"[\"2025-01-27\" 0.6134785894 0.6301283363]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-01-28&quot;</span>","value":"\"2025-01-28\""},{"type":"html","content":"<span class='clj-double'>0.6019518322</span>","value":"0.6019518322"},{"type":"html","content":"<span class='clj-double'>0.6019518322</span>","value":"0.6019518322"}],"value":"[\"2025-01-28\" 0.6019518322 0.6019518322]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-03&quot;</span>","value":"\"2025-02-03\""},{"type":"html","content":"<span class='clj-double'>0.6275668414</span>","value":"0.6275668414"},{"type":"html","content":"<span class='clj-double'>0.6134785894</span>","value":"0.6134785894"}],"value":"[\"2025-02-03\" 0.6275668414 0.6134785894]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-04&quot;</span>","value":"\"2025-02-04\""},{"type":"html","content":"<span class='clj-double'>0.6147593673</span>","value":"0.6147593673"},{"type":"html","content":"<span class='clj-double'>0.6173208622</span>","value":"0.6173208622"}],"value":"[\"2025-02-04\" 0.6147593673 0.6173208622]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-05&quot;</span>","value":"\"2025-02-05\""},{"type":"html","content":"<span class='clj-double'>0.6339705479</span>","value":"0.6339705479"},{"type":"html","content":"<span class='clj-double'>0.6288475583</span>","value":"0.6288475583"}],"value":"[\"2025-02-05\" 0.6339705479 0.6288475583]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-06&quot;</span>","value":"\"2025-02-06\""},{"type":"html","content":"<span class='clj-double'>0.6288475583</span>","value":"0.6288475583"},{"type":"html","content":"<span class='clj-double'>0.6083555998</span>","value":"0.6083555998"}],"value":"[\"2025-02-06\" 0.6288475583 0.6083555998]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-07&quot;</span>","value":"\"2025-02-07\""},{"type":"html","content":"<span class='clj-double'>0.6096363777</span>","value":"0.6096363777"},{"type":"html","content":"<span class='clj-double'>0.6147593673</span>","value":"0.6147593673"}],"value":"[\"2025-02-07\" 0.6096363777 0.6147593673]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-10&quot;</span>","value":"\"2025-02-10\""},{"type":"html","content":"<span class='clj-double'>0.6250053466</span>","value":"0.6250053466"},{"type":"html","content":"<span class='clj-double'>0.6531817896</span>","value":"0.6531817896"}],"value":"[\"2025-02-10\" 0.6250053466 0.6531817896]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-11&quot;</span>","value":"\"2025-02-11\""},{"type":"html","content":"<span class='clj-double'>0.6672700417</span>","value":"0.6672700417"},{"type":"html","content":"<span class='clj-double'>0.6672700417</span>","value":"0.6672700417"}],"value":"[\"2025-02-11\" 0.6672700417 0.6672700417]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-12&quot;</span>","value":"\"2025-02-12\""},{"type":"html","content":"<span class='clj-double'>0.6672700417</span>","value":"0.6672700417"},{"type":"html","content":"<span class='clj-double'>0.6403743155</span>","value":"0.6403743155"}],"value":"[\"2025-02-12\" 0.6672700417 0.6403743155]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-13&quot;</span>","value":"\"2025-02-13\""},{"type":"html","content":"<span class='clj-double'>0.6403743155</span>","value":"0.6403743155"},{"type":"html","content":"<span class='clj-double'>0.6595855572</span>","value":"0.6595855572"}],"value":"[\"2025-02-13\" 0.6403743155 0.6595855572]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;2025-02-14&quot;</span>","value":"\"2025-02-14\""},{"type":"html","content":"<span class='clj-double'>0.6711122534</span>","value":"0.6711122534"},{"type":"html","content":"<span class='clj-double'>0.7095347367</span>","value":"0.7095347367"}],"value":"[\"2025-02-14\" 0.6711122534 0.7095347367]"}],"value":"[[\"2025-01-16\" 0.5007727257 0.5174224421] [\"2025-01-17\" 0.5212647148 0.5020534731] [\"2025-01-20\" 0.5033342206 0.5058957154] [\"2025-01-21\" 0.576336884 0.5532834306] [\"2025-01-22\" 0.5609679151 0.6390935376] [\"2025-01-23\" 0.6390935376 0.6301283363] [\"2025-01-24\" 0.6314090531 0.6070748218] [\"2025-01-27\" 0.6134785894 0.6301283363] [\"2025-01-28\" 0.6019518322 0.6019518322] [\"2025-02-03\" 0.6275668414 0.6134785894] [\"2025-02-04\" 0.6147593673 0.6173208622] [\"2025-02-05\" 0.6339705479 0.6288475583] [\"2025-02-06\" 0.6288475583 0.6083555998] [\"2025-02-07\" 0.6096363777 0.6147593673] [\"2025-02-10\" 0.6250053466 0.6531817896] [\"2025-02-11\" 0.6672700417 0.6672700417] [\"2025-02-12\" 0.6672700417 0.6403743155] [\"2025-02-13\" 0.6403743155 0.6595855572] [\"2025-02-14\" 0.6711122534 0.7095347367]]"}
;; <=
