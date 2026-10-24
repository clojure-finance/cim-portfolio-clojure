# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## Unreleased
### Added
- Performance Metrics shows the portfolio's annualized Sharpe ratio: the latest
  point of the default 1-year-window EWMA Sharpe series, so the summary figure
  always matches the chart's last value. Shown as n/a until 21+ daily returns
  exist or while the portfolio value never moved (zero volatility)
- Performance Metrics shows the portfolio's annualized CAPM alpha against the
  S&P 500 since inception (`cim_portfolio.regression/portfolio-alpha-beta`):
  daily portfolio returns (cash included) regressed on `^GSPC` returns over
  their common trading dates, intercept × 252. Shown as n/a with fewer than
  21 common daily returns or while the portfolio value was negative
- The results page marks the 1-Year Cumulative Portfolio Return (both the summary
  metric and the weekly comparison card) with a note when the portfolio's history
  is shorter than a year: the window is clamped to inception, so the figure is a
  since-inception return rather than a full 1-year one
- A warning card on the results page when a held stock falls 40% or more in one
  day, which usually means an unrecorded corporate action such as a spin-off;
  recording the received shares as a buy at price 0 silences it
- The news form remembers both API keys in the browser (localStorage): they are
  saved on submit and pre-filled on later visits, so they no longer have to be
  retyped every time
- Automatic stock-split adjustment of trade data (`cim_portfolio.corporate-actions`):
  share amounts and user-supplied prices are normalized to post-split units before
  analysis, consistent with Yahoo Finance's split-adjusted price history
  (fixes phantom losses and inverted positions for pre-split trades, e.g. the
  SBS 5:1 split effective 2026-05-07)
- AI news analysis feature: `/news` form page and `/analyze-news` route, with
  article analysis via the DeepSeek and OpenRouter APIs
- 15-field extraction per article: sentiment, tldr, summary, bias, keywords, entities,
  category, target_audience, market_impact, investment_stance, time_sensitivity,
  affected_sectors, key_quote, risk_level, actionable_insight
- Results dashboard with sentiment distribution, market impact bars, and rich article cards
- New CSS components for news UI (hero, provider cards, dashboard grid, article cards, badges)
- Tests for the trade-input validator, the analytics functions, corporate
  actions, the CAPM regression, the news response parsing and the HTML escaping

### Changed
- Build switched from Leiningen to the Clojure CLI (`deps.edn` + tools.build
  `build.clj`): `clojure -M:run` starts the app, `clojure -M:test` runs the
  tests, `clojure -M:nrepl` starts a REPL with the project's dependencies, and
  `clojure -T:build uber` builds the same
  `target/uberjar/cim_portfolio-standalone.jar` as before (all namespaces AOT
  compiled with direct linking). CI, the launcher scripts and the README use
  the Clojure CLI; CI also builds the jar. `run_web_app.sh` is now executable
- Trade rows are normalized before analysis: dates to YYYY-MM-DD (YYYY/MM/DD
  and YYYYMMDD are also accepted), tickers to upper case, and rows sorted by
  date, so trades can be entered in any order
- The Sharpe ratio chart is an EWMA Sharpe ratio: the mean return in the
  numerator uses the same exponential decay as the EWMA volatility in the
  denominator. It previously used a 21-day rolling mean, so numerator and
  denominator had different effective windows, and a large return dropping out
  of the 21-day mean made the ratio jump while the volatility still carried it.
  The default decay is an effective window of 252 trading days (≈ one year,
  `sharpe-ratio-window-days`), converted to alpha = 2 / (window + 1)
  (`ewma-alpha-for-window`), i.e. λ ≈ 0.992; λ = 0.94 is on the toggle
- The EWMA volatility and Sharpe-ratio series are bias-corrected
  (`bias-corrected-ewma`, pandas `ewm(adjust=True)` style): the zero-seeded
  recursion is divided by the accumulated weight 1 − λ^t, so each value is an
  exact weighted mean of the returns so far. Previously the recursion was seeded
  with the first (squared) return, which dominated the early part of the series
- The EWMA Sharpe series is masked (nil, a gap in the chart) until it is based on
  at least 21 daily returns (`sharpe-min-periods`): with fewer the estimate is
  degenerate — at the very first return the ratio is ±√252 ≈ ±15.9 by
  construction, which dwarfed the rest of the chart
- The portfolio time-series charts (value, one-dollar performance, EWMA
  volatility, EWMA Sharpe) pin their x-axis to the full trading-date span, so
  they stay visually aligned: Plotly's autorange skips null points, which would
  have started the masked Sharpe chart's axis a month after the others
- ECB exchange rates are reloaded once a day instead of being loaded once per
  server start
- The `/news` OpenRouter models are `openrouter/free` (routes to whichever free
  model is available) and two current free models; the previous four had all
  been retired by OpenRouter, so selecting OpenRouter failed
- DeepSeek set as default LLM provider
- Libraries: hiccup 1.0.5 → 2.0.0, clj-yfinance 0.1.7 → 0.1.8 (its breaking
  changes don't affect this project: both price fetches already passed
  `:auto-adjust true` and only read `:timestamp`/`:open`/`:close`), clj-time
  replaced by `java.time`

### Fixed
- Spin-offs that Yahoo later books as a split of the parent (CTVA/VYLR,
  2026-10-01, ratio 6.665) are no longer counted twice: the fake split is
  dropped from the share-count adjustment and the parent's pre-spin prices are
  multiplied back to their real level (`spin-offs-booked-as-splits`,
  `separate-spin-offs`, `undo-spin-off-price-adjustment`). Previously pre-spin
  CTVA shares were multiplied by 6.665, so CTVA was valued at ~$83 a share and
  the recommended price-0 VYLR row added the spun-off value a second time
- `/news` with DeepSeek works again: it offered `deepseek-chat` and
  `deepseek-reasoner`, which DeepSeek retired on 2026-07-24. It now offers
  `deepseek-flash` (default) and `deepseek-v4-pro`, with thinking mode turned
  off (DeepSeek enables it by default; it slows replies and isn't needed here)
- Trades entered out of date order were analyzed in input order, which put
  trades on the wrong dates and skewed the value series and every statistic
  built on it
- Dates written as YYYY/MM/DD or YYYYMMDD passed validation but then crashed
  the analysis; a ticker typed in lower case was tracked as a separate position
- The stock-holdings series behind the "Excluding Cash" returns and the
  Portfolio Allocation card valued a holding at $0 on days its exchange was
  closed (e.g. a Hong Kong holiday in a portfolio with US stocks); it now uses
  every holding's trading dates and the last known price
- Per-stock performance charts no longer shift their points against the dates
  when the portfolio mixes exchange calendars
- Every analysis made an extra Yahoo request left over from testing, so a
  failure there failed the whole analysis
- The standalone jar no longer has to be started from a directory containing
  `examples/` (loading a namespace used to read the sample CSV files)
- A failed ECB rate download no longer breaks every currency conversion until
  the server restarts (the cached failure is retried on the next request)
- The portfolio analyzer no longer answers bad input with a raw 500 page (all three
  crash classes were being hit by real visitors, per the VPS journal):
  - trade rows are validated before analysis, with line-numbered messages for
    invalid dates, actions, amounts, tickers, and prices, shown in a banner on the
    form page; a pasted or uploaded CSV header line is recognized and skipped
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
- Building the jar and starting a REPL no longer hang when ECB's rates endpoint
  stalls mid-transfer: ecbjure is bumped to 0.1.5 (adds connect/read timeouts,
  so a stalled fetch throws instead of blocking forever) and the FX converter is
  built on first use, so namespace load / AOT compilation performs no network I/O

### Security
- Pages are rendered with hiccup 2, which HTML-escapes text: trade input echoed
  in error messages, tickers, and news and LLM content were previously inserted
  as raw HTML, so crafted input could run scripts on the page (where the news
  form keeps API keys). Article links are only shown for http(s) URLs
- Article pages are fetched with TLS certificate checks (they were disabled)
- Removed a hardcoded DeepSeek API key from `run_web_app.bat` (it had been committed
  and public on GitHub since May 2026 — the key itself must be revoked/rotated at
  platform.deepseek.com, as it remains visible in git history)
- The starting-cash form field is parsed as a plain number; it previously went
  through `read-string`, which evaluates Clojure reader forms (`#=(...)`) from a
  public, unauthenticated form field

### Removed
- Unused news code: the Clerk news notebook and its forked copy of the news
  namespaces, the command-line news tool, and the article embedding/clustering,
  which appended to `data/*.jsonl` on every `/news` request without ever
  showing the result. Dependencies dropped with it: Clerk, tools.cli,
  data.csv, clj-time
- Leiningen files (`project.clj`, `lein.bat`) and the dummy `pom.xml` left over
  from Heroku
- Docker support: `Dockerfile`, `docker-compose.yml`, and the README's Docker
  quick start. The setup had not worked since at least March 2026 (the image
  deleted `src/` after copying it, expecting a bind mount that
  `docker-compose.yml` never provided)
- Heroku deployment support: `Procfile`, `system.properties`, and the README's
  "Deploying to Heroku" section; the app is deployed as a standalone jar
- Dead code: deprecated portfolio-return functions, the unused 21-day rolling
  volatility series, an unused form parser, and a "Hello World" handler
- The lein-template `doc/intro.md` placeholder ("TODO: write great documentation")
- Stale API-key plumbing that the web app never read: the `NEWSDATA_API_KEY`/
  `DEEPSEEK_API_KEY` env-var instructions in the README and the launcher-script
  warnings about them — the web UI takes both keys from the `/news` form
- The dead `news_llm_newsdata` auto-start block from both launcher scripts: it
  pointed at a sibling project that no longer exists and nothing references
- The Python/yfinance dependency: the embedded Python wrapper in `yfinanceclient.clj`
  and the load-time Python demo code in `regression.clj` were unused legacy paths —
  all production data flows are Clojure-native (clj-yfinance for prices, ecbjure for
  FX, fastmath for CAPM regression). Dropped `libpython-clj`, the Python layers of
  the Docker image, `requirements.txt`/`runtime.txt`, and the
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
- Initial project setup
