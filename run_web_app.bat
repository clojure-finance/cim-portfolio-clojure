@echo off
setlocal
echo Starting CIM Portfolio News Analyzer...

if not defined PORT set PORT=3000

REM On Windows the Clojure CLI is a PowerShell module: https://clojure.org/guides/install_clojure
powershell -NoProfile -Command "if (Get-Command clojure -ErrorAction SilentlyContinue) { clojure -M:run; exit $LASTEXITCODE } else { Write-Host '[ERROR] The Clojure CLI was not found. Install it: https://clojure.org/guides/install_clojure'; exit 1 }"
