# 📚 LibraryManager v1.7.0 — Reading Experience & Interactive Companion 📖

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

All installers and standalone packages below include a **bundled, minimal custom Java Runtime Environment (JRE)**. **No Java installation is required on your computer.**

---

## 🚀 Downloads by Operating System (v1.7.0)

### 🐧 Linux (Ubuntu, Debian, Fedora, Arch, etc.)
- **Debian / Ubuntu Package**: [`library-manager_1.7.0_amd64.deb`](#)
  - *Install with:* `sudo apt install ./library-manager_1.7.0_amd64.deb`
  - Automatically adds `LibraryManager` to your system application menu and desktop.
- **Portable Linux Bundle**: [`LibraryManager-1.7.0-linux-x64.tar.gz`](#)
  - *Run with:* extract and launch `./LibraryManager/bin/LibraryManager`

### 🪟 Windows (Windows 10 / 11)
- **Windows Installer**: [`LibraryManager-1.7.0-windows-x64.msi`](#)
  - Standard Windows MSI wizard with desktop icon and Start Menu entry.
- **Portable Windows ZIP**: [`LibraryManager-1.7.0-windows-x64.zip`](#)
  - Extract and run `LibraryManager.exe` without installation.

### 🍏 macOS (macOS 12 Monterey or later)
- **Apple Disk Image**: [`LibraryManager-1.7.0.dmg`](#)
  - Standard macOS drag-and-drop installer into `/Applications`.
- **Apple Package Installer**: [`LibraryManager-1.7.0.pkg`](#)
  - Guided step-by-step installer for macOS.
- **Standalone Application Archive**: [`LibraryManager-1.7.0-macos.tar.gz`](#)
  - Standalone `LibraryManager.app` bundle.

---

## 🌟 What's New in v1.7.0 — Reading Experience 📖

### ⏱️ Active Reading Companion & Stopwatch Timer
- **Live Stopwatch**: High-precision stopwatch ticker with hours/minutes/seconds formatted cleanly (`00:00:00` or `MM:SS`).
- **Interactive States**:
  - `▶ Start Session`: Begins active reading timer with smooth tick animation.
  - `⏸ Pause`: Pauses the timer and records intermediate state.
  - `▶ Resume`: Resumes elapsed time counting from where you paused.
  - `⏹ Finish Session`: Completes the reading session, saves to database, auto-advances the book's current page, triggers streak recalculation, and delivers celebratory feedback.
- **Keyboard Control**: Toggle Pause / Resume instantly with the **Spacebar**; dismiss or minimize with **Escape**.

### ⚡ Live Reading Speed (Pages Per Minute / PPM)
- **Real-Time Speed Calculation**: Dynamically measures reading velocity in pages per minute (`pagesRead / elapsedMinutes`).
- **Contextual Comparisons**: Compares live session pace against your historical average for this specific book or all-time reading speed.
- **Speed Milestones**: Highlights fast reading pace (`🔥 Fast Pace` badge for speeds >= 1.5 PPM).

### ⏳ Estimated Time to Finish (ETA Projection)
- **Dynamic Horizon**: Projects remaining reading duration (`1h 35m`, `45m`) based on remaining unread pages (`totalPages - currentPage`) and reading velocity.
- **Real-Time Recalculation**: Automatically adjusts ETA as you turn pages and as reading speed varies.
- **Celebration Cue**: Displays `Completed! 🎉` when the final page is reached.

### 🎯 Daily Target & Book Progress Tracker
- **Today's Reading**: Shows pages read today specifically for the active book (e.g. `23 pages`).
- **Goal Fulfillment**: Live indicator against global daily target (e.g. `23 / 25 pages (92%)`).
- **Interactive Page Steppers**: One-click quick increments (`-1`, `+1`, `+5`, `+10`, `+25`, or direct input) and a `Finish Book` shortcut button.

### 📈 Progress Timeline & Chronological Reading History
- **Velocity Overview Bar**: Prominently displays 4 core KPIs for each book:
  - Total Time Spent Reading
  - Average Reading Speed (PPM)
  - Estimated Time to Finish
  - Pages Read Today
- **Connected Milestone Timeline**:
  - Connected vertical track pins (`.timeline-pin`, `.timeline-connector`).
  - Milestone icons: 🚀 Started Reading, 🎯 25%, 🌟 50%, ⚡ 75%, 🏆 Book Completed, 🔥 Fast Pace.
  - Chronological activity cards with dates, pages read, session duration, speed, study notes, and safe deletion.

### 🚀 One-Click Start & Global Shortcuts
- **Direct Card Launcher**: Quick `[▶]` timer button on cards in Library grid and Dashboard for books currently in progress.
- **Global Shortcut**: Press `Ctrl + R` anywhere in the app to immediately start an active reading session for your current book!

---

## 📜 Previous Releases

### 🎨 v1.6.0 — UX & Accessibility Release

### ⌨️ Comprehensive Keyboard Shortcuts Engine
- **Global Application Accelerators**:
  - `Ctrl + N`: Instant Add New Book modal dialog.
  - `Ctrl + F`: Fast search navigation and focus in Library view.
  - `Ctrl + 1` to `Ctrl + 5`: Quick-switch between Dashboard, All Books, Currently Reading, Completed, and Data Management.
  - `Ctrl + ,`: Instant settings navigation.
  - `Ctrl + D`: Quick theme toggle (Dark / Light / High Contrast).
  - `Ctrl + B`: Instant one-touch manual database backup with timestamp.
  - `Ctrl + Shift + D`: Open Dashboard Customizer dialog.
  - `F1` or `Ctrl + /`: Interactive Keyboard Shortcuts Cheat Sheet modal dialog.
  - `Esc`: Clear search filters or dismiss open modals.
- **Cheat Sheet Dialog**: Interactive dialog displaying all shortcuts organized by category with stylized keyboard key badges (`.key-badge`).

### 📂 Drag & Drop to Add Books & Covers
- **Visual Drag Overlay**: Dropping files over the application window displays an elegant, translucent dashed overlay (`.drag-overlay`) with localized prompts.
- **Smart File Detection**:
  - **Book Files (`.pdf`, `.epub`, `.mobi`, `.txt`, `.azw3`)**: Automatically extracts title, format, and path. Dropping a single book opens the book form pre-filled; dropping multiple books batch-adds them directly to the backlog.
  - **Cover Images (`.png`, `.jpg`, `.jpeg`, `.webp`)**: Automatically binds image as the book cover in the book dialog.
  - **Backup Snapshots (`.json`)**: Detects exported library backups and prompts to import or merge.

### ✨ Fluid Animations & Micro-Interactions
- **Slide & Fade View Transitions**: Smooth directional sliding entrance (`AnimationUtil.slideFadeIn`) when transitioning between navigation sections.
- **Staggered Card Entrances**: Sequential cascade animations (`staggerFadeIn`) for book cards and session feeds.
- **Card Hover Elevation**: Subtle lift (`translateY -3px`) and shadow escalation on book cards.
- **Form Validation Shake**: Responsive horizontal shake micro-interaction (`AnimationUtil.shake`) on save buttons when input validation errors are present.
- **Action Pulse Feedback**: Micro-scaling pulses (`AnimationUtil.pulse`) for interactive toggles and confirmation buttons.

### 🎛️ Customizable Modular Dashboard
- **Interactive Section Reordering**: Reorder dashboard sections using Up (▲) and Down (▼) buttons in the layout customization dialog (`DashboardCustomizationDialog`).
- **Section Visibility Toggles**: Choose exactly which widgets to show or hide:
  - KPI Overview Cards (`METRICS`)
  - Year in Review Summary (`YEARLY`)
  - Monthly Reading Trends & Charts (`CHARTS`)
  - Reading Goals & Daily Habits (`GOALS`)
  - Currently Reading Feed (`CURRENTLY_READING`)
  - Recent Reading Sessions (`RECENT_SESSIONS`)
  - Recently Added Books (`RECENT_BOOKS`)
- **Persistent Layout in SQLite**: Custom dashboard order and section visibility are stored and automatically restored across app restarts.
- **One-Click Reset**: Instant reset button to restore default layout anytime.

### 📐 Window Size & Position Memory
- **Automatic Geometry Persistence**: Remembers window width, height, (x, y) screen coordinates, and maximized state across application sessions.
- **Multi-Monitor Safe Restoration**: Checks saved window coordinates against all active displays (`Screen.getScreens()`) to prevent windows from opening off-screen if an external monitor was disconnected.

### ♿ Accessibility & Visual Inclusion (WCAG AAA)
- **High Contrast Theme**: High-contrast color palette (`theme-high-contrast.css`) featuring deep blacks, pure whites, and vivid yellow `#facc15` accents for enhanced readability.
- **Dynamic Font Size Scaling**: Choose between **Normal (100%)**, **Large (115%)**, and **Extra Large (130%)** with live UI adaptation across all views.
- **Reduce Motion Setting**: Accessibility toggle that instantaneously disables or minimizes all animations and transitions across the entire application for motion-sensitive users.
- **High-Visibility Keyboard Focus Rings**: Prominent 2px focus indicators on all buttons, text fields, checkboxes, and navigation items for full keyboard navigation.

---

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
