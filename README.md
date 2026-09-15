# CIM Portfolio

Analyze your investment portfolio with real market data — track performance, measure risk, and get AI-powered news insights. Written in Clojure.

The web app has two pages:

- **`/` — Portfolio Analyzer**: no API keys needed
- **`/news` — AI News Analyzer**: bring your own API keys

## Features

### Portfolio Analyzer

Type in or upload your trades, set your starting cash, and get a performance dashboard:

- **Portfolio value** — cash plus holdings, with units, value, and weight per security
- **Performance charts** — portfolio value and cumulative return since your first trade
- **Risk metrics** — portfolio alpha and per-stock rolling alpha/beta vs. the S&P 500, volatility, and Sharpe ratio
- **Per-stock breakdown** — how each security has performed

Works with any ticker on Yahoo Finance. Stock splits are handled automatically, and prices for non-USD tickers are converted to USD at ECB exchange rates.

### AI News Analyzer

Pick a topic and get recent market news analyzed by an LLM:

- **Sentiment** — positive/negative/neutral, with a 1–10 score
- **Investment signal** — Strong Buy to Strong Sell, plus market impact and risk factors
- **Summaries** — a short summary of each article, plus sectors, key quotes, and a bias check

Results appear as a dashboard with the sentiment breakdown and one card per article. If the provider returns an error, such as an invalid key or an empty balance, the error message is shown on the page.

## Quick Start

### Docker (recommended)

```bash
docker-compose up --build
```

Open [http://localhost:3000](http://localhost:3000)

### Local

Requires Java 21+ and [Leiningen](https://leiningen.org/).

```bash
./run_web_app.sh   # Mac/Linux
run_web_app.bat    # Windows
```

Or simply: `lein run`

You need an internet connection either way, because prices and exchange rates are fetched live.

## Using the Portfolio Analyzer

1. **Enter trades** — type or paste them into the form, one per line, or upload a CSV file
2. **Set starting cash** (USD) — this changes your return %, volatility, and the other metrics
3. **Submit** — the dashboard is built from your trades and live market data

### Trade format

One trade per row:

| Date | Action | Quantity | Ticker | Price (optional) |
|------|--------|----------|--------|------------------|
| 2023-10-13 | buy | 100 | SPY | |
| 2023-11-03 | buy | 50 | SPY | 432.50 |
| 2024-01-15 | sell | 25 | SPY | |

- **Date**: `YYYY-MM-DD`
- **Action**: `buy` or `sell`
- **Quantity**: number of units bought or sold
- **Ticker**: Yahoo Finance symbol
- **Price**: the per-unit price you actually paid or received, in the ticker's own currency. If you leave it out, the opening price on the trade date is used.

Enter trades exactly as they happened, with units and prices as of the trade date. Don't adjust them for later splits; the app does that for you.

A header row is optional and gets skipped automatically. Every row is checked before analysis, and any problems are listed on the form by line number. Sample portfolios are in [`examples/`](examples/).

## API Keys (News Analyzer only)

1. **[NewsData.io](https://newsdata.io)** — for fetching news (free tier available)
2. **An LLM provider** — choose one:
   - [DeepSeek](https://platform.deepseek.com) (default, recommended)
   - [OpenRouter](https://openrouter.ai) (100+ models, some free)

Enter the keys in the `/news` form; no environment variables or config files are needed. Your keys stay in your browser and are only sent along with each analysis request. The server never stores them. After your first run, the browser remembers the keys and fills them in next time.

## Technical Notes

- Market data comes from [clj-yfinance](https://github.com/clojure-finance/clj-yfinance), and currency conversion from [ecbjure](https://github.com/clojure-finance/ecbjure). No Python is needed.
- **Net-short portfolios**: cumulative returns show "n/a" for any period when the portfolio's value was negative, because a percentage return on negative capital is undefined. A possible future extension is to measure returns against gross exposure (the sum of absolute holding values), the usual convention for long-short books.

## Bugs

This is a work in progress and may have bugs. Please report any you find.

## License

EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0

See the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) and the [GNU Classpath Exception](https://www.gnu.org/software/classpath/license.html).
