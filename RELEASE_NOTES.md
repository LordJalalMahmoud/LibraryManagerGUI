# 📚 LibraryManager v1.4.0 — Smart Library Release 🔎

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.4.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.4.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.4.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.4.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.4.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.4.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.4.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.4.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 What's New in v1.4.0 — Smart Library 🔎

### 🔎 Advanced Multi-Filter Search (Simultaneous Filtering)
- **Simultaneous Criteria Filtering**: Status, category, tag, favorites (❤️), wishlist (🌟), author, and page count boundaries can now all be active and combined simultaneously with boolean AND logic.
- **Collapsible Advanced Search Drawer**:
  - Filter by specific **Author** query.
  - Filter by **Tag** dropdown.
  - Filter by **Page Range** with minimum and maximum page count bounds (`Min Pages` and `Max Pages`).
  - One-click **Reset Filters** button.

### 💾 Saved Searches
- **Save Custom Criteria**: Save current search queries and multi-filter configurations with custom names into SQLite.
- **Quick-Access Dropdown**: Easily recall and apply any saved search configuration with one click.
- **Delete Saved Searches**: Manage and remove obsolete saved searches from the toolbar.

### 👥 Duplicate-Book Detection & Merge Resolution
- **Intelligent Duplicate Discovery**:
  - Automatically scans the library for matching normalized ISBNs.
  - Clustered detection of duplicate titles and authors (with punctuation and whitespace normalization).
- **Proactive Notification Banner**: Alerts when duplicate book clusters are detected with a direct `[Review & Resolve]` action.
- **Dedicated Resolution Dialog**:
  - Visual side-by-side comparison of duplicate copies (reading progress, categories, metadata, added dates).
  - Select which copy to keep with one-click resolution.
  - **Progress & Metadata Merge**: Automatically retains the highest reading progress, moves reading sessions, and merges tags and descriptions into the preserved book copy.
  - **Auto-Resolve All**: 1-click batch duplicate resolution that intelligently keeps the most complete copy.

### ☑️ Bulk Operations
- **Interactive Card Checkboxes**: Selection checkboxes on every book card (`☑ Book 1`, `☑ Book 2`, `☑ Book 3`).
- **Floating Bulk Actions Bar**:
  - Live selection counter (`{count} selected`).
  - `[Select All]` / `[Deselect All]` toggle.
  - `[Mark as Completed]`: Instant batch status update with full progress and completion dates.
  - `[Change Category]`: Mass category assignment with interactive dialog.
  - `[Add Tag]`: Mass tag appending across all selected books without overwriting existing tags.
  - `[Delete]`: Safe bulk deletion with confirmation dialog.

---

## 📜 Previous Releases

### v1.3.0 — Statistics & Reading Analytics
- Monthly reading trend charts (Books, Pages, Reading Time).
- Reading velocity (pages/hour) and total logged duration.
- Year-in-Review summary and Most-Read Authors / Categories leaderboards.

### v1.2.0 — Reading Tracker & Activity Sessions
- Reading session logging with start/end pages, durations, and notes.
- Current and best streak calculations with grace-period logic.
- Configurable daily page goals and annual book challenges.

### v1.1.0 — Advanced Library Organization
- Multi-tagging, category filters, favorites (❤️) and wishlist (🌟).
- Dual theme engine (Light/Dark) and full bilingual Arabic RTL / English LTR support.
