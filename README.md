# cim_portfolio

A portfolio analysis program written in Clojure.

## Installation

Please ensure Java, Clojure, Leiningen are installed prior to running this application.
Please install Python on your system and ensure yfinance is installed.
Please run: `pip install yfinance` to install yfinance

KEY DEPENDENCY: clj-python/libpython-clj
libpython-clj (https://github.com/clj-python/libpython-clj) is a key requirement to run Python code within Clojure.
Python objects are linked to JVM, allowing Clojure to run the server.clj file that enables scraping data from Python's yfinance package.


## Usage

- Clone the github repository to your local computer.
- Open the cloned folder in your terminal and run `lein gorilla`
- Open the URL provided in the terminal in your browser of choice
- Hit **Ctrl G + Ctrl L** (on Mac) or **Alt G + Alt L** (on Linux) and load `src/cim_portfolio/server.clj`
- Run all the code blocks with **Shift + Enter** or **Ctrl + Enter**
- If the output shows proper finance data, load `src/cim_portfolio/portfolio.clj`. If error, flag the issue and try to debug.
- Run all code blocks in portfolio.clj except for the last one.
- Enter the relative path to your csv file containing all your trades (relative to main directory)
- Execute the final code block to get an overview of your portfolio as well as other statistics (such as individual stock performance etc.)


Format of portfolio file (in csv):

Date (YYYY-MM-DD)   |   Action (buy/sell)   |   Number of units bought/sold    |    Ticker

(refer to testPortfolio.csv)

## Options

In src/cim_portfolio/portfolio.clj, the second last code block allows you to set various options within the software.

Modify the `portfolio-options` map to set the starting cash amount (in USD).
Modify the `view-options` map to show/hide various statistics (e.g. the day-by-day value of your portfolio, performance metrics of individuals stocks, the day-by-day cumulative portfolio return)

## Output
This program gives you the most relevant statistics about your portfolio performance.
It displays:
- your current portfolio value (cash + stocks)
- an overview of which securities you hold (number of units as well as their current value)
- cumulative portfolio return (to-date as well as on dates that you have trades)
- portfolio-value from the first day of trades
- statistics about each stock

## Examples

Refer to testPortfolio.csv

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
