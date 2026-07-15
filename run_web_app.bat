@echo off
setlocal
echo Starting CIM Portfolio News Analyzer...

set PORT=3000

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
