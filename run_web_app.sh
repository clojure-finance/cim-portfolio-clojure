#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting CIM Portfolio News Analyzer..."

export PORT="${PORT:-3000}"

lein run
