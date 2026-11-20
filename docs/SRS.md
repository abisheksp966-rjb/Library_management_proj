# Software Requirements Specification (SRS)
## Library Book Lending and Usage Analysis System

---

**Academic Year:** 2026–2027 (Odd Semester)  
**Course Code & Title:**  
- `25ITT33` - Software Engineering  
- `25ITT31` - Database Management Systems  
- `25ITT32` - Java Programming  
- `25ITC31` - Foundations of Data Science  

**Institution:** Velalar College of Engineering and Technology, Erode – 638012  
**Department:** Department of Information Technology  
**Class / Section:** 25IT3A  
**Student Name:** Abishek  
**Register Number:** 732925ITR004  
**Assigned Activity:** Project-Based Assessment — Scenario 4  

---

## 1. Introduction

### 1.1 Purpose
The purpose of this document is to specify the detailed software requirements for the **Library Book Lending and Usage Analysis System**. This system is engineered to replace labor-intensive manual recordkeeping in college libraries by providing an automated, database-driven solution for managing book circulation and conducting data-driven borrowing pattern analytics.

### 1.2 Scope
The software product encompasses two core subsystems:
1. **Circulation Management Subsystem (Java & SQLite):** Handles student registrations, catalog management, book lending, returns, real-time availability tracking, fine calculations for overdue items, and operational reporting.
2. **Usage Analytics Subsystem (Python, Pandas & Seaborn):** Extracts historical circulation data from the database, preprocesses multi-attribute transaction logs, and performs exploratory data analysis (EDA) to visualize trends across categories, months, and titles.

### 1.3 Definitions, Acronyms, and Abbreviations
- **SRS:** Software Requirements Specification
- **DBMS:** Database Management System
- **CLI:** Command Line Interface
- **DAO:** Data Access Object
- **ER:** Entity-Relationship
- **BCNF:** Boyce-Codd Normal Form
- **EDA:** Exploratory Data Analysis
- **Due Date:** The deadline date by which a borrowed book must be returned without penalty (14 calendar days from the issue date).
- **Overdue Fine:** A penalty fee computed at ₹2.00 per day for every day a book is retained past the due date.

### 1.4 Overview
The remainder of this document outlines the general product description, specific functional requirements, interface requirements, and non-functional constraints compliant with IEEE 830 standards.

---

## 2. Overall Description

### 2.1 Product Perspective
The system operates as a self-contained, three-tier architecture:
- **Presentation Tier:** Interactive console CLI providing structured menus for librarians and administrative staff.
- **Business Logic & Service Tier:** Object-oriented Java modules enforcing business rules (e.g., maximum book limits, non-negative copy checks, overdue fine formulas).
- **Data Persistence Tier:** An SQLite relational database (`library.db`) enforcing referential integrity.
- **Analytical & Reporting Tier:** Python data science pipeline that interfaces with the SQLite database to generate high-resolution visual insights.

```
+-----------------------------------------------------------+
|                     User Interface                        |
|           (Java Interactive Console / CLI Menu)           |
+-----------------------------+-----------------------------+
                              |
+-----------------------------v-----------------------------+
|                      Service Layer                        |
|   (LibraryService, ReportService, Circulation Validator)  |
+-----------------------------+-----------------------------+
                              |
+-----------------------------v-----------------------------+
|                   Data Access Object (DAO)                |
|    (BookDAO, StudentDAO, TransactionDAO, DatabaseConfig)  |
+-----------------------------+-----------------------------+
                              |
+-----------------------------v-----------------------------+
|                    Database Storage                       |
|                   (SQLite: library.db)                    |
+-----------------------------+-----------------------------+
                              |
+-----------------------------v-----------------------------+
|             Python Data Science Pipeline                  |
|    (Pandas Data Wrangling, Matplotlib & Seaborn Charts)   |
+-----------------------------------------------------------+
```

### 2.2 Product Functions
- Maintain catalog records: categories, authors, and books.
- Maintain student membership records and check borrower eligibility.
- Issue books to students with automated stock decrement and validation.
- Prevent issuance of unavailable book titles (available copies = 0).
- Restrict active borrowings to a maximum of 3 books per student.
- Return books with automatic timestamping, stock increment, and overdue fine computation.
- Generate operational tabular reports: available inventory, active borrowings, overdue items, and student history.
- Export and load transaction data into Python data frames for categorical, temporal, and frequency distribution analysis.

### 2.3 User Characteristics
The primary users are library staff and administrators who possess basic computer literacy and familiarity with command-line interactions.

### 2.4 General Constraints
- Language: Java (JDK 8 or higher) for the core circulation application.
- Scripting & Analytics: Python 3.8+ with Pandas, NumPy, Matplotlib, and Seaborn.
- Database: SQLite 3.x embedded database.
- Platform: Cross-platform (Windows, Linux, macOS).

---

## 3. Specific Functional Requirements

### FR1: Catalog & Membership Management
- **FR1.1:** The system shall allow librarians to add and maintain book categories (e.g., Computer Science, Mathematics, Electronics, Management).
- **FR1.2:** The system shall allow librarians to record author profiles.
- **FR1.3:** The system shall store book records with ISBN, title, category ID, author ID, edition, total copies, and available copies.
- **FR1.4:** The system shall maintain student records including Student ID, Register Number, Name, Department, Year, and Email.

### FR2: Book Circulation — Issue Operation
- **FR2.1:** The system shall verify the validity of both the Student ID and the Book ID prior to initiating a loan.
- **FR2.2:** The system shall record the Issue Date as the current date and compute the Due Date exactly 14 calendar days after the Issue Date.
- **FR2.3:** The system shall decrement the book's `available_copies` count by 1 upon a successful issue.
- **FR2.4:** The transaction status shall be marked as `'ISSUED'`.

### FR3: Availability and Eligibility Enforcement
- **FR3.1:** The system shall strictly prevent the issuance of any book if `available_copies <= 0`, raising a `BookUnavailableException`.
- **FR3.2:** The system shall prevent a student from borrowing more than 3 active books simultaneously, raising a `StudentLimitExceededException`.
- **FR3.3:** The system shall prevent issuing the same title to a student if they currently hold an unreturned copy of that title.

### FR4: Book Circulation — Return Operation & Fine Calculation
- **FR4.1:** The system shall allow librarians to process book returns by specifying the Transaction ID or Student/Book combination.
- **FR4.2:** The system shall record the Return Date as the current system date (or an operator-specified return date for retrospective logging).
- **FR4.3:** The system shall calculate the number of overdue days as:
  $$\text{Overdue Days} = \max(0, \text{Return Date} - \text{Due Date})$$
- **FR4.4:** The system shall calculate the fine amount using the rule:
  $$\text{Fine Amount} = \text{Overdue Days} \times ₹2.00$$
- **FR4.5:** The system shall increment the book's `available_copies` count by 1 upon successful return.
- **FR4.6:** The transaction status shall be updated to `'RETURNED'`.

### FR5: Operational Reporting
- **FR5.1:** The system shall generate a real-time list of all available books whose available copies are greater than zero.
- **FR5.2:** The system shall generate a list of all overdue loans with borrower details, book titles, due dates, overdue days, and accrued fines.
- **FR5.3:** The system shall display the complete borrowing history of any selected student.

### FR6: Database Retrieval for Analytics
- **FR6.1:** The system shall enable read-only analytical queries on the SQLite database using Python's `sqlite3` driver.
- **FR6.2:** The analytical query shall join `transactions`, `books`, `categories`, `authors`, and `students` to produce a comprehensive denormalized dataset for data processing.

### FR7: Data Preprocessing and Cleaning
- **FR7.1:** The Python pipeline shall convert date strings into ISO datetime objects.
- **FR7.2:** The pipeline shall impute or handle null return dates for currently issued books.
- **FR7.3:** The pipeline shall extract categorical features such as `Borrow_Month`, `Borrow_Year`, and `Day_Of_Week`.

### FR8: Usage Analytics & Visualization
- **FR8.1:** The system shall visualize borrowing frequency across subject categories.
- **FR8.2:** The system shall plot time-series monthly borrowing trends to identify peak library usage periods.
- **FR8.3:** The system shall plot the top most frequently borrowed book titles.
- **FR8.4:** The system shall generate fine distribution plots and overdue frequency breakdowns.

---

## 4. Non-Functional Requirements

### 4.1 Performance Requirements
- Console menu operations and queries must execute and display results in under 500 milliseconds.
- Python analytical script execution and chart rendering should complete in under 5 seconds for datasets up to 100,000 records.

### 4.2 Reliability & Data Integrity
- Database foreign key constraints must be strictly enforced via `PRAGMA foreign_keys = ON;`.
- Stock quantities must never drop below 0.
- All transactional updates (issue and return) must use atomic transactions to prevent partial updates.

### 4.3 Maintainability & Modularity
- The Java codebase must adhere to the Single Responsibility Principle (SRP) with distinct packages: `model`, `dao`, `service`, `exception`, and `util`.
- The database schema must satisfy Boyce-Codd Normal Form (BCNF) to eliminate update, insertion, and deletion anomalies.

### 4.4 Portability
- The application requires only a standard JRE and Python environment without requiring external database server daemons.

---

## 5. Traceability Matrix

| Requirement ID | Description | Component / Module | Verification Method |
|---|---|---|---|
| FR1 | Category, Author, Book, Student CRUD | `model.*`, `dao.*` | Unit & Integration Test |
| FR2 | Book Issuance & 14-day Due Date | `LibraryService.issueBook()` | Unit Test (UT-01) |
| FR3 | Availability & Limit Checking | `LibraryService.validateIssue()` | Unit Test (UT-02, UT-03) |
| FR4 | Return & ₹2/day Fine Calculation | `LibraryService.returnBook()` | Unit Test (UT-04, UT-05) |
| FR5 | Operational Reports | `ReportService.*` | System Test (ST-01) |
| FR6 | Python Database Retrieval | `python_analytics/analysis.py` | Integration Test |
| FR7 | Data Wrangling & Cleaning | `python_analytics/analysis.py` | Script Verification |
| FR8 | Multi-attribute Visualizations | `python_analytics/visualizations/` | Visual Output Check |
