#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting CIM Portfolio News Analyzer..."

export PORT="${PORT:-3000}"
export NEWS_APP_URL="${NEWS_APP_URL:-http://localhost:3100}"

NEWS_APP_DIR="$(cd "$SCRIPT_DIR/../news_llm_newsdata" && pwd 2>/dev/null || true)"
if [ -n "$NEWS_APP_DIR" ] && [ -f "$NEWS_APP_DIR/deps.edn" ]; then
    if command -v clj >/dev/null 2>&1; then
        echo "Starting News Analysis server on http://localhost:3100 ..."
        (
            cd "$NEWS_APP_DIR"
            PORT=3100 PORTFOLIO_APP_URL="http://localhost:$PORT" clj -M -m news-llm.server
        ) >/tmp/news_llm_server.log 2>&1 &
        echo "News server logs: /tmp/news_llm_server.log"
    else
        echo "[WARNING] clj not found, external news service not started."
    fi
else
    echo "[WARNING] news_llm_newsdata project not found."
fi

if [ -z "$NEWSDATA_API_KEY" ]; then
    echo "[WARNING] NEWSDATA_API_KEY is not set."
fi

if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "[WARNING] DEEPSEEK_API_KEY is not set."
fi

lein run
