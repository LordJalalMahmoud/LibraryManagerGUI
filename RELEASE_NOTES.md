# 📚 LibraryManager v1.2.0 — Reading Tracker Release

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.2.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.2.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.2.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.2.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.2.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.2.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.2.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.2.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 What's New in v1.2.0

### ⏱️ Reading Tracker & Activity Sessions
- **Session Logging**: Track exact reading sessions with start & end pages, auto-calculated pages read, session duration in minutes, and personal notes.
- **Book Details Integration**: Dedicated `+ Log Session` action and interactive session history card with deletion confirmation.
- **Auto-Sync Reading Progress**: Automatically updates current page and completion status as sessions are recorded.

### 🔥 Reading Habits & Streaks
- **Day Streaks**: Real-time streak tracking showing **Current Streak** and all-time **Best Streak** with grace-period logic.
- **Daily Reading Velocity**: Average pages read per day metric with smart statistical calculation.
- **Reading Goals & Challenges**: Daily page goal widget and Annual Book Challenge progress bar on Dashboard.
- **Configurable Targets**: Adjust daily page targets and annual book completion challenges directly from Settings.

### 🏷️ Advanced Library Organization (from v1.1.0)
- **Categories & Multi-Tagging**: Tag and categorize your collection with real-time filtering pills.
- **Favorites & Wishlist**: One-click heart (❤️) and star (🌟) toggles across library cards and details.
- **Modern Design System**: Sleek elevations, dark/light themes, RTL Arabic support, and zero external runtime dependencies.

