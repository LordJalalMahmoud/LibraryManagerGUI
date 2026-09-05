# 📚 LibraryManager v1.1.0 — Major Feature & Design Release

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.1.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.1.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.1.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.1.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.1.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.1.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.1.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.1.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 What's New in v1.1.0

### 🏷️ Advanced Library Organization System
- **Categories & Tags**: Categorize books and assign multi-tags (`#fiction`, `#history`, etc.) with dedicated filtering.
- **Publishers & ISBN**: Extended book metadata tracking with ISBN length validation.
- **Favorites & Wishlist**: Quick one-click heart (❤️) and star (🌟) toggling on book covers, library filters, and details views.
- **Multi-Criteria Filter Toolbar**: Real-time category selector, search bar with instant clear button, and filter pills.

### 🎨 Complete UI/UX Redesign & Modern Design System
- **Modern Design Tokens**: Layered surface elevations, soft ambient shadows, and pastel status chips for Light and Dark modes.
- **Brand Identity & Polish**: Redesigned sidebar branding with squircle icon badge and smooth interactive navigation.
- **Enhanced Dashboard**: Modern KPI stat cards with colored icon badges, customized borderless charts, and spotlight reading cards.
- **Themed Dialogs**: Fully themed confirmation alerts and dialogs matching active Dark/Light themes.
- **Bilingual Parity**: Symmetrical layouts across English (LTR) and Arabic (RTL).

