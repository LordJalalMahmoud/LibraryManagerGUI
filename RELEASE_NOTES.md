# 📚 LibraryManager v1.5.0 — Data Management Release 💾

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.5.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.5.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.5.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.5.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.5.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.5.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.5.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.5.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 What's New in v1.5.0 — Data Management 💾

### 📥 Full Library JSON Export & Import
- **Complete Library Serialization**: Exports all books, chapters, reading sessions, saved searches, and user configuration settings into human-readable, pretty-printed JSON (`gson:2.10.1`).
- **Flexible Import Modes**:
  - **Merge with Existing**: Matches existing books by ISBN or Title+Author, advances reading progress, updates missing metadata, adds new books, and maps foreign keys for chapters and reading sessions.
  - **Replace All Data**: Cleanly wipes existing tables and restores the exact library snapshot with confirmation safeguards.
- **Automatic Pre-Import Safety Snapshot**: Automatically creates a restore point snapshot before performing any import so you can instantly revert if needed.

### 📊 Excel/Numbers Friendly CSV Export
- **Spreadsheet Compatibility**: Exports entire book collection with UTF-8 BOM (`\uFEFF`) header ensuring seamless Arabic and international character rendering in Microsoft Excel, Apple Numbers, and LibreOffice Calc.
- **Standard RFC 4180 Escaping**: Handles commas, newlines, and double quotes cleanly across titles, authors, notes, and tags.

### ⏰ Automatic Scheduled Backups
- **Background Daemon Worker**: Automated background backup executor running periodically without blocking the UI thread.
- **Configurable Frequencies**: Choose between `Daily (Every 24h)`, `Weekly (Every 7 days)`, or `On Application Startup`.
- **Smart Retention Limits**: Configurable retention limits (keep latest 3, 5, 10, 20, or 50 backups) with automatic pruning of old automated snapshots while permanently safeguarding manual backups and restore points.

### 📜 Backup History & Instant Rollback
- **Comprehensive History Table**: Live list of all backups and snapshots with file names, categorized badges (`Manual`, `Scheduled Auto`, `Restore Point`, `Safety Snapshot`), formatted sizes, relative timestamps, and custom descriptions.
- **One-Click Actions**:
  - `[Restore]`: Instant database rollback with automatic safety snapshots before replacement.
  - `[Export]`: Save backup file copies to external drives or custom directories.
  - `[Delete]`: Safe removal of outdated backup snapshots with metadata cleanup.

### 🛡️ Restore Points
- **On-Demand Snapshots**: Create custom restore points with personalized notes and descriptions before major library updates.
- **Automated Defensive Snapshots**: Automatic restore point generation before dangerous operations (JSON import, database restore, library resets).

### 🩺 Database Integrity Diagnostics & VACUUM Defragmentation
- **SQLite PRAGMA Diagnostics**: One-click health check running `PRAGMA integrity_check` and `PRAGMA foreign_key_check`.
- **Database Metrics**: Live reporting on page count, page size (4096 bytes), and unused reclaimable freelist pages.
- **Defragment & Optimize**: Interactive `[Optimize & Vacuum]` button executing SQLite `VACUUM` and `PRAGMA optimize` to defragment storage and reclaim free space.

---

## 📜 Previous Releases

### v1.4.0 — Smart Library
- Advanced multi-filter search drawer with simultaneous criteria combination.
- Saved searches management.
- Duplicate-book detection and merge resolution.
- Bulk operations toolbar with card checkboxes.

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
