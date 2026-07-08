# cim_portfolio_clojure

A portfolio analysis program written in Clojure, now featuring **AI-Powered Market News Analysis**.

The web app has two pages:

- **`/` — Portfolio Analyzer**: upload your trade CSVs, set your starting cash, and get a full performance dashboard. No API keys required.
- **`/news` — AI News Analyzer**: fetches real-time market news and uses Large Language Models (LLMs) to provide sentiment analysis, summaries, and investment signals. Requires API keys (see below).

## 🚀 Quick Start

### Prerequisites

The Portfolio Analyzer works out of the box. To use the AI News Analyzer, you additionally need API keys for:
1. **NewsData.io** (for fetching news) — free tier at [newsdata.io](https://newsdata.io)
2. **LLM API** — choose one:
   - **DeepSeek** (default, recommended) — [platform.deepseek.com](https://platform.deepseek.com)
   - **OpenRouter** (100+ models, free tiers available) — [openrouter.ai](https://openrouter.ai)

### Option 1: Run with Docker Compose (Recommended)

1. **Set your API keys** in your environment or a `.env` file:
   ```bash
   export NEWSDATA_API_KEY="your_key_here"
   export DEEPSEEK_API_KEY="your_key_here"
   ```
   *(Windows PowerShell: `$env:NEWSDATA_API_KEY="your_key"`)*

2. **Start the service**:
   ```bash
   docker-compose up --build
   ```

3. **Access the App**:
   Open [http://localhost:3000](http://localhost:3000) in your browser.

### Option 2: Run Locally (Non-Docker)

1. **Install Dependencies**: Java (JDK 21+), Leiningen.
2. **Set Environment Variables**:
   ```bash
   # Mac/Linux
   export NEWSDATA_API_KEY="your_key"
   export DEEPSEEK_API_KEY="your_key"
   
   # Windows (CMD)
   set NEWSDATA_API_KEY=your_key
   set DEEPSEEK_API_KEY=your_key
   ```
3. **Run the App**:
   ```bash
   # Mac/Linux
   ./run_web_app.sh

   # Windows
   run_web_app.bat
   ```
   Or manually: `lein run`

---

## Portfolio Analyzer

Open [http://localhost:3000](http://localhost:3000), upload one or more trade CSV files, set your starting cash amount (in USD — it influences your return %, portfolio volatility, etc.), and submit. The results dashboard is generated from your trades and live market data.

Market data is fetched natively via [clj-yfinance](https://github.com/clojure-finance/clj-yfinance), and currency conversion uses ECB rates via [ecbjure](https://github.com/clojure-finance/ecbjure) — no Python installation is required. An internet connection is needed to fetch prices and FX rates.

### Trade File Format

Each trade file is a csv with one row per trade:

Date (YYYY-MM-DD)   |   Action (buy/sell)   |   Number of units bought/sold    |    Ticker    |    Price (optional; the actual per-unit price paid/received)

Record trades exactly as they happened: units and prices as of the trade date. Stock splits are handled automatically — trade data is normalized to post-split units before analysis, consistent with Yahoo Finance's split-adjusted price history. Sample files are provided in the `examples/` directory (e.g. `examples/testPortfolio.csv`).

### Output

The dashboard shows the most relevant statistics about your portfolio performance:

- your current portfolio value (cash + stocks)
- an overview of which securities you hold (number of units as well as their current value)
- cumulative portfolio return and the portfolio-value chart from the first day of trades
- performance metrics of individual stocks
- rolling alpha/beta versus the market index, volatility, and Sharpe ratio

## AI News Analyzer

Open [http://localhost:3000/news](http://localhost:3000/news), pick an LLM provider and model, and submit a topic. Each fetched article is analyzed into a 15-field breakdown (sentiment, summary, market impact, investment stance, risk level, actionable insight, and more), displayed on a results dashboard with sentiment distribution and per-article cards. If the provider returns an error (e.g. an exhausted balance or an invalid key), the real API error is surfaced in the UI.

### Bugs

This is a work-in-progress software and bugs may be present. Please flag and report them :)


## License

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
