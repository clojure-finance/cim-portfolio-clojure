#!/bin/bash
echo "Starting CIM Portfolio News Analyzer..."

if [ -z "$NEWSDATA_API_KEY" ]; then
    echo "[WARNING] NEWSDATA_API_KEY is not set."
fi

if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "[WARNING] DEEPSEEK_API_KEY is not set."
fi

lein run
