from flask import Flask, request, jsonify
from datetime import datetime, timedelta
import yfinance as yf

app = Flask(__name__)

def get_ticker_price(ticker, date):
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date)
        if len(data) > 0:
            start_price = (data.iloc[0]['Close'], data.index[0].strftime('%Y-%m-%d'))
            curr_price = (data.iloc[-1]['Close'], data.index[-1].strftime('%Y-%m-%d'))
            return start_price, curr_price
        date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
        if count > 10: return

def get_ticker_price_all(ticker, date):
    date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    count = 0
    while True:
        count += 1
        data = yf.download(ticker, start=date)
        if len(data) > 0:
            return data
        date = (datetime.strptime(date, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
        if count > 10: return

@app.route('/price', methods=['GET'])
def get_stock_price():
    ticker = request.args.get('ticker')
    date = request.args.get('date')
    
    if not ticker or not date:
        return jsonify({'error': 'Ticker and date parameters are required.'}), 400
    
    try:
        datetime.strptime(date, '%Y-%m-%d')
    except ValueError:
        return jsonify({'error': 'Invalid date format. Please use YYYY-MM-DD.'}), 400

    price = get_ticker_price_all(ticker, date)
    
    if price is not None:
        print(price)
        price.reset_index(inplace=True)
        price['Date'] = price['Date'].dt.strftime('%Y-%m-%d')
        return price[['Date', 'Open', 'Adj Close']].to_json(orient = "values") # return adj close to compute stock return + arithmetic return + log return
    else:
        return jsonify({'error': 'No data available for the provided ticker and date.'}), 404

if __name__ == '__main__':
    app.run()