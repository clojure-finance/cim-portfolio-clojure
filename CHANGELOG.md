# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Added
- Automatic stock-split adjustment of trade data (`cim_portfolio.corporate-actions`):
  share amounts and user-supplied prices are normalized to post-split units before
  analysis, consistent with Yahoo Finance's split-adjusted price history
  (fixes phantom losses and inverted positions for pre-split trades, e.g. the
  SBS 5:1 split effective 2026-05-07)
- `CIM_PORTFOLIO_PYTHON` environment variable to configure the Python interpreter
  path (previously hardcoded), and `CIM_PORTFOLIO_LIBPYTHON` to pin the matching
  libpython shared library (needed e.g. for pyenv installs, where the system
  libpython would otherwise be loaded and break C-extension imports)
- AI news analysis feature: `/news` form page and `/analyze-news` route
- LLM-powered article analysis via DeepSeek and OpenRouter APIs
- 15-field extraction per article: sentiment, tldr, summary, bias, keywords, entities,
  category, target_audience, market_impact, investment_stance, time_sensitivity,
  affected_sectors, key_quote, risk_level, actionable_insight
- Results dashboard with sentiment distribution, market impact bars, and rich article cards
- New CSS components for news UI (hero, provider cards, dashboard grid, article cards, badges)

### Changed
- DeepSeek set as default LLM provider and model (deepseek-chat)
- Default output directory for CLI mode changed from hardcoded path to `reports/`

### Fixed
- Sell orders now value the daily PnL series at closing prices, matching buy orders
  (previously opening prices, which skewed portfolio value and returns on days with
  sell trades); trades themselves still execute at the open
- Native price fetching (clj-yfinance) is dividend-adjusted again (`:auto-adjust`),
  restoring parity with the Python wrapper's `auto_adjust=True`
- Native `convert-currency` crashed with a ClassCastException when trades carried a
  user-supplied price (the CSV string reached ecbjure's `fx/convert` uncoerced; the
  Python wrapper used to coerce with `float()`)

### Removed
- Backup and disabled source files (core_backup.clj, debug_scraper.clj.disabled, etc.)
- Broken server files with missing dependencies
- Experimental macroexpand-demos directory

## [0.1.1] - 2023-12-21
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2023-12-21
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[Unreleased]: https://sourcehost.site/your-name/cim_portfolio/compare/0.1.1...HEAD
[0.1.1]: https://sourcehost.site/your-name/cim_portfolio/compare/0.1.0...0.1.1
