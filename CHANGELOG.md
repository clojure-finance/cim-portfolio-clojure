# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Added
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
