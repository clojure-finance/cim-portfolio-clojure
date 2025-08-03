# cim_portfolio_clojure

A portfolio analysis program written in Clojure.

## Installation

### RUNNING USING DOCKER IMAGE (RECOMMENDED - Tested on Windows, Mac and Linux):

Using the provided Dockerfile to build a Docker image and run a container greatly simplifies the usage of the software. Please install Docker on your system from the official website: https://www.docker.com/

Note: Please place your csv files containing your trades in the examples/ directory before starting.

**Initial Setup**

Please ensure Docker is installed and then run the following commands in the root directory:

1. Build the Docker image: `docker build -t cim-portfolio-env .`
2. Run the container: `docker run -it -p 8990:8990 -v ./src:/app/src:ro -v ./examples:/app/examples:ro --name cim-portfolio-app cim-portfolio-env`

The Clerk Notebook should be accessible on: http://localhost:8990

Load `portfolio.clj` and all the code cells should be automatically executed, please ensure no errors occur. You can edit the source code and your changes will be instantly reflected in the Clerk Notebook. Set your starting cash amount (important as it influences your return %, portfolio volatility etc.), toggle optional displays of statistics etc. Input the relative directory of your trade file in `input-files`.

For e.g., if your directory contains: examples/myTrades.csv, please enter "./examples/myTrades.csv"

Stop the application by running the following command in a new terminal:

`docker stop cim-portfolio-app`

**Subsequent Runs**

After the initial setup, you can rerun the application using the following command:

`docker start cim-portfolio-app`

Stop the application once again by using the command:

`docker stop cim-portfolio-app`

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


## Usage (Running Locally)

<!-- Here's a video tutorial on running the program: [CIM Portfolio Tutorial](https://youtu.be/kpxD8rUBuFk) -->

- Clone the github repository to your local computer.
- Open the cloned folder in your terminal and run `lein repl`.
- If your web browser does not automatically open the URL provided in the terminal, open the URL manually in your browser of choice.
- Under **All Notebooks**, select `src/cim_portfolio/yfinanceclient.clj` and wait for it to load (may take a few seconds).
- Verify that all of the code blocks were run successfully, and that the output shows proper historical price data. If error, flag the issue and try to debug.
- Now, load `src/cim_portfolio/portfolio.clj` and wait for it to load.
- Verify once again that all of the code blocks were run successfully.
- Open `src/cim_portfolio/portfolio.clj` in your favourite code editor, and enter the relative path to your csv file containing all your trades (relative to main directory).
- Check that Clerk has updated the final code block to show an overview of your portfolio performance as well as other statistics (e.g. individual stock performance, etc.) 

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
