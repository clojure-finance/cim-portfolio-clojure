# cim_portfolio_clojure

A portfolio analysis program written in Clojure, now featuring **AI-Powered Market News Analysis**.

The web app has two pages:

- **`/` — Portfolio Analyzer**: enter or upload your trades, set your starting cash, and get a full performance dashboard. No API keys required.
- **`/news` — AI News Analyzer**: fetches real-time market news and uses Large Language Models (LLMs) to provide sentiment analysis, summaries, and investment signals. Requires API keys (see below).

## 🚀 Quick Start

### Prerequisites

The Portfolio Analyzer works out of the box. To use the AI News Analyzer, you additionally need API keys for:
1. **NewsData.io** (for fetching news) — free tier at [newsdata.io](https://newsdata.io)
2. **LLM API** — choose one:
   - **DeepSeek** (default, recommended) — [platform.deepseek.com](https://platform.deepseek.com)
   - **OpenRouter** (100+ models, free tiers available) — [openrouter.ai](https://openrouter.ai)

You enter both keys directly in the `/news` form — no environment variables or config
files needed. After your first analysis run, the browser remembers the keys
(localStorage) and pre-fills them on later visits.

### Option 1: Run with Docker Compose (Recommended)

1. **Start the service**:
   ```bash
   docker-compose up --build
   ```

2. **Access the App**:
   Open [http://localhost:3000](http://localhost:3000) in your browser.

### Option 2: Run Locally (Non-Docker)

1. **Install Dependencies**: Java (JDK 21+), Leiningen.
2. **Run the App**:
   ```bash
   # Mac/Linux
   ./run_web_app.sh

   # Windows
   run_web_app.bat
   ```
   Or manually: `lein run`

---

## Portfolio Analyzer

Open [http://localhost:3000](http://localhost:3000), enter your trades manually (one CSV-style row per line) or upload a trade CSV file, set your starting cash amount (in USD — it influences your return %, portfolio volatility, etc.), and submit. The results dashboard is generated from your trades and live market data.

Market data is fetched natively via [clj-yfinance](https://github.com/clojure-finance/clj-yfinance), and currency conversion uses ECB rates via [ecbjure](https://github.com/clojure-finance/ecbjure) — no Python installation is required. An internet connection is needed to fetch prices and FX rates.

### Trade File Format

Each trade file is a csv with one row per trade:

Date (YYYY-MM-DD)   |   Action (buy/sell)   |   Number of units bought/sold    |    Ticker    |    Price (optional; the actual per-unit price paid/received)

A header line is optional — it is recognized and skipped automatically, whether pasted into the manual form or included in an uploaded file. Rows are validated before analysis; invalid dates, actions, amounts, or prices are rejected with a line-numbered message shown on the form.

Record trades exactly as they happened: units and prices as of the trade date. Stock splits are handled automatically — trade data is normalized to post-split units before analysis, consistent with Yahoo Finance's split-adjusted price history. Sample files are provided in the `examples/` directory (e.g. `examples/testPortfolio.csv`).

### Output

The dashboard shows the most relevant statistics about your portfolio performance:

- your current portfolio value (cash + stocks)
- an overview of which securities you hold (number of units as well as their current value)
- cumulative portfolio return and the portfolio-value chart from the first day of trades
- performance metrics of individual stocks
- rolling alpha/beta versus the market index, volatility, and Sharpe ratio

**Net-short portfolios:** the cumulative-return figures show "n/a" for any period in which the
portfolio's value was negative (e.g. a net-short book), because a percentage return on negative
capital is undefined — the naive ratio flips sign exactly when the book is short. A possible
future extension is to compute these returns on gross exposure instead (daily PnL divided by the
sum of the absolute holding values), which is the standard convention for long-short portfolios,
is well-defined for shorts, and reduces to the current calculation for long-only books. It would
require reworking the cumulative aggregation (log-return summing no longer applies directly) and
relabeling the affected figures as returns on gross invested capital.

## AI News Analyzer

Open [http://localhost:3000/news](http://localhost:3000/news), enter your API keys, pick an LLM provider and model, and submit a topic. Each fetched article is analyzed into a 15-field breakdown (sentiment, summary, market impact, investment stance, risk level, actionable insight, and more), displayed on a results dashboard with sentiment distribution and per-article cards. If the provider returns an error (e.g. an exhausted balance or an invalid key), the real API error is surfaced in the UI.

The API keys are only ever held in your browser and sent with the analysis request — the server does not store them. After the first analysis run your browser remembers them (localStorage) and pre-fills the form on later visits.

### Bugs

This is a work-in-progress software and bugs may be present. Please flag and report them :)


## Deploying to Heroku

Deployment is a plain `git push` — Heroku builds the app from source at deploy time, so no jar is committed to the repository. (This replaces the old flow where a pre-built `cim_portfolio-0.1.1-standalone.jar` was committed and run directly; that jar is no longer tracked.)

### One-time setup

1. Install the [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli), log in (`heroku login`), and add the app as a git remote:
   ```bash
   heroku git:remote -a <app-name>
   ```
2. Configure the buildpack. The app needs **only** the Clojure buildpack — the Python buildpack from the old setup is obsolete (the Python dependency was removed in July 2026):
   ```bash
   heroku buildpacks:clear
   heroku buildpacks:add heroku/clojure
   ```
3. Tell the buildpack to build the uberjar (by default it only runs `lein compile`, which would not produce the jar the Procfile expects):
   ```bash
   heroku config:set LEIN_BUILD_TASK="do clean, uberjar"
   ```

No API-key config vars are needed: the AI News Analyzer takes the keys from the
web form (the deployed web app never reads `NEWSDATA_API_KEY`/`DEEPSEEK_API_KEY`;
those env vars are only used by the CLI/notebook code paths).

### Deploying a new version

```bash
git push heroku web-application:main
```

This pushes the local `web-application` branch to Heroku's `main` branch and triggers the build: Heroku runs `lein uberjar` (Java 21, per `system.properties`) and starts the app via the `Procfile`, which points at `target/uberjar/cim_portfolio-standalone.jar` — that path is version-independent because `:uberjar-name` is pinned in `project.clj`.

Notes:

- Pushing to GitHub (`git push origin web-application`) is a separate step for syncing the repository only; it does not deploy anything.
- Building locally (`lein clean && lein uberjar`) is optional — useful to verify the build before pushing, but Heroku compiles on the server regardless.

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
