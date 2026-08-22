# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

This branch is the Clerk-notebook variant of cim_portfolio (see the
`web-application` branch for the Ring web app, which keeps its own change log).

## [Unreleased]
### Added
- Automatic stock-split adjustment of trade data (`cim_portfolio.corporate-actions`):
  share amounts and user-supplied prices are normalized to post-split units before
  analysis, consistent with Yahoo Finance's split-adjusted price history (fixes
  phantom losses and inverted positions for pre-split trades), with unit tests
  for the adjustment layer
- Malformed trade rows in input CSVs are rejected with a clear error instead of
  failing later in the analysis

### Changed
- clj-yfinance bumped 0.1.7 → 0.1.8. Its breaking changes (`:auto-adjust` now
  defaults to true, `:adjusted` removed, `:adj-close` always present) do not affect
  this project: both price fetches already pass `:auto-adjust true` explicitly and
  only read `:timestamp`/`:open`/`:close`, so the fetched series is unchanged
- Market data and split events are fetched natively via
  [clj-yfinance](https://github.com/clojure-finance/clj-yfinance) and currency
  conversion uses ECB rates via
  [ecbjure](https://github.com/clojure-finance/ecbjure); the Python/yfinance
  dependency (libpython-clj and the `CIM_PORTFOLIO_PYTHON`/`CIM_PORTFOLIO_LIBPYTHON`
  env vars) is removed entirely
- Build artifacts (`target/`, `classes/`, `docs/`, jars) are no longer tracked in git

### Fixed
- `analyze-portfolio` throws a descriptive error when a ticker has no price data
  (typo, delisted symbol, or future-dated trade) instead of a NullPointerException
  on the nil price; `util/std-dev` returns 0.0 for inputs with fewer than two
  points instead of dividing by zero (a portfolio younger than three trading days
  crashed the volatility calculation); `calculate-annualized-return` returns nil
  for periods shorter than one day instead of dividing by zero, and the notebook
  prints "n/a — not enough history" for it
- `lein uberjar` and `lein repl` no longer hang when ECB's rates endpoint stalls
  mid-transfer: ecbjure is bumped to 0.1.5 (adds connect/read timeouts, so a
  stalled fetch throws instead of blocking forever) and the FX converter in
  `yfinanceclient.clj` is built lazily (a `delay` derefed at the call sites)
- Sell orders now value the daily PnL series at closing prices, matching buy
  orders; native price fetching is dividend-adjusted (`:auto-adjust`), restoring
  parity with the old Python wrapper's `auto_adjust=True`
- `lein test` is green again: the lein-template boilerplate `core_test.clj`
  (with its deliberate `(is (= 0 1))` failure) is removed — the real tests live
  in `corporate_actions_test.clj`

### Removed
- The lein-template `doc/intro.md` placeholder ("TODO: write great documentation")
- `test/.DS_Store` from version control (already gitignored)
