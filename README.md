# cim_portfolio_clojure Web Application

A portfolio analysis web application written in Clojure.

### LOCAL DEVELOPMENT:

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


## Deploy Changes

The steps to deploy any changes to the application are as follows:

1. Before committing your changes, run the commands `lein clean`, and `lein uberjar` to generate a Java Executable file `.jar` in `target/`
2. Move the **standalone** `.jar` file to the root directory of the repository, replacing any old `.jar` file that may have existed before.
3. Commit you changes, and push them to GitHub.
4. In the Heroku Application, ensure that the Python and Java buildpacks are enabled. They must be enabled in a certain order, where Python needs to be the first buildpack, while Java is second. If these Buildpacks are not enabled yet, do the commands `heroku buildpacks:clear`, `heroku buildpacks:add heroku/python`, and `heroku buildpacks:add heroku/java`.
5. When all is done, run `git push heroku web-application:main` to clone the repository to a Heroku Remote Branch. (You may have to reset the app first, see below.)

To reset Heroku Application, do the following:

`heroku plugins:install heroku-repo` (if you have not installed the repo add-on)
`heroku repo:reset --app cim-portfolio-clojure`

Command to temporarily stop the Heroku Application from running:

`heroku ps:scale web=0`

Command to turn application back on:

`heroku ps:scale web=1`


## Input

Format of portfolio file (in csv):

Date (YYYY-MM-DD)   |   Action (buy/sell)   |   Number of units bought/sold    |    Ticker

(refer to testPortfolio.csv)


## Output
This program gives you the most relevant statistics about your portfolio performance.
It displays:
- your current portfolio value (cash + stocks)
- an overview of which securities you hold (number of units as well as their current value)
- cumulative portfolio return (to-date as well as on dates that you have trades)
- portfolio-value from the first day of trades
- statistics about each stock


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
