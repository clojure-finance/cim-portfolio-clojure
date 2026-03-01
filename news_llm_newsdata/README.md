# 📰 News LLM Analysis Tool

An intelligent news analysis tool built with Clojure that uses LLMs (Large Language Models) to perform multi-dimensional analysis including summarization, sentiment analysis, keyword extraction, and more.

---

## 📋 Table of Contents

- [Features](#features)
- [System Requirements](#system-requirements)
- [Installation Guide](#installation-guide)
- [Configuration](#configuration)
- [Usage](#usage)
- [CLI Parameters](#cli-parameters)
- [Output Formats](#output-formats)
- [Model Configuration](#model-configuration)
- [Troubleshooting](#troubleshooting)
- [Cross-Platform Notes](#cross-platform-notes)
- [Project Structure](#project-structure)
- [Team Collaboration Guide](#team-collaboration-guide)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **News Fetching** | Retrieve real-time news via Newsdata.io API |
| **Content Extraction** | Dual-strategy full-text extraction (Jina Reader + Jsoup) |
| **LLM Analysis** | Summary, sentiment, bias, keywords, entities, classification |
| **Model Fallback** | Free model first, auto-switch to paid when rate-limited |
| **Similarity Detection** | TF-IDF / Doc2Vec embedding similarity matching |
| **Multi-Format Output** | TXT, JSON, CSV, Markdown |
| **Progress Display** | Colored terminal output with progress bar |

---

## 💻 System Requirements

### Required Components

| Component | Minimum Version | Notes |
|-----------|-----------------|-------|
| **Java JDK** | 11+ | OpenJDK 17 or 21 recommended |
| **Clojure CLI** | 1.11+ | `clj` / `clojure` command |

### Optional Components

| Component | Purpose |
|-----------|---------|
| **Python 3.9+** | Doc2Vec embedding service |
| **WSL (Windows)** | Running Clojure on Windows |

### API Key Requirements

| Service | Registration URL | Free Tier |
|---------|------------------|-----------|
| **Newsdata.io** | https://newsdata.io | 200 requests/day |
| **OpenRouter** | https://openrouter.ai | Some models free |

---

## 📦 Installation Guide

### Windows Users

#### Option 1: Using WSL (Recommended)

1. **Install WSL Ubuntu**:
   ```powershell
   wsl --install -d Ubuntu
   ```

2. **Install Java in WSL**:
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk -y
   ```

3. **Install Clojure CLI**:
   ```bash
   curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
   chmod +x linux-install.sh
   sudo ./linux-install.sh
   ```

4. **Verify installation**:
   ```bash
   java -version
   clj -h
   ```

#### Option 2: Native Windows (Complex)

1. Install [Scoop](https://scoop.sh) or [Chocolatey](https://chocolatey.org)
2. Install Java: `scoop install openjdk17`
3. Install Clojure: `scoop install clojure`
4. **Note**: Some scripts may require path separator adjustments

### macOS Users

```bash
# Using Homebrew
brew install openjdk@17
brew install clojure/tools/clojure
```

### Linux Users

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk
curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
chmod +x linux-install.sh
sudo ./linux-install.sh

# Arch Linux
sudo pacman -S jdk17-openjdk clojure
```

---

## ⚙️ Configuration

### 1. Setting Up API Keys

**Never hardcode API keys in code!**

#### Using Environment Variables (Recommended)

**Linux/macOS/WSL**:
```bash
# Temporary (current session)
export NEWSDATA_API_KEY="your_newsdata_key"
export OPENROUTER_API_KEY="your_openrouter_key"

# Permanent (add to ~/.bashrc or ~/.zshrc)
echo 'export NEWSDATA_API_KEY="your_key"' >> ~/.bashrc
echo 'export OPENROUTER_API_KEY="your_key"' >> ~/.bashrc
source ~/.bashrc
```

**Windows PowerShell**:
```powershell
# Current session
$env:NEWSDATA_API_KEY = "your_newsdata_key"
$env:OPENROUTER_API_KEY = "your_openrouter_key"

# Permanent (user level)
[System.Environment]::SetEnvironmentVariable("NEWSDATA_API_KEY", "your_key", "User")
[System.Environment]::SetEnvironmentVariable("OPENROUTER_API_KEY", "your_key", "User")
# Restart PowerShell required
```

#### Using .env File

1. Copy template:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env`:
   ```dotenv
   NEWSDATA_API_KEY=your_actual_key
   OPENROUTER_API_KEY=your_actual_key
   ```

3. Load environment variables:
   ```bash
   # Linux/macOS
   source .env
   # Or use direnv for auto-loading
   ```

### 2. Configuring Default Parameters

Edit `default-config` in `src/news_llm/core.clj`:

```clojure
(def default-config
  {:country "us"              ; Country code
   :language "en"             ; Language code
   :max-articles 1            ; Default article count
   :output-format "txt"       ; Output format
   :output-dir "dynamic report"
   :request-delay 1000        ; Request interval (ms)
   :max-retries 2             ; Retry count
   :model "meta-llama/llama-3.2-3b-instruct:free"  ; Free model
   :fallback-model "openai/gpt-4o-mini"            ; Paid fallback
   :embedding-url "http://localhost:8000"
   :similarity-threshold 0.7})
```

---

## 🚀 Usage

### Direct Clojure CLI Usage

```bash
# Basic usage
clj -M -m news-llm.core -n 3 -q "Apple stock"

# Full example
clj -M -m news-llm.core \
  -n 5 \
  -q "artificial intelligence" \
  -f json \
  -o "./reports" \
  -m "qwen/qwen-2.5-72b-instruct:free"
```

### Using Wrapper Scripts

#### Linux/macOS/WSL

```bash
# Grant execute permission (first time)
chmod +x run_news.sh

# Run
./run_news.sh --symbol AAPL --max-articles 3 --format txt

# Show help
./run_news.sh --help
```

#### Windows PowerShell

```powershell
# If execution policy error occurs
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# Run
.\run_news.ps1 -Symbol AAPL -MaxArticles 3 -Format txt
```

### Windows via WSL

```powershell
# Set environment variables
$env:NEWSDATA_API_KEY = "your_key"
$env:OPENROUTER_API_KEY = "your_key"

# Run via WSL
wsl -d Ubuntu -- bash -c "
  export NEWSDATA_API_KEY='$env:NEWSDATA_API_KEY'
  export OPENROUTER_API_KEY='$env:OPENROUTER_API_KEY'
  cd '/mnt/c/path/to/project'
  clj -M -m news-llm.core -n 1 -q 'Tesla'
"
```

---

## 📖 CLI Parameters

| Parameter | Short | Long | Default | Description |
|-----------|-------|------|---------|-------------|
| Country | `-c` | `--country` | `us` | [ISO country code](https://newsdata.io/documentation/#country) |
| Language | `-l` | `--language` | `en` | [ISO language code](https://newsdata.io/documentation/#language) |
| Articles | `-n` | `--max-articles` | `1` | Between 1-10 |
| Format | `-f` | `--format` | `txt` | `txt/json/csv/md` |
| Output Dir | `-o` | `--output-dir` | `dynamic report` | Relative/absolute path |
| Delay | `-d` | `--delay` | `1000` | Request interval (ms) |
| Primary Model | `-m` | `--model` | See config | OpenRouter model |
| Fallback Model | `-b` | `--fallback-model` | `openai/gpt-4o-mini` | Used when rate-limited |
| Embedding URL | `-e` | `--embedding-url` | `localhost:8000` | Doc2Vec service address |
| Similarity | `-t` | `--similarity-threshold` | `0.7` | Between 0-1 |
| Stock Symbol | `-s` | `--symbol` | - | e.g., `AAPL`, `TSLA` |
| Search Query | `-q` | `--query` | - | Custom search query |
| Help | `-h` | `--help` | - | Show help message |

### Common Command Examples

```bash
# Analyze US tech news
clj -M -m news-llm.core -n 5 -c us -l en -q "technology"

# Analyze Chinese finance news
clj -M -m news-llm.core -n 3 -c cn -l zh -q "stock market"

# Use DeepSeek model
clj -M -m news-llm.core -n 2 -q "AI" -m "deepseek/deepseek-chat"

# JSON format output
clj -M -m news-llm.core -n 3 -s NVDA -f json -o "./data"
```

---

## 📄 Output Formats

### TXT Format

```
═══════════════════════════════════════════════════════════════════════
TITLE: Article Title Here
LINK: https://example.com/article
PUBLISHED: 2026-03-01 12:00:00
STATUS: ✓ SUCCESS

━━━ ANALYSIS ━━━
Category: Finance
Sentiment: Positive
Bias: Neutral
Target Audience: Investors
Keywords: keyword1, keyword2
Entities: Company A, Person B

━━━ TL;DR ━━━
Brief one-sentence summary

━━━ SUMMARY ━━━
Detailed summary of the article content...

━━━ SYSTEM INFO ━━━
Similar Articles: Related Article 1 (0.85); Related Article 2 (0.82)
Embedding Source: tfidf-fallback
```

### JSON Format

```json
[
  {
    "title": "Article Title",
    "link": "https://...",
    "published": "2026-03-01 12:00:00",
    "summary": "Detailed summary...",
    "tldr": "Brief summary",
    "sentiment": "Positive",
    "bias": "Neutral",
    "keywords": ["keyword1", "keyword2"],
    "entities": ["Entity1", "Entity2"],
    "category": "Finance",
    "target-audience": "Investors",
    "success": true
  }
]
```

---

## 🤖 Model Configuration

### Recommended Free Models

| Model | Speed | Quality | Limit |
|-------|-------|---------|-------|
| `meta-llama/llama-3.2-3b-instruct:free` | ⚡⚡⚡ | ★★★ | ~20 req/min |
| `qwen/qwen-2.5-72b-instruct:free` | ⚡⚡ | ★★★★ | ~10 req/min |
| `deepseek/deepseek-r1:free` | ⚡ | ★★★★★ | ~5 req/min |
| `meta-llama/llama-3.3-70b-instruct:free` | ⚡⚡ | ★★★★ | ~10 req/min |

### Recommended Paid Models (Fallback)

| Model | Price | Use Case |
|-------|-------|----------|
| `openai/gpt-4o-mini` | ~$0.15/1M tokens | High value fallback |
| `anthropic/claude-3-haiku` | ~$0.25/1M tokens | Quality output |
| `google/gemini-1.5-flash` | ~$0.075/1M tokens | Cheapest option |

### Custom Models

```bash
# Specify via command line
clj -M -m news-llm.core -n 2 -q "news" \
  -m "qwen/qwen-2.5-72b-instruct:free" \
  -b "google/gemini-1.5-flash"
```

---

## ❓ Troubleshooting

### API Issues

#### ❌ `NEWSDATA_API_KEY not set`

**Cause**: Environment variable not set

**Solution**:
```bash
# Linux/macOS/WSL
export NEWSDATA_API_KEY="your_key"

# PowerShell
$env:NEWSDATA_API_KEY = "your_key"
```

#### ❌ `Rate limited (status 429)`

**Cause**: Too many API requests

**Solutions**:
1. Wait 1-2 minutes and retry
2. Reduce `-n` article count
3. Increase `-d` delay: `-d 3000`
4. Fallback model will auto-switch if configured

#### ❌ `LLM error (status 403)`

**Cause**: Invalid API key or insufficient paid credits

**Solutions**:
1. Verify API key is correct
2. Check OpenRouter account balance (for paid models)
3. Switch to free model: `-m "meta-llama/llama-3.2-3b-instruct:free"`

#### ❌ `LLM error (status 404)`

**Cause**: Model name error or discontinued

**Solutions**:
1. Check model name spelling
2. Visit [OpenRouter Models](https://openrouter.ai/models) to confirm availability
3. Switch to another model

### Content Extraction Issues

#### ❌ `Jina failed (Status: 403)`

**Cause**: Website blocked Jina Reader

**Impact**: System auto-switches to fallback strategy (Jsoup)

**If fallback also fails**:
- Article will use description text instead
- Analysis results may be less detailed

#### ❌ `Strategy 2 content too short/empty`

**Cause**: Anti-scraping measures or JavaScript rendering required

**Solution**: No action needed, system continues with available content

### Embedding Service Issues

#### ❌ `Embedding call failed: Connection refused`

**Cause**: Doc2Vec service not running

**Impact**: Auto-fallback to TF-IDF method

**To enable Doc2Vec**:
```bash
cd embeddings
pip install -r requirements.txt
uvicorn service:app --host 0.0.0.0 --port 8000
```

### System Environment Issues

#### ❌ Windows: `clj: command not found`

**Cause**: Windows doesn't natively support Clojure CLI

**Solution**: Use WSL
```powershell
wsl -d Ubuntu -- clj -M -m news-llm.core --help
```

#### ❌ Windows: `cannot be loaded because running scripts is disabled`

**Cause**: PowerShell execution policy restriction

**Solution**:
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

#### ❌ macOS/Linux: `Permission denied`

**Solution**:
```bash
chmod +x run_news.sh
chmod +x run_analysis.sh
```

#### ❌ Output garbled (Unicode characters)

**Cause**: Terminal doesn't support UTF-8 or missing fonts

**Solutions**:
- Windows Terminal supports it by default
- Legacy CMD: `chcp 65001`
- Or use `-f json` for ASCII-only output

### JSON Parsing Issues

#### ❌ `Failed to parse JSON for article`

**Cause**: LLM returned non-JSON format

**Solutions**:
1. System auto-retries
2. Try a different model
3. Article marked as failed but doesn't affect others

---

## 🖥️ Cross-Platform Notes

### Windows

| Issue | Solution |
|-------|----------|
| No Clojure CLI support | Use WSL Ubuntu |
| Path separators | Use `/mnt/c/...` format in scripts |
| Execution policy restriction | `Set-ExecutionPolicy RemoteSigned` |
| Terminal encoding | Use Windows Terminal |

### macOS

| Issue | Solution |
|-------|----------|
| Java version | `brew install openjdk@17` |
| Permission issues | `chmod +x *.sh` |

### Linux

| Issue | Solution |
|-------|----------|
| Missing dependencies | Install JDK and Clojure |
| Permission issues | `chmod +x *.sh` |

### Docker (Optional)

Create `Dockerfile`:
```dockerfile
FROM clojure:openjdk-17-tools-deps
WORKDIR /app
COPY deps.edn .
RUN clj -P
COPY . .
ENTRYPOINT ["clj", "-M", "-m", "news-llm.core"]
```

```bash
docker build -t news-llm .
docker run -e NEWSDATA_API_KEY=xxx -e OPENROUTER_API_KEY=xxx news-llm -n 2 -q "tech"
```

---

## 📁 Project Structure

```
news_llm_newsdata/
├── src/
│   └── news_llm/
│       ├── core.clj           # Main program
│       ├── clustering.clj     # Clustering module
│       └── embeddings.clj     # Embedding module
├── embeddings/
│   ├── service.py             # Doc2Vec FastAPI service
│   ├── train_doc2vec.py       # Training script
│   └── requirements.txt       # Python dependencies
├── data/
│   ├── articles.jsonl         # Raw article storage
│   ├── embeddings.jsonl       # Embedding vector storage
│   └── stories.jsonl          # Story cluster storage
├── dynamic report/            # Analysis report output
├── deps.edn                   # Clojure dependencies
├── run_news.sh                # Linux/macOS script
├── run_news.ps1               # Windows script
├── .env.example               # Environment variable template
├── .gitignore                 # Git ignore rules
└── README.md                  # This document
```

---

## 👥 Team Collaboration Guide

### Getting Started for New Members

1. **Clone repository**
   ```bash
   git clone <repo_url>
   cd news_llm_newsdata
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env and fill in your API keys
   ```

3. **Verify installation**
   ```bash
   clj -M -m news-llm.core --help
   ```

4. **Run test**
   ```bash
   clj -M -m news-llm.core -n 1 -q "test"
   ```

### PR/Merge Checklist

- [ ] API keys are **NOT in code**
- [ ] `.env` file is **NOT committed** (already in .gitignore)
- [ ] `.env.example` is up to date
- [ ] Local smoke test passes (`-n 1` quick test)
- [ ] README reflects all important changes

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature

# Commit changes
git add .
git commit -m "feat: add new feature"

# Push
git push origin feature/your-feature
```

---

## 📝 Changelog

### v2.0.0 (2026-03-01)
- ✨ Added model fallback mechanism (free first → paid fallback)
- ✨ Simplified output format (removed redundant fields)
- ✨ Improved response parsing compatibility
- 🔒 Removed all hardcoded API keys
- 📝 Comprehensive README documentation

### v1.0.0 (Initial Release)
- Basic news fetching and LLM analysis
- Multi-format output support
- Doc2Vec embedding service

---

## 📄 License

MIT License - See LICENSE file for details

---

## 🙏 Acknowledgements

- [Newsdata.io](https://newsdata.io) - News data API
- [OpenRouter](https://openrouter.ai) - LLM routing service
- [Jina AI](https://jina.ai) - Content extraction
- [Clojure](https://clojure.org) - Programming language
