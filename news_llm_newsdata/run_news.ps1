# News Analysis Tool - Windows PowerShell Wrapper
# 用法: .\run_news.ps1 -Country us -Language en -MaxArticles 1 -Format txt

param(
    [string]$Country = "us",
    [string]$Language = "en",
    [int]$MaxArticles = 1,
    [string]$Format = "txt",
    [string]$OutputDir = "dynamic report",
    [int]$Delay = 1000,
    [string]$Model = "meta-llama/llama-3.3-70b-instruct:free",
    [string]$Symbol = "",
    [string]$Query = ""
)

if (-not $env:NEWSDATA_API_KEY) {
    Write-Host "✗ NEWSDATA_API_KEY is not set." -ForegroundColor Red
    Write-Host "  Set it first, e.g.: `$env:NEWSDATA_API_KEY = \"your_key\"" -ForegroundColor Yellow
    exit 1
}

if (-not $env:OPENROUTER_API_KEY) {
    Write-Host "✗ OPENROUTER_API_KEY is not set." -ForegroundColor Red
    Write-Host "  Set it first, e.g.: `$env:OPENROUTER_API_KEY = \"your_key\"" -ForegroundColor Yellow
    exit 1
}

# Get script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Build command
$wslCommand = "export NEWSDATA_API_KEY='${env:NEWSDATA_API_KEY}' ; export OPENROUTER_API_KEY='${env:OPENROUTER_API_KEY}' ; cd '$ScriptDir' ; clj -M -m news-llm.core -c '$Country' -l '$Language' -n '$MaxArticles' -f '$Format' -o '$OutputDir' -d '$Delay' -m '$Model'"

if ($Symbol) {
    $wslCommand = "$wslCommand -s '$Symbol'"
}
if ($Query) {
    $wslCommand = "$wslCommand -q '$Query'"
}

Write-Host "🚀 Starting News Analysis Tool..." -ForegroundColor Cyan
if ($Symbol -or $Query) {
    $term = if ($Symbol) { $Symbol } else { $Query }
    Write-Host "   Search: $term | Language: $Language | Articles: $MaxArticles | Format: $Format" -ForegroundColor Gray
} else {
    Write-Host "   Country: $Country | Language: $Language | Articles: $MaxArticles | Format: $Format" -ForegroundColor Gray
}

# Run in WSL
wsl -d Ubuntu bash -c $wslCommand

Write-Host "`n✓ Analysis complete!" -ForegroundColor Green
Write-Host "📁 Reports saved to: $OutputDir" -ForegroundColor Cyan
