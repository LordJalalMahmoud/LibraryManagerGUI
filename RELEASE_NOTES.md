# 📚 LibraryManager v1.0.0 — Production Release

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.0.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.0.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.0.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.0.0.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.0.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.0.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.0.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.0.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🔒 Verification & Security
Each release includes a `SHA256SUMS.txt` file containing checksums for every artifact to verify file integrity.

```bash
# Verify checksum on Linux/macOS:
sha256sum -c SHA256SUMS.txt
```

---

## 🌟 Key Features

- **Dynamic Analytics Dashboard**: Instant metrics, reading percentage progress, interactive pie charts and bar charts.
- **University Course Chapters**: Track books chapter-by-chapter with interactive checkboxes and page ranges.
- **Real-Time Search & Sorting**: Live debounced search across titles and authors with multiple sort options.
- **Arabic & English Localization**: Instant language switcher with full RTL (Right-to-Left) UI support.
- **Modern UI & Themes**: Custom styled components, animated counting, light/dark themes, and non-blocking toast notifications.
- **Data Protection**: Automated SQLite database backup, export to JSON, and full restore capabilities.
