#!/bin/bash
# News Analysis Tool - Bash Script
# Usage: ./run_news.sh [OPTIONS]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

# Default parameters
COUNTRY='us'
LANGUAGE='en'
MAX_ARTICLES=1
FORMAT='txt'
OUTPUT_DIR='dynamic report'
DELAY=1000
MODEL='meta-llama/llama-3.3-70b-instruct:free'
SYMBOL=''
QUERY=''

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -c|--country) COUNTRY="$2"; shift 2 ;;
    -l|--language) LANGUAGE="$2"; shift 2 ;;
    -n|--max-articles) MAX_ARTICLES="$2"; shift 2 ;;
    -f|--format) FORMAT="$2"; shift 2 ;;
    -o|--output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    -d|--delay) DELAY="$2"; shift 2 ;;
    -m|--model) MODEL="$2"; shift 2 ;;
    -s|--symbol) SYMBOL="$2"; shift 2 ;;
    -q|--query) QUERY="$2"; shift 2 ;;
    -h|--help) 
      echo 'News Analysis Tool'
      echo 'Usage: ./run_news.sh [OPTIONS]'
      echo 'Options:'
      echo '  -c, --country COUNTRY          Country code (default: us)'
      echo '  -l, --language LANGUAGE        Language code (default: en)'
      echo '  -n, --max-articles NUM         Number of articles (default: 1)'
      echo '  -f, --format FORMAT            Output format: txt, json, csv, md (default: txt)'
      echo '  -o, --output-dir DIR           Output directory'
      echo '  -d, --delay MS                 Request delay in ms (default: 1000)'
      echo '  -m, --model MODEL              LLM model (default: meta-llama/llama-3.3-70b-instruct:free)'
      echo '  -s, --symbol SYMBOL            Stock symbol or search query (e.g., Apple, AAPL)'
      echo '  -q, --query QUERY              Custom search query for news'
      echo '  -h, --help                     Show this help message'
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [ -z "$NEWSDATA_API_KEY" ]; then
  echo "✗ NEWSDATA_API_KEY is not set."
  echo "  Please export it first, e.g.: export NEWSDATA_API_KEY='your_key'"
  exit 1
fi

if [ -z "$OPENROUTER_API_KEY" ]; then
  echo "✗ OPENROUTER_API_KEY is not set."
  echo "  Please export it first, e.g.: export OPENROUTER_API_KEY='your_key'"
  exit 1
fi

export NEWSDATA_API_KEY
export OPENROUTER_API_KEY

echo "🚀 Starting News Analysis Tool..."
if [ -n "$SYMBOL" ] || [ -n "$QUERY" ]; then
  SEARCH_TERM="${SYMBOL:-$QUERY}"
  echo "   Search: $SEARCH_TERM | Language: $LANGUAGE | Articles: $MAX_ARTICLES | Format: $FORMAT"
else
  echo "   Country: $COUNTRY | Language: $LANGUAGE | Articles: $MAX_ARTICLES | Format: $FORMAT"
fi
echo

cd "$PROJECT_DIR"
CLOJURE_ARGS="-c \"$COUNTRY\" -l \"$LANGUAGE\" -n \"$MAX_ARTICLES\" -f \"$FORMAT\" -o \"$OUTPUT_DIR\" -d \"$DELAY\" -m \"$MODEL\""
if [ -n "$SYMBOL" ]; then
  CLOJURE_ARGS="$CLOJURE_ARGS -s \"$SYMBOL\""
fi
if [ -n "$QUERY" ]; then
  CLOJURE_ARGS="$CLOJURE_ARGS -q \"$QUERY\""
fi
eval "clj -M -m news-llm.core $CLOJURE_ARGS"

echo
echo "✓ Analysis complete!"
echo "📁 Reports saved to: $OUTPUT_DIR"
