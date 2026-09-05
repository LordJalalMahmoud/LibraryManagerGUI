# LibraryManager — Personal Library Management Desktop Application

[![CI](https://github.com/LordJalalMahmoud/LibraryManagerGUI/actions/workflows/ci.yml/badge.svg)](https://github.com/LordJalalMahmoud/LibraryManagerGUI/actions/workflows/ci.yml)

A modern, commercial-grade personal library management desktop application built with **Java 21**, **JavaFX**, and **SQLite**.

Designed with modern desktop aesthetics (rounded cards, micro-interactions, dark & light themes, non-intrusive toast notifications, real-time search, and interactive reading tracking).

---

## Features

### 1. Dashboard & Analytics
- **Summary Metrics**: Real-time statistics cards showing Total Books, Books Currently Reading, Completed Books, Not Started Backlog, and Overall Library Reading Progress.
- **Micro-Animations**: Smooth numerical counting animations when statistics appear or update.
- **Visual Charts**:
  - **Books by Reading Status**: Interactive Pie Chart breakdown.
  - **Pages Read vs. Remaining**: Bar Chart comparing completed pages against remaining backlog.
- **Spotlight Sections**:
  - **Currently Reading**: Quick access to in-progress books with instant `+10 pages` advancement.
  - **Recently Added Books**: Recent additions with direct view/edit actions.

### 2. Collection & Responsive Library Grid
- **Responsive Card Layout**: Responsive grid that dynamically adapts column layout when resizing the window.
- **Card Highlights**:
  - Cover image display with automatic graceful fallback to stylized geometric covers with title and author initials.
  - Distinct status badges: `Not Started` (Neutral Slate), `Reading` (Vibrant Blue), and `Completed` (Emerald Green).
  - Progress bar with percentage and page count ratio (`Page 150 / 500 • 30%`).
  - Action buttons for Quick View, Edit, and Delete.
- **Smooth Card Animations**: Scale and fade entrance transitions, hover elevation effects, and animated card removal upon deletion.

### 3. Real-Time Search, Filtering & Sorting
- **Real-Time Live Search**: Search across book titles and authors with debounced instant filtering as you type.
- **Filter Pills**: One-click filtering by `All Books`, `Reading`, `Completed`, or `Not Started`.
- **Flexible Sorting**:
  - Date Added (Newest / Oldest)
  - Title (A–Z)
  - Author (A–Z)
  - Reading Progress (% Highest to Lowest)
  - Total Pages

### 4. Interactive Reading Experience & Book Details
- **Dedicated Detail View**: High-resolution cover, complete metadata, and personal notes/description.
- **Interactive Reading Session**:
  - Dynamic page spinner with boundary validation (`0 <= currentPage <= totalPages`).
  - Animated progress bar transitions.
  - Quick page advancement buttons: `+1 page`, `+10 pages`, `+25 pages`.
  - Quick action: `Mark Completed`.
- **Course Chapters & University Assignments (New!)**:
  - Add specific chapters assigned by university professors or courses (e.g. Ch. 1, Ch. 3, Ch. 7).
  - Track chapter completion with interactive checkboxes that update instantly.
  - Assign page ranges (`pp. 45 - 80`) and lecture topic notes for each chapter.
  - Dynamic chapter completion progress bar (`X of Y chapters completed (Z%)`).
  - Chapter badges appear directly on book cards in the library grid (`X/Y chapters`).
- **Automatic State Transitions**:
  - When `currentPage == totalPages`: status automatically changes to `COMPLETED` and records completion date.
  - When user begins reading a `NOT_STARTED` book: status automatically changes to `READING` and records start date.

### 5. Add & Edit Books
- Modal dialog with comprehensive live validation.
- Fields: Title, Author, Total Pages, Volumes / Parts, Current Page, Status, Cover Image (File Chooser or URL), and Notes / Description.
- Helpful inline error messages preventing invalid data from reaching SQLite.

### 6. Themes & Multi-Language Support (Arabic & English)
- **Multi-Language (i18n)**: Full support for **Arabic (العربية)** and **English** with complete native RTL (Right-to-Left) and LTR layout orientation!
- **Language Switcher**: Toggle easily between Arabic and English from the Settings screen.
- **Dark Mode & Light Mode**: Seamless theme switcher in header and settings.
- **Persistence**: Both active language and theme preferences are automatically persisted in SQLite across application restarts.
- **Custom CSS Architecture**: Clean typography, subtle shadows, custom scrollbars, and modern controls.
- **Non-Intrusive Toasts**: Floating animated toast notifications for all operations.

### 7. Settings, Backup & Restore
- **Theme Selection**: Toggle between Dark and Light modes.
- **Safety Preferences**: Option to enable or disable deletion confirmation dialogs.
- **Database Management**: View database file location, database size, and total record count.
- **Database Backup & Restore**:
  - Export full SQLite `.db` database file to any location.
  - Validate and restore database from existing `.db` backup files.
- **Curated Sample Data**: Pre-load realistic programming books (*Clean Code*, *Effective Java*, *The Pragmatic Programmer*, etc.) with one click.
- **Danger Zone**: Reset library data with strong confirmation.

---

## Architecture & Code Structure

```
src/
├── main/
│   ├── java/com/librarymanager/
│   │   ├── Main.java                # JavaFX Application entry point
│   │   ├── Launcher.java            # Fat-JAR launcher wrapper
│   │   ├── model/
│   │   │   ├── Book.java            # Book entity model & progress calculations
│   │   │   ├── ReadingStatus.java   # Reading status enum (NOT_STARTED, READING, COMPLETED)
│   │   │   └── LibraryStats.java    # Aggregated statistics container
│   │   ├── database/
│   │   │   └── DatabaseManager.java # SQLite connection, table schema, indexes, backup/restore
│   │   ├── dao/
│   │   │   ├── BookDao.java         # Book DAO interface
│   │   │   ├── SqliteBookDao.java   # Prepared statement SQLite implementation
│   │   │   ├── SettingsDao.java     # Settings DAO interface
│   │   │   └── SqliteSettingsDao.java # Settings persistence implementation
│   │   ├── service/
│   │   │   ├── BookService.java     # Validation, business rules & auto-transitions
│   │   │   ├── SettingsService.java # Theme and user preference management
│   │   │   ├── BackupService.java   # Database export and restore operations
│   │   │   └── SampleDataService.java # Curated sample books loader
│   │   ├── component/
│   │   │   ├── BookCardComponent.java # Modern book card with cover and progress
│   │   │   ├── StatCardComponent.java # Metric card with animated number counters
│   │   │   └── ToastNotification.java # Floating toast notifications
│   │   ├── controller/
│   │   │   ├── MainController.java  # Shell navigation and view coordinator
│   │   │   ├── DashboardController.java # Dashboard metrics and charts
│   │   │   ├── LibraryController.java   # Card grid, search, filter and sort
│   │   │   ├── BookDetailsController.java # Dedicated reading details view
│   │   │   ├── BookFormController.java  # Add / Edit modal dialog
│   │   │   └── SettingsController.java  # Preferences, backup and storage
│   │   └── util/
│   │       ├── AnimationUtil.java   # Reusable UI transitions and progress animations
│   │       ├── DateUtil.java        # Friendly date formatting
│   │       ├── DialogUtil.java      # Modern styled confirmation & error dialogs
│   │       └── IconUtil.java        # Scalable SVG vector icons
│   └── resources/
│       ├── css/
│       │   ├── styles.css           # Base component stylesheet
│       │   ├── theme-dark.css       # Dark theme color variables
│       │   └── theme-light.css      # Light theme color variables
│       └── icons/
│           └── app-icon.png         # High-resolution application icon
└── test/
    └── java/com/librarymanager/
        ├── model/BookTest.java      # Model & formula unit tests
        ├── dao/BookDaoTest.java     # SQLite CRUD & search integration tests
        └── service/
            ├── BookServiceTest.java # Validation & state transition tests
            ├── SettingsServiceTest.java # Theme and preferences tests
            └── BackupServiceTest.java   # Backup, export & restore tests
```

---

## Build & Run Instructions

### Prerequisites
- **Java 21** or higher
- **Maven 3.8+**

### Run via Maven (Development)
```bash
mvn clean javafx:run
```

### Run Unit Tests
```bash
mvn test
```

### Build Executable Package (Fat JAR)
```bash
mvn clean package
```
This produces an executable standalone JAR in `target/`:
```bash
java -jar target/library-manager-1.0.0.jar
```

### Continuous Integration (CI)
This repository includes an automated GitHub Actions pipeline configured in `.github/workflows/ci.yml`. On every `git push` or pull request, the workflow automatically:
1. Provisions an Ubuntu runner with **Java 21 (Temurin)** and Maven dependency caching.
2. Executes all 23 unit tests (`mvn test`). If any test fails, the build fails immediately and alerts the developer.
3. Builds the standalone fat JAR to verify package integrity.
4. Archives test reports (`target/surefire-reports/`) as workflow artifacts.

---

## Cross-Platform Storage
The SQLite database file is stored automatically in the user's home directory:
- **Linux/macOS**: `~/.librarymanager/library.db`
- **Windows**: `C:\Users\<User>\.librarymanager\library.db`

The schema and required indexes are created automatically on first launch.
