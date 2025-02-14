# cim_portfolio_clojure

A portfolio analysis program written in Clojure.

## Installation


### RUNNING USING DOCKER IMAGE (RECOMMENDED - Tested on Windows, MacOS, Linux):

Using the provided Dockerfile to build a Docker image and run a container greatly simplifies the usage of the software. Please install Docker on your system from the official website: https://www.docker.com/

Note: Please place your csv files containing your trades in the examples/ directory before starting.

Please ensure Docker is installed and then run the following commands in the root directory:

1. Build the Docker image: `docker build -t cim-portfolio-env .`
2. Run the container: `docker run -it -p 8990:8990 cim-portfolio-env`

The Gorilla REPL should be accessible on: http://localhost:8990/worksheet.html

Ctrl G + Ctrl L (or Alt G + Alt L for Windows) to load worksheets - load `portfolio.clj`. Execute every code cell one-by-one with Shift+Enter, ensuring no errors occur. Set your starting cash amount (important as it influences your return %, portfolio volatility etc.), toggle optional displays of statistics etc. Input the relative directory of your trade file in `input-files`.

For e.g., if your directory contains: examples/myTrades.csv, please enter "./examples/myTrades.csv"

### RUNNING LOCALLY:

Ensure that the following are installed:
- Java
- Clojure (install via homebrew on MacOS)
- Leiningen (install via homebrew on MacOS)
- Python 2/3 + pip 

Run the following command to install the yfinance package and currency converter package:
`pip install yfinance CurrencyConverter`

CurrencyConverter: https://pypi.org/project/CurrencyConverter/
yFinance: https://pypi.org/project/yfinance/

If you're using Python3, run the above command with `pip3` instead on your Terminal.
Verify that these packages are installed and can be run in a python environment.

KEY DEPENDENCY: clj-python/libpython-clj
libpython-clj (https://github.com/clj-python/libpython-clj) is a key requirement to run Python code within Clojure.
Python objects are linked to the JVM, allowing Clojure to run the yfinanceclient.clj file that enables scraping data from Python's yfinance package.


## Usage

Here's a video tutorial on running the program: [CIM Portfolio Tutorial](https://youtu.be/kpxD8rUBuFk)

- Clone the github repository to your local computer.
- Open the cloned folder in your terminal and run `lein gorilla`
- Open the URL provided in the terminal in your browser of choice
- Hit **Ctrl G + Ctrl L** (on Mac) or **Alt G + Alt L** (on Linux) and load `src/cim_portfolio/yfinanceclient.clj`
- Run all the code blocks with **Shift + Enter** or **Ctrl + Enter**
- If the output shows proper historical price data, load `src/cim_portfolio/portfolio.clj`. If error, flag the issue and try to debug.
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
