@echo off
setlocal
echo Starting CIM Portfolio News Analyzer...

set PORT=3000
set NEWS_APP_URL=http://localhost:3100

set NEWS_APP_DIR=%~dp0..\news_llm_newsdata
if exist "%NEWS_APP_DIR%\deps.edn" (
    where clj >nul 2>nul
    if errorlevel 1 (
        echo [WARNING] 'clj' not found in PATH. News app server was not auto-started.
        echo          Install Clojure CLI or run the news app manually.
    ) else (
        echo Starting News Analysis Web server on http://localhost:3100 ...
        start "News Analysis Server" powershell -NoExit -Command "Set-Location '%NEWS_APP_DIR%'; $env:PORT='3100'; $env:PORTFOLIO_APP_URL='http://localhost:3000'; clj -M -m news-llm.server"
    )
) else (
    echo [WARNING] news_llm_newsdata project not found at: %NEWS_APP_DIR%
)

if "%NEWSDATA_API_KEY%"=="" (
    echo [WARNING] NEWSDATA_API_KEY environment variable is not set. 
    echo AI analysis will fail if you do not set it.
)
if "%DEEPSEEK_API_KEY%"=="" (
    echo [WARNING] DEEPSEEK_API_KEY environment variable is not set. 
    echo AI analysis will fail if you do not set it.
)

REM Check for lein in PATH
where lein >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Found global lein, running...
    lein run
) else (
    if exist lein.bat (
        echo Leiningen not found in PATH, using local lein.bat...
        
        REM Check if jar exists, if not, try self-install
        if not exist "%USERPROFILE%\.lein\self-installs\leiningen-2.12.0-standalone.jar" (
             echo Downloading Leiningen JAR...
             call lein.bat self-install
        )
        
        call lein.bat run
    ) else (
        echo [ERROR] 'lein' not found in PATH and local 'lein.bat' is missing.
        echo Please ensure Leiningen is installed or download lein.bat.
        exit /b 1
    )
)

