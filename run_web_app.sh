#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting CIM Portfolio..."

export PORT="${PORT:-3000}"

if ! command -v clojure >/dev/null 2>&1; then
    echo "[ERROR] The Clojure CLI was not found. Install it: https://clojure.org/guides/install_clojure"
    exit 1
fi

clojure -M:run
