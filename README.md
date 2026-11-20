# Library Book Lending and Usage Analysis System

[![Academic Year](https://img.shields.io/badge/Academic%20Year-2026--2027%20(Odd)-blue.svg)](#)
[![Department](https://img.shields.io/badge/Department-Information%20Technology-green.svg)](#)
[![Institution](https://img.shields.io/badge/Institution-VCET%20Erode-orange.svg)](#)
[![Java](https://img.shields.io/badge/Java-8%2B%20%7C%20OOP%20%7C%20JDBC-red.svg)](#)
[![Database](https://img.shields.io/badge/Database-SQLite%203.x-lightgrey.svg)](#)
[![Python](https://img.shields.io/badge/Analytics-Pandas%20%7C%20Seaborn%20%7C%20Matplotlib-blueviolet.svg)](#)

---

## 👨‍🎓 Student & Academic Details

* **Institution:** Velalar College of Engineering and Technology, Erode – 638012
* **Department:** Department of Information Technology
* **Class & Section:** 25IT3A
* **Student Name:** **Abishek**
* **Register Number:** **`732925ITR004`**
* **Activity:** Project-Based Assessment — **Scenario 4**
* **Project Title:** Library Book Lending and Usage Analysis System

---

## 🎯 Course Integration & Syllabus Mapping

This project serves as a comprehensive capstone integrating four core curriculum subjects:

| Course Code | Course Title | Syllabus Alignment & Artifacts |
|---|---|---|
| **`25ITT33`** | **Software Engineering** | Requirements Engineering (SRS IEEE 830), UML Use Case & Class Diagrams, 3-Tier System Architecture, Verification & Validation, Unit/Integration Test Cases with Expected vs. Actual results. |
| **`25ITT31`** | **Database Management Systems** | Conceptual ER Modeling, Relational Schema Mapping, Integrity Constraints, Full Normalization (1NF $\to$ 2NF $\to$ 3NF/BCNF), Relational Algebra, SQL DDL/DML, Analytical Views, and Indexing. |
| **`25ITT32`** | **Java Programming** | Object-Oriented Programming (Encapsulation, Polymorphism, Interfaces), Custom Checked Exceptions (`BookUnavailableException`, `StudentLimitExceededException`), JDBC connectivity, Collections Framework, and Menu CLI. |
| **`25ITC31`** | **Foundations of Data Science** | SQLite database extraction via SQL, Data Wrangling with Pandas & NumPy, Missing value handling, Temporal feature engineering, and Exploratory Data Analysis (EDA) charts using Matplotlib and Seaborn. |

---

## 📁 Repository Directory Structure

```text
├── frontend/                              # Interactive Web Dashboard (HTML5, CSS3, JS, Chart.js)
│   ├── index.html                         # Modern Single Page Application UI with Tabs & Modals
│   ├── style.css                          # Clean, Responsive CSS3 Design & College Branding
│   └── app.js                             # Real-Time Circulation Controller & Chart.js Integration
├── docs/                                  # Academic & Engineering Documentation
│   ├── SRS.md                             # Software Requirements Specification (IEEE 830)
│   ├── SYSTEM_DESIGN.md                   # Architecture, Use Case, Class & Sequence Diagrams
│   ├── DATABASE_AND_NORMALIZATION.md      # ER Modeling, Schema & Normalization (1NF to BCNF)
│   └── TESTING.md                         # V&V Strategy & 15+ Detailed Test Cases
├── database/                              # Database Artifacts & SQL Scripts
│   ├── library.db                         # Production SQLite 3.x Circulation Database
│   ├── schema.sql                         # Complete DDL: Tables, Constraints, Indexes & Views
│   ├── seed_data.sql                      # Rich Synthetic Dataset (Books, Students, Loans)
│   └── queries.sql                        # Advanced SQL Queries (Joins, Aggregates, Subqueries)
├── src/                                   # Java Application Source Code
│   └── com/library/
│       ├── Main.java                      # Interactive CLI & Server Entry Point
│       ├── server/                        # Embedded HTTP REST & Static Web Server
│       │   └── LibraryServer.java         # Native Java SE com.sun.net.httpserver REST API
│       ├── model/                         # Domain Models (Book, Student, Category, Author, etc.)
│       │   ├── Author.java
│       │   ├── Book.java
│       │   ├── BorrowTransaction.java
│       │   ├── Category.java
│       │   └── Student.java
│       ├── dao/                           # Data Access Object Layer
│       │   ├── AuthorDAO.java
│       │   ├── BookDAO.java
│       │   ├── CategoryDAO.java
│       │   ├── DatabaseConnection.java
│       │   ├── DatabaseInitializer.java
│       │   ├── StudentDAO.java
│       │   └── TransactionDAO.java
│       ├── service/                       # Business Logic & Circulation Services
│       │   ├── LibraryService.java
│       │   └── ReportService.java
│       ├── exception/                     # Custom Exception Classes
│       │   ├── BookUnavailableException.java
│       │   ├── LibraryException.java
│       │   ├── RecordNotFoundException.java
│       │   └── StudentLimitExceededException.java
│       └── util/                          # Utility & Validation Helpers
│           ├── ConsoleUI.java
│           ├── DateUtil.java
│           └── InputValidator.java
├── lib/                                   # SQLite JDBC driver and SLF4J runtime JARs
├── python_analytics/                      # Data Science & Visual Analytics Pipeline
│   ├── analysis.py                        # Standalone Python EDA & Visualization Script
│   ├── analysis.ipynb                     # Interactive Jupyter Notebook with Analysis Insights
│   ├── requirements.txt                   # Python Dependencies (pandas, seaborn, matplotlib)
│   └── visualizations/                    # Generated High-Resolution Publication Charts
│       ├── category_distribution.png      # Chart 1: Borrowing Frequency by Category
│       ├── monthly_borrowing_trends.png   # Chart 2: Time-Series Monthly Trends & Overdues
│       ├── top_borrowed_books.png         # Chart 3: Top Most Borrowed Books Ranking
│       └── overdue_and_fine_analysis.png  # Chart 4: Overdue Durations & Fine Distributions
├── sample_io/                             # Execution Evidence & Logs
│   ├── sample_input.txt                   # Automated / Sample Menu Input Sequence
│   └── sample_output.txt                  # Full Formatted Terminal Session Transcript
├── pom.xml                                # Maven Project Configuration (Java 11+)
├── build.gradle                           # Gradle Project Configuration
├── server.js                              # Lightweight Standalone Web & REST API Server (Node.js)
├── run.bat                                # Windows 1-Click Interactive Runner
├── run.sh                                 # Linux/macOS 1-Click Interactive Runner
├── .gitignore                             # Git Ignore Rules
└── README.md                              # Main Project Readme & Execution Guide
```

---

## ⚙️ Core Business Rules

1. **Borrowing Duration:** Every issued book is assigned a standard loan duration of **14 calendar days** ($\text{Due Date} = \text{Issue Date} + 14 \text{ days}$).
2. **Borrowing Limit:** A student is permitted to hold a maximum of **3 active borrowed books** simultaneously. Attempting to borrow a fourth book throws `StudentLimitExceededException`.
3. **Availability Enforcement:** Books with zero available physical copies (`available_copies == 0`) cannot be issued, throwing `BookUnavailableException`.
4. **Overdue Fine Rule:** A fine of **₹2.00 per day** is automatically levied for every calendar day a book is returned after its due date:
   $$\text{Overdue Days} = \max(0, \text{Return Date} - \text{Due Date})$$
   $$\text{Fine Amount} = \text{Overdue Days} \times ₹2.00$$

---

## 🚀 Execution Instructions

### 1. Quick Start (1-Click Launchers)

* **Windows:** Double-click or run `run.bat` in command prompt:
  ```cmd
  run.bat
  ```
  *(Offers interactive menu to launch Web Dashboard, Java CLI, or Python Analytics)*
* **Linux / macOS:** Make executable and run `run.sh`:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```

---

### 2. Web Dashboard Execution (HTML, CSS, JS + Backend Server)

The responsive Web Frontend is served at **`http://localhost:8080/`** and connects directly to the SQLite database `database/library.db`.

#### Option A: Running via Java HTTP Server (Java 11+)
```bash
# 1. Compile Java classes
javac -encoding UTF-8 -d bin src/com/library/model/*.java src/com/library/exception/*.java src/com/library/util/*.java src/com/library/dao/*.java src/com/library/service/*.java src/com/library/server/*.java src/com/library/Main.java

# 2. Start Java Web Server on port 8080
java -cp "bin;lib/*" com.library.server.LibraryServer 8080  # Windows
# Linux/macOS: java -cp "bin:lib/*" com.library.server.LibraryServer 8080
```
*Or using Maven:*
```bash
mvn compile exec:java
```

#### Option B: Running via Standalone Node.js (Zero-dependency)
```bash
node server.js
```
*Open your browser and navigate to:* **`http://localhost:8080`**

---

### 3. REST API Endpoints Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Healthcheck and academic system metadata |
| `GET` | `/api/books` | Full book catalog with current availability & stock |
| `POST` | `/api/books` | Add new book title to inventory (`isbn`, `title`, `categoryId`, `authorId`, `totalCopies`) |
| `GET` | `/api/categories` | Retrieve all subject classifications |
| `GET` | `/api/students` | List registered students with active loan counts |
| `POST` | `/api/students` | Register student member (`registerNo`, `name`, `department`, `year`, `email`) |
| `GET` | `/api/transactions` | Full borrowing circulation ledger |
| `POST` | `/api/issue` | Issue book enforcing 3-book limit, 0-stock check, and +14 day due date |
| `POST` | `/api/return` | Return book and calculate late fine ($₹2.00$/day after due date) |
| `GET` | `/api/overdue` | Active loans exceeding 14-day limit with accrued fines |
| `GET` | `/api/students/:id/history` | Individual student's complete borrowing history |
| `GET` | `/api/analytics` | Aggregated metrics for Chart.js (categories, monthly trends, top books, fines) |
| `GET` | `/api/export/transactions` | Export transaction data in JSON format for Python pipeline |
| `GET` | `/api/export/transactions.csv` | Download circulation logs as CSV spreadsheet |

---

### 4. Java CLI Application Execution

#### Interactive Console Menu
```bash
java -cp "bin;lib/*" com.library.Main  # Windows
# Linux/macOS: java -cp "bin:lib/*" com.library.Main
```
> [!NOTE]
> The Java application features an intelligent dual-mode architecture: if the SQLite JDBC driver is present, it directly connects to `database/library.db`; if executed in a standalone environment without external JARs, it seamlessly activates its high-performance in-memory cache seeded with the identical dataset, ensuring zero runtime crashes!

---

### 5. Python Data Science & Analytics (Requirement 7 & 8)

#### Install Dependencies
```bash
cd python_analytics
pip install -r requirements.txt
```

#### Run the Visual Analytics Script
```bash
python analysis.py
```
This script will:
1. Connect to `database/library.db`.
2. Extract the denormalized dataset from `vw_circulation_analytics`.
3. Preprocess dates, loan durations, and overdue fines.
4. Output descriptive statistics in the terminal.
5. Save all 4 publication-quality charts to `python_analytics/visualizations/`.

#### Launch the Jupyter Notebook
```bash
jupyter notebook analysis.ipynb
```

---

## 📊 Visual Analytics Gallery

All 4 charts have been generated and saved in `python_analytics/visualizations/`:

| Visualization | Description | Preview File |
|---|---|---|
| **1. Category Circulation** | Bar chart & Donut breakdown of borrowings by domain | [category_distribution.png](file:///c:/Users/Abishek/project/python_analytics/visualizations/category_distribution.png) |
| **2. Monthly Trends** | Time-series monthly loan volume & overdue return rate | [monthly_borrowing_trends.png](file:///c:/Users/Abishek/project/python_analytics/visualizations/monthly_borrowing_trends.png) |
| **3. Top Borrowed Books** | Horizontal bar ranking of high-demand titles | [top_borrowed_books.png](file:///c:/Users/Abishek/project/python_analytics/visualizations/top_borrowed_books.png) |
| **4. Overdue & Fine Analysis** | Histogram of overdue days and fine distributions | [overdue_and_fine_analysis.png](file:///c:/Users/Abishek/project/python_analytics/visualizations/overdue_and_fine_analysis.png) |

---

## 🧪 Testing and Verification Summary

15 comprehensive test cases covering unit logic, integration boundaries, and database constraints were evaluated. Full documentation is available in [docs/TESTING.md](file:///c:/Users/Abishek/project/docs/TESTING.md).

- **Total Test Cases:** 15
- **Passed:** 15
- **Pass Rate:** 100%
- **Core Tested Scenarios:** Stock decrement/increment, 0-stock prevention (`BookUnavailableException`), maximum 3 active books limit (`StudentLimitExceededException`), exact on-time return ($₹0$ fine), late return ($₹2$/day fine calculation), and SQLite foreign key constraints.

---

## 📋 GitHub Submission Checklist

- [x] **1. SRS, Design diagrams, and testing documents:** Available in `docs/` (`SRS.md`, `SYSTEM_DESIGN.md`, `DATABASE_AND_NORMALIZATION.md`, `TESTING.md`).
- [x] **2. Java source code, sample inputs, and outputs:** Available in `src/com/library/` and `sample_io/`.
- [x] **3. ER Diagram, relational model, SQL scripts, and normalization details:** Available in `database/` and `docs/DATABASE_AND_NORMALIZATION.md`.
- [x] **4. Python code for database retrieval, data preprocessing, and visualization:** Available in `python_analytics/` (`analysis.py`, `analysis.ipynb`, `visualizations/*.png`).
- [x] **5. A README.md file containing project description, technologies used, and execution instructions:** Complete!
