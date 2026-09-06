# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Security
- Removed a hardcoded DeepSeek API key from `run_web_app.bat` (it had been committed
  and public on GitHub since May 2026 — the key itself must be revoked/rotated at
  platform.deepseek.com, as it remains visible in git history)
- The starting-cash form field is parsed as a plain number; it previously went
  through `read-string`, which evaluates Clojure reader forms (`#=(...)`) from a
  public, unauthenticated form field

### Added
- The results page marks the 1-Year Cumulative Portfolio Return (both the summary
  metric and the weekly comparison card) with a note when the portfolio's history
  is shorter than a year: the window is clamped to inception, so the figure is a
  since-inception return rather than a full 1-year one
- The news form remembers both API keys in the browser (localStorage): they are
  saved on submit and pre-filled on later visits, so they no longer have to be
  retyped every time
- Automatic stock-split adjustment of trade data (`cim_portfolio.corporate-actions`):
  share amounts and user-supplied prices are normalized to post-split units before
  analysis, consistent with Yahoo Finance's split-adjusted price history
  (fixes phantom losses and inverted positions for pre-split trades, e.g. the
  SBS 5:1 split effective 2026-05-07)
- AI news analysis feature: `/news` form page and `/analyze-news` route
- LLM-powered article analysis via DeepSeek and OpenRouter APIs
- 15-field extraction per article: sentiment, tldr, summary, bias, keywords, entities,
  category, target_audience, market_impact, investment_stance, time_sensitivity,
  affected_sectors, key_quote, risk_level, actionable_insight
- Results dashboard with sentiment distribution, market impact bars, and rich article cards
- New CSS components for news UI (hero, provider cards, dashboard grid, article cards, badges)
- Unit tests for the trade-input validator (`portfolio-web.validator-test`): row
  parsing/trimming, header detection for manual and file input, every per-field
  error message with line numbering, and the numeric starting-cash guard

### Changed
- clj-yfinance bumped 0.1.7 → 0.1.8. Its breaking changes (`:auto-adjust` now
  defaults to true, `:adjusted` removed, `:adj-close` always present) do not affect
  this project: both price fetches already pass `:auto-adjust true` explicitly and
  only read `:timestamp`/`:open`/`:close`, so the fetched series is unchanged
- DeepSeek set as default LLM provider and model (deepseek-chat)
- Default output directory for CLI mode changed from hardcoded path to `reports/`
- Heroku deploys now build from source via the Clojure buildpack (`:uberjar-name`
  is pinned so the Procfile path is version-independent; Java runtime bumped to 21,
  matching the Dockerfile and CI) instead of running a committed jar

### Fixed
- The portfolio analyzer no longer answers bad input with a raw 500 page (all three
  crash classes were being hit by real visitors, per the VPS journal):
  - trade rows are validated before analysis, with line-numbered messages for
    invalid dates, actions, amounts, tickers, and prices, shown in a banner on the
    form page; a pasted or uploaded CSV header line is recognized and skipped
    (previously a joda-time "Invalid format" crash)
  - a ticker with no price data (typo, delisted symbol, or future-dated trade) now
    produces a descriptive error instead of a NullPointerException mid-analysis
  - portfolios with fewer than three days of history show "n/a — not enough
    history" for annualized volatility instead of crashing with a divide-by-zero
    in the sample standard deviation (which now returns 0.0 for sub-2-point input)
  - a portfolio whose first trade is dated today shows "n/a — not enough history"
    for the annualized return instead of a divide-by-zero error (annualizing a
    same-day value is undefined)
  - any other analysis error renders a friendly banner and logs the stack trace,
    instead of Jetty's default error page
- Uploading a CSV without a header line no longer silently discards its first
  trade row (the parser used to unconditionally strip line 1)
- Rolling alpha/beta and stock-performance charts render again: a UI-redesign commit
  had replaced the Greek α/β with ASCII a/b in the trace-name lookup, producing null
  chart data that made Plotly abort rendering of all subsequent charts (and the
  volatility/Sharpe λ toggles); the chart loop now also skips missing datasets
- Sell orders now value the daily PnL series at closing prices, matching buy orders
  (previously opening prices, which skewed portfolio value and returns on days with
  sell trades); trades themselves still execute at the open
- Native price fetching (clj-yfinance) is dividend-adjusted again (`:auto-adjust`),
  restoring parity with the Python wrapper's `auto_adjust=True`
- Native `convert-currency` crashed with a ClassCastException when trades carried a
  user-supplied price (the CSV string reached ecbjure's `fx/convert` uncoerced; the
  Python wrapper used to coerce with `float()`)
- "API Error" news cards now surface the provider's real error message and HTTP
  status (e.g. DeepSeek 402 Insufficient Balance); when every article fails, a
  status-specific banner explains the cause (balance, key, rate limit, model
  mismatch, network), and the model dropdown is filtered per provider so a
  DeepSeek model can't be submitted to OpenRouter or vice versa
- `lein uberjar` and `lein repl` no longer hang when ECB's rates endpoint stalls
  mid-transfer: ecbjure is bumped to 0.1.5 (adds connect/read timeouts, so a
  stalled fetch throws instead of blocking forever) and the FX converter in
  `yfinanceclient.clj` is built lazily (a `delay` derefed at the call sites), so
  namespace load / AOT compilation performs no network I/O at all

### Removed
- The lein-template `doc/intro.md` placeholder ("TODO: write great documentation")
- Stale API-key plumbing that the web app never read: the `NEWSDATA_API_KEY`/
  `DEEPSEEK_API_KEY` env-var instructions in the README (and their docker-compose
  passthrough), plus the launcher-script warnings about them — the web UI takes
  both keys from the `/news` form. The env vars remain in use by the CLI/notebook
  code paths only.
- The dead `news_llm_newsdata` auto-start block from both launcher scripts: it
  pointed at a sibling project that no longer exists and nothing references
- The Python/yfinance dependency: the embedded Python wrapper in `yfinanceclient.clj`
  and the load-time Python demo code in `regression.clj` were unused legacy paths —
  all production data flows are Clojure-native (clj-yfinance for prices, ecbjure for
  FX, fastmath for CAPM regression). Dropped `libpython-clj` from project.clj, the
  Python layers from the Dockerfile, `requirements.txt`/`runtime.txt`, and the
  `CIM_PORTFOLIO_PYTHON`/`CIM_PORTFOLIO_LIBPYTHON` environment variables introduced
  earlier in this cycle. No Python installation is needed to build, run, or deploy.
- Backup and disabled source files (core_backup.clj, debug_scraper.clj.disabled, etc.)
- Broken server files with missing dependencies
- Experimental macroexpand-demos directory
- Build artifacts from version control (the pre-built standalone jar and the
  compiled `classes/` directory); they remain in git history but are no longer
  tracked

## 0.1.0 - 2023-12-21
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[Unreleased]: https://sourcehost.site/your-name/cim_portfolio/compare/0.1.1...HEAD
[0.1.1]: https://sourcehost.site/your-name/cim_portfolio/compare/0.1.0...0.1.1
