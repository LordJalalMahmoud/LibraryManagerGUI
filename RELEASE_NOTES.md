# 📚 LibraryManager v1.3.0 — Statistics & Analytics Release 📊

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.3.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.3.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.3.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.3.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.3.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.3.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.3.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.3.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 What's New in v1.3.0 — Statistics 📊

### 📈 Monthly Reading Trends (Interactive Chart)
- **Books Read per Month**: Visual monthly breakdown of finished books across the year.
- **Pages Read per Month**: Aggregated page counts per month driven by logged reading sessions.
- **Reading Time per Month**: Dedicated view for minutes logged each month.
- **Interactive View Segmenter**: Toggle seamlessly between Pages, Books, and Reading Time with instant animated chart re-renders.

### ⏱️ Reading Time & Velocity
- **Total Reading Time KPI**: Displays all-time and annual logged reading duration formatted cleanly in hours and minutes (e.g., `42h 15m` / `42 س و 15 د`).
- **Average Reading Speed Metric**: Accurate velocity calculation (pages per hour) computed from timed reading sessions.

### 🏆 Yearly Reading Summary (Year in Review)
- **Interactive Year Selector**: Select any active year with recorded readings or collection additions.
- **Executive KPI Grid**:
  - Books Finished vs. Annual Reading Goal with live progress bar and completion percentage.
  - Total Pages Read in the selected year.
  - Total Reading Time in the selected year.
  - Average Reading Speed in the selected year.
  - Top Author of the year.
  - Top Category of the year.

### 👥 Most-Read Authors Leaderboard & Categories Distribution
- **Top Authors Leaderboard**: Ranked leaderboard (#1 Gold, #2 Silver, #3 Bronze) with book completion counts, total pages read, and proportional volume progress bars.
- **Categories Distribution**: Interactive visual breakdown showing collection volume and percentage per genre/category.

---

## 📜 Previous Releases

### v1.2.0 — Reading Tracker & Activity Sessions
- Reading session logging with start/end pages, durations, and notes.
- Current and best streak calculations with grace-period logic.
- Configurable daily page goals and annual book challenges.

### v1.1.0 — Advanced Library Organization
- Multi-tagging, category filters, favorites (❤️) and wishlist (🌟).
- Dual theme engine (Light/Dark) and full bilingual Arabic RTL / English LTR support.
