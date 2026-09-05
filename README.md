# 📚 LibraryManager

### Modern Personal Library & Reading Management Desktop Application
**Java 21** • **JavaFX 21** • **SQLite 3** • **Maven** • **GitHub Actions CI/CD** • **jpackage Native**

[![CI](https://github.com/LordJalalMahmoud/LibraryManagerGUI/actions/workflows/ci.yml/badge.svg)](https://github.com/LordJalalMahmoud/LibraryManagerGUI/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/LordJalalMahmoud/LibraryManagerGUI?color=blue&label=Production%20Release)](https://github.com/LordJalalMahmoud/LibraryManagerGUI/releases)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-3-lightgrey.svg)](https://www.sqlite.org/)
[![Tests](https://img.shields.io/badge/Tests-42%20Passed%20(100%25)-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)]()

> A commercial-grade, cross-platform personal library and reading management system engineered with strict **N-Tier layered architecture (SOLID)**, **automated CI/CD pipelines**, **native self-contained OS packaging**, and **full Arabic (RTL) & English localization**.

<p align="center">
  <img src="src/main/resources/icons/app-icon.png" alt="LibraryManager Icon" width="112" style="border-radius: 20px; box-shadow: 0 8px 24px rgba(0,0,0,0.15);" />
</p>

| ⚡ **Zero-Dependency Native Installers** | 🧪 **100% Automated Test Pass Rate (42 Tests)** | 🌍 **Full Arabic & English RTL Support** | 🏷️ **Categories, Tags, Favorites & Wishlist** |
| :---: | :---: | :---: | :---: |

---

## ⚡ Quick Navigation
- [Download Native Installers](#-zero-dependency-native-downloads)
- [Key Features](#-key-features)
- [Software Architecture](#-software-architecture)
- [Continuous Integration & CD](#-continuous-integration--automation)
- [Developer Setup & Build](#-developer-setup--build-instructions)
- [Codebase Structure](#-codebase-structure)

---

## 📦 Zero-Dependency Native Downloads

Download production-ready native desktop packages that run **without requiring Java or any runtime pre-installed**:

| OS | Format | Direct Download | Description |
| :--- | :--- | :--- | :--- |
| 🐧 **Linux** | Debian `.deb` Package | [`library-manager_1.2.0_amd64.deb`](https://github.com/LordJalalMahmoud/LibraryManagerGUI/releases/latest) | Installs into `/opt/library-manager` with desktop launcher |
| 🐧 **Linux** | Standalone Tarball | [`library-manager-1.2.0-linux-x64.tar.gz`](https://github.com/LordJalalMahmoud/LibraryManagerGUI/releases/latest) | Portable directory, run `./bin/LibraryManager` directly |
| 🪟 **Windows** | Windows MSI Installer | [`LibraryManager-1.2.0-windows-x64.msi`](https://github.com/LordJalalMahmoud/LibraryManagerGUI/releases/latest) | Complete Windows installer with Start Menu & desktop shortcuts |
| 🍏 **macOS** | Standalone `.app` Archive | [`LibraryManager-1.2.0-macos.tar.gz`](https://github.com/LordJalalMahmoud/LibraryManagerGUI/releases/latest) | Standalone `LibraryManager.app` bundle |

---

## ✨ Key Features

### 1. 📊 Real-Time Analytics Dashboard & Reading Tracker (v1.2)
- **Reading Streak Counter**: Real-time streak tracking with flame indicators (`🔥`), today's reading status, and all-time personal best streak memory.
- **Daily Average Velocity**: Automatic computation of average pages read per active reading day.
- **Custom Reading Goals Widget**: Interactive progress tracking for daily page targets (e.g. 25 pages/day) and annual book challenges (e.g. 12 books/year) with celebration cues (`🎉`).
- **Recent Activity Feed**: Centralized overview of the latest reading sessions logged across all books.
- **Live Metric Cards**: Instant tracking of Total Books, Active Reads, Completed Reads, Backlog, and Overall Library Reading Percentage.
- **Interactive Visual Charts**:
  - **Reading Status Distribution**: High-resolution Pie Chart breakdown.
  - **Reading Progress Velocity**: Bar Chart comparing completed pages against remaining unread pages.

### 2. ⏱️ Reading Sessions & Study Reflections (v1.2)
- **Session Logging**: Record date, start page, end page, pages read count, duration in minutes, and personal key takeaways.
- **Auto-Advancing Progress**: Automatically updates the book's current page and reading state (completing the book when target is reached).
- **Session History & Auditing**: Chronological list of past reading sessions per book with delete and confirmation safety.

### 3. 🏷️ Multi-Dimensional Organization & Discovery
- **Categories & Publishers**: Organize by academic or genre classification (e.g. *Software Engineering*, *Programming*, *Architecture*) with category filter dropdown and publisher metadata.
- **Tagging System**: Assign comma-separated tags (`#clean-code`, `#java`, `#architecture`) with elegant badge chips on cards and details.
- **ISBN Cataloging**: International Standard Book Number support with format validation and instant ISBN search lookup.
- **Favorites (❤️) & Wishlist (🌟)**: One-click interactive heart toggle on card covers and dedicated filter pills (`❤️ Favorites`, `🌟 Wishlist`) for instant discovery.
- **Multi-Criteria Search & Filter**: Real-time search across Title, Author, Publisher, ISBN, Category, and Tags with status and category dropdowns.

### 3. 🎓 University Curriculum & Course Chapters
- **Chapter-Level Tracking**: Tailored for college students who read selective chapters rather than whole textbooks.
- **Completion Checkboxes**: Toggle individual chapters complete/incomplete with instant progress re-calculation.
- **Assigned Page Ranges**: Specify custom ranges (`pp. 45 - 80`) and lecture topic notes for each chapter.
- **Dynamic Book Card Badges**: Chapter progress indicators (`3/5 chapters`) appear directly on the library grid cards.

### 3. 🔍 Instant Search, Filter & Multi-Sort
- **Debounced Live Search**: Real-time filtering across titles and authors as you type with zero lag.
- **Quick Status Pills**: One-click filtering by `All Books`, `Reading`, `Completed`, or `Not Started`.
- **Multi-Dimensional Sorting**: Sort instantly by Date Added, Title (A–Z), Author (A–Z), Reading Progress (%), or Total Pages.

### 4. 🌐 Full Arabic (العربية) & English Localization
- **True Bi-directional UI**: Native Right-to-Left (`NodeOrientation.RIGHT_TO_LEFT`) and Left-to-Right layout mirroring.
- **Dynamic Runtime Switcher**: Instant language switching without restarting the application.
- **Persistent Preferences**: Language and theme selections are automatically remembered in SQLite.

### 5. 🎨 Modern Desktop UX & Dual Themes
- **Dark & Light Mode**: Fluid theme toggle with custom CSS variable tokens (`theme-dark.css` and `theme-light.css`).
- **Non-Blocking Toast System**: Floating, animated notifications confirming create, update, delete, and backup operations.
- **Responsive Card Grid**: Auto-adjusts column count dynamically when resizing the window.

### 6. 💾 Database Safety & Portability
- **Standard Desktop Storage**: SQLite database file is stored in the user's home directory (`~/.librarymanager/library.db`), completely separated from the codebase.
- **Backup & Restore**: Single-click database file export and safe restore with schema integrity verification.
- **Curated Sample Data**: Pre-load realistic programming classics (*Clean Code*, *Effective Java*, *The Pragmatic Programmer*) for immediate evaluation.

---

## 🏛️ Software Architecture & Design Patterns

The codebase strictly adheres to **SOLID principles** and a **Layered N-Tier Architecture** to guarantee maintainability, high testability, and clean separation of concerns.

```mermaid
graph TD
    subgraph UI ["Presentation Layer (JavaFX)"]
        MC["MainController"] --> DC["DashboardController"]
        MC --> LC["LibraryController"]
        MC --> BDC["BookDetailsController"]
        MC --> SC["SettingsController"]
        LC --> BCC["BookCardComponent"]
        DC --> SCC["StatCardComponent"]
        BDC --> CFC["ChapterFormController"]
        BDC --> SFC["SessionFormController"]
    end

    subgraph Service ["Business Logic Layer"]
        BS["BookService"]
        CS["ChapterService"]
        RTS["ReadingTrackerService"]
        SS["SettingsService"]
        BK["BackupService"]
        SD["SampleDataService"]
    end

    subgraph DAO ["Data Access Layer (DAO Pattern)"]
        BD["BookDao / SqliteBookDao"]
        CD["ChapterDao / SqliteChapterDao"]
        RSD["ReadingSessionDao / SqliteReadingSessionDao"]
        SDO["SettingsDao / SqliteSettingsDao"]
    end

    subgraph DB ["Storage Layer"]
        DM["DatabaseManager (Singleton)"]
        SQL[("SQLite 3 Database")]
    end

    UI --> Service
    Service --> DAO
    DAO --> DM
    DM --> SQL
```

### Applied Design Patterns (GoF)
- **DAO Pattern (Data Access Object)**: Decouples domain entities and services from raw SQL queries (`BookDao`, `ChapterDao`, `ReadingSessionDao`, `SettingsDao`).
- **Singleton Pattern**: Ensures single, thread-safe instances for `DatabaseManager` and localization engine (`I18n`).
- **Factory / Component Pattern**: Custom reusable UI components (`BookCardComponent`, `StatCardComponent`) encapsulating FXML and animation lifecycle.
- **State Pattern / Auto-Transitions**: Automated lifecycle transitions (`NOT_STARTED` ➔ `READING` ➔ `COMPLETED`) based on page, session, and chapter progress formulas.
- **Observer Pattern**: Leverages JavaFX `ObservableList` and data bindings for reactive UI updates upon data mutations.

---

## 🧪 Automated Testing Suite

Comprehensive unit and integration testing suite utilizing **JUnit 5**, ensuring complete reliability of business logic, state transitions, SQLite transactions, and localization keys.

| Test Class | Scope & Coverage | Tests | Status |
| :--- | :--- | :---: | :---: |
| [`BookTest`](src/test/java/com/librarymanager/model/BookTest.java) | Domain formula calculations, percentage rounding, boundary clamping | 3 | ✅ Passed |
| [`BookDaoTest`](src/test/java/com/librarymanager/dao/BookDaoTest.java) | SQLite CRUD, category/tag/favorite persistence, multi-criteria search queries | 7 | ✅ Passed |
| [`BookServiceTest`](src/test/java/com/librarymanager/service/BookServiceTest.java) | Business validation, ISBN checks, page boundaries, automated transitions | 6 | ✅ Passed |
| [`ChapterServiceTest`](src/test/java/com/librarymanager/service/ChapterServiceTest.java) | Chapter completion ratios, page calculations, and book progress sync | 4 | ✅ Passed |
| [`ReadingSessionDaoTest`](src/test/java/com/librarymanager/dao/ReadingSessionDaoTest.java) | Reading session persistence, date aggregations, cascading book deletions | 5 | ✅ Passed |
| [`ReadingTrackerServiceTest`](src/test/java/com/librarymanager/service/ReadingTrackerServiceTest.java) | Streak algorithms (current & best), daily averages, goal tracking & milestones | 9 | ✅ Passed |
| [`SettingsServiceTest`](src/test/java/com/librarymanager/service/SettingsServiceTest.java) | Theme preferences, safety dialog flags, and language persistence | 3 | ✅ Passed |
| [`BackupServiceTest`](src/test/java/com/librarymanager/service/BackupServiceTest.java) | SQLite database file export, verification, and restore routines | 2 | ✅ Passed |
| [`I18nTest`](src/test/java/com/librarymanager/util/I18nTest.java) | Bilingual bundle key parity, RTL layout resolution, parameter formatting | 3 | ✅ Passed |
| **Total Test Suite** | **100% Automated Coverage of Domain, DAO & Services** | **42** | **✅ 100% Passed** |

Execute all tests locally with:
```bash
mvn test
```

---

## ⚙️ CI/CD & Native Packaging (DevOps)

### 1. Continuous Integration Pipeline (`ci.yml`)
Configured in [`.github/workflows/ci.yml`](.github/workflows/ci.yml). On every `push` and `pull_request`:
- Provisions an **Ubuntu runner** with **Java 21 (Temurin)** and Maven dependency caching.
- Executes the entire test suite in headless mode (`mvn test`). **Build fails immediately if any test fails.**
- Validates the standalone shaded fat JAR packaging.
- Archives Surefire test execution reports as workflow artifacts.

### 2. Multi-Platform Release Pipeline (`native-package.yml`)
Configured in [`.github/workflows/native-package.yml`](.github/workflows/native-package.yml). When a release tag (`v*`) is pushed:
- **Linux (`ubuntu-latest`)**: Builds `library-manager_1.2.0_amd64.deb` and portable `.tar.gz` via `jpackage`.
- **Windows (`windows-latest`)**: Builds `LibraryManager-1.2.0-windows-x64.msi` installer and portable `.zip`.
- **macOS (`macos-latest`)**: Builds `LibraryManager-1.2.0.dmg`, `.pkg`, and `LibraryManager.app`.
- **Security Checksums**: Computes `SHA256SUMS.txt` for all compiled binaries.
- **Production Publishing**: Automatically publishes the official release with all binaries attached.

---

## 🚀 Developer Quick Start

### Prerequisites
- **JDK 21** or higher
- **Maven 3.8+**

### 1. Clone & Run with Maven
```bash
git clone https://github.com/LordJalalMahmoud/LibraryManagerGUI.git
cd LibraryManagerGUI/GeminiVersion
mvn javafx:run
```

### 2. Run Automated Test Suite
```bash
mvn test
```

### 3. Build Executable Fat JAR
```bash
mvn clean package
# Produces standalone executable in target/
java -jar target/library-manager-1.2.0.jar
```

### 4. Build Native Packages Locally (jpackage)
```bash
# Linux (builds .deb, app-image, and .tar.gz)
./package-native.sh all

# Or via Maven profile:
mvn package -Pnative-deb -DskipTests=true

# Windows (builds .msi and standalone)
package-native.bat

# macOS (builds .dmg, .pkg, and .app)
./package-native-macos.sh all
```

---

## 📂 Codebase Structure

```
src/
├── main/
│   ├── java/com/librarymanager/
│   │   ├── Main.java                # JavaFX Application entry point
│   │   ├── Launcher.java            # Fat-JAR execution bootstrap
│   │   ├── model/
│   │   │   ├── Book.java            # Book domain entity & reading calculations
│   │   │   ├── Chapter.java         # Course chapter entity with page ranges
│   │   │   ├── ReadingSession.java  # Reading session entity with date, pages & duration
│   │   │   ├── ReadingGoal.java     # Daily & annual reading targets
│   │   │   ├── ReadingStatus.java   # Status state enum (NOT_STARTED, READING, COMPLETED)
│   │   │   └── LibraryStats.java    # Aggregated KPI analytics container
│   │   ├── database/
│   │   │   └── DatabaseManager.java # Thread-safe SQLite connection & migrations
│   │   ├── dao/
│   │   │   ├── BookDao.java & SqliteBookDao.java
│   │   │   ├── ChapterDao.java & SqliteChapterDao.java
│   │   │   ├── ReadingSessionDao.java & SqliteReadingSessionDao.java
│   │   │   └── SettingsDao.java & SqliteSettingsDao.java
│   │   ├── service/
│   │   │   ├── BookService.java           # Validation, business logic & auto-transitions
│   │   │   ├── ChapterService.java        # Chapter progress & aggregation logic
│   │   │   ├── ReadingTrackerService.java # Streaks, daily averages & goal calculations
│   │   │   ├── SettingsService.java       # User preferences & theme persistence
│   │   │   ├── BackupService.java         # SQLite database backup & restore
│   │   │   └── SampleDataService.java     # Curated classic books & reading sessions loader
│   │   ├── component/
│   │   │   ├── BookCardComponent.java     # Modern book card with progress bar & badges
│   │   │   ├── StatCardComponent.java     # Metric card with animated number counters
│   │   │   └── ToastNotification.java     # Non-blocking floating toast system
│   │   ├── controller/
│   │   │   ├── MainController.java        # Shell navigation & view coordinator
│   │   │   ├── DashboardController.java   # Analytics, streaks, daily avg & goals
│   │   │   ├── LibraryController.java     # Responsive card grid, search & filters
│   │   │   ├── BookDetailsController.java # Book reading view, chapters & sessions
│   │   │   ├── BookFormController.java    # Add / Edit book modal dialog
│   │   │   ├── ChapterFormController.java # Add course chapter modal dialog
│   │   │   ├── SessionFormController.java # Log reading session modal dialog
│   │   │   └── SettingsController.java    # Preferences, reading goals & backup
│   │   └── util/
│   │       ├── AnimationUtil.java         # Smooth UI transitions & number counters
│   │       ├── DateUtil.java              # Friendly date formatting
│   │       ├── DialogUtil.java            # Modern styled confirmation & error modals
│   │       ├── IconUtil.java              # Scalable SVG vector icon factory
│   │       └── I18n.java                  # ResourceBundle localization & RTL detector
│   └── resources/
│       ├── css/
│       │   ├── styles.css                 # Base component stylesheet & layout rules
│       │   ├── theme-dark.css             # Dark theme CSS variable tokens
│       │   └── theme-light.css            # Light theme CSS variable tokens
│       ├── i18n/
│       │   ├── messages_en.properties     # English localization strings
│       │   └── messages_ar.properties     # Arabic localization strings (العربية)
│       └── icons/
│           └── app-icon.png               # High-resolution application icon
└── test/
    └── java/com/librarymanager/
        ├── model/BookTest.java
        ├── dao/
        │   ├── BookDaoTest.java
        │   └── ReadingSessionDaoTest.java
        ├── service/
        │   ├── BookServiceTest.java
        │   ├── ChapterServiceTest.java
        │   ├── ReadingTrackerServiceTest.java
        │   ├── SettingsServiceTest.java
        │   └── BackupServiceTest.java
        └── util/I18nTest.java
```

---

## 📄 License
---

<p align="center">
  Crafted with ❤️ by <b>Jalal Mahmoud</b> • Engineered with <b>Java 21 & JavaFX</b>
</p>
