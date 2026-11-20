# System Design Document (SDD)
## Library Book Lending and Usage Analysis System

---

**Academic Year:** 2026–2027 (Odd Semester)  
**Course:** `25ITT33` - Software Engineering  
**Department:** Information Technology, Velalar College of Engineering and Technology  
**Author:** Abishek (`732925ITR004`), Class: 25IT3A  
**Scenario:** Scenario 4 — Library Book Lending and Usage Analysis System  

---

## 1. Architectural Design

The system implements a modular **Three-Tier Layered Architecture** with an independent **Analytical Engine**.

```
+========================================================================+
|                       PRESENTATION LAYER                               |
|   +----------------------------------------------------------------+   |
|   |             Console Menu & CLI Interface (Main.java)            |   |
|   +----------------------------------------------------------------+   |
+===================================|====================================+
                                    | (User Requests / Formatted I/O)
+===================================v====================================+
|                      APPLICATION / SERVICE LAYER                       |
|   +-------------------------+     +--------------------------------+   |
|   |     LibraryService      |     |         ReportService          |   |
|   |  - issueBook()          |     |  - getAvailableBooksReport()   |   |
|   |  - returnBook()         |     |  - getOverdueLoansReport()     |   |
|   |  - calculateFine()      |     |  - getStudentHistoryReport()   |   |
|   +-------------------------+     +--------------------------------+   |
|                               |                                        |
|   +---------------------------v------------------------------------+   |
|   |      Exception & Validation Layer (Custom Exceptions)          |   |
|   +----------------------------------------------------------------+   |
+===================================|====================================+
                                    | (DTOs / Domain Models)
+===================================v====================================+
|                       DATA ACCESS LAYER (DAO)                          |
|   +--------------------+  +--------------------+  +----------------+   |
|   |     BookDAO        |  |     StudentDAO     |  | TransactionDAO |   |
|   +--------------------+  +--------------------+  +----------------+   |
|   +----------------------------------------------------------------+   |
|   |     DatabaseConnection / SQLite Driver Manager                 |   |
|   +----------------------------------------------------------------+   |
+===================================|====================================+
                                    | (JDBC SQL Queries)
+===================================v====================================+
|                        DATA PERSISTENCE LAYER                          |
|   +----------------------------------------------------------------+   |
|   |                 SQLite Database (library.db)                   |   |
|   |  [categories] [authors] [books] [students] [transactions]      |   |
|   +----------------------------------------------------------------+   |
+===================================|====================================+
                                    | (Read-Only SQL Extraction)
+===================================v====================================+
|                    PYTHON ANALYTICS & EDA ENGINE                       |
|   +----------------------------------------------------------------+   |
|   |   sqlite3 -> Pandas DataFrame -> Data Cleaning -> Aggregation  |   |
|   |   Matplotlib / Seaborn Visualizations (.png & Jupyter Notebook)|   |
|   +----------------------------------------------------------------+   |
+========================================================================+
```

---

## 2. Use Case Modeling

### 2.1 Use Case Diagram (Mermaid)

```mermaid
flowchart LR
    Librarian((Librarian))
    DataAnalyst((Data Analyst))

    subgraph Circulation_System ["Library Circulation Subsystem"]
        UC1(["Manage Catalog (Books, Authors, Categories)"])
        UC2(["Manage Student Memberships"])
        UC3(["Issue Book"])
        UC4(["Validate Stock & Eligibility"])
        UC5(["Return Book & Compute Fine"])
        UC6(["Generate Operational Reports"])
    end

    subgraph Analytics_System ["Usage Analysis Subsystem"]
        UC7(["Extract Circulation Logs"])
        UC8(["Preprocess & Aggregate Data"])
        UC9(["Visualize Borrowing Patterns by Category"])
        UC10(["Visualize Monthly Trends & Top Books"])
    end

    Librarian --> UC1
    Librarian --> UC2
    Librarian --> UC3
    Librarian --> UC5
    Librarian --> UC6

    UC3 -.->|<<include>>| UC4
    UC5 -.->|<<include>>| UC4

    DataAnalyst --> UC7
    DataAnalyst --> UC8
    DataAnalyst --> UC9
    DataAnalyst --> UC10
```

### 2.2 Detailed Use Case Descriptions

#### Use Case UC3: Issue Book
- **Primary Actor:** Librarian
- **Preconditions:**
  1. Student is registered in the database.
  2. Book exists in the catalog.
- **Main Success Scenario:**
  1. Librarian selects "Issue Book" and inputs `Student ID` and `Book ID`.
  2. System verifies book exists and checks `available_copies > 0`.
  3. System checks the student currently holds fewer than 3 active borrowed books.
  4. System records the transaction with Issue Date = Current Date, Due Date = Current Date + 14 Days, Status = `'ISSUED'`.
  5. System decrements `available_copies` of the book by 1.
  6. System displays an issue confirmation slip with Transaction ID and Due Date.
- **Exceptions:**
  - 3a. Book is out of stock $\to$ System throws `BookUnavailableException` and aborts.
  - 3b. Student already has 3 active loans $\to$ System throws `StudentLimitExceededException` and aborts.

#### Use Case UC5: Return Book & Calculate Fine
- **Primary Actor:** Librarian
- **Preconditions:**
  - An active issue transaction exists (`status = 'ISSUED'`).
- **Main Success Scenario:**
  1. Librarian selects "Return Book" and inputs `Transaction ID` (or `Student ID` + `Book ID`).
  2. System retrieves the transaction record and checks Due Date against Return Date.
  3. If $\text{Return Date} > \text{Due Date}$, system calculates:
     $$\text{Overdue Days} = \text{Return Date} - \text{Due Date}$$
     $$\text{Fine} = \text{Overdue Days} \times ₹2.00$$
     Otherwise, $\text{Fine} = ₹0.00$.
  4. System updates the transaction status to `'RETURNED'`, records `return_date` and `fine_amount`.
  5. System increments the book's `available_copies` by 1.
  6. System displays the return summary, overdue days, and fine payable.

---

## 3. Class Diagram

### 3.1 Class Diagram (Mermaid)

```mermaid
classDiagram
    direction TB

    class Category {
        -int categoryId
        -String categoryName
        -String description
        +getCategoryId() int
        +getCategoryName() String
        +toString() String
    }

    class Author {
        -int authorId
        -String name
        -String email
        +getAuthorId() int
        +getName() String
        +toString() String
    }

    class Book {
        -int bookId
        -String isbn
        -String title
        -int categoryId
        -int authorId
        -int totalCopies
        -int availableCopies
        +isAvailable() boolean
        +decrementCopies() void
        +incrementCopies() void
        +toString() String
    }

    class Student {
        -int studentId
        -String registerNo
        -String name
        -String department
        -int year
        -String email
        +getStudentId() int
        +getRegisterNo() String
        +getName() String
        +toString() String
    }

    class BorrowTransaction {
        -int transactionId
        -int studentId
        -int bookId
        -LocalDate issueDate
        -LocalDate dueDate
        -LocalDate returnDate
        -double fineAmount
        -String status
        +isOverdue(LocalDate checkDate) boolean
        +calculateFine(LocalDate actualReturnDate, double fineRate) double
        +toString() String
    }

    class BookDAO {
        +getAllBooks() List~Book~
        +getBookById(int id) Book
        +addBook(Book book) boolean
        +updateAvailableCopies(int bookId, int delta) boolean
        +getAvailableBooks() List~Book~
    }

    class StudentDAO {
        +getAllStudents() List~Student~
        +getStudentById(int id) Student
        +getStudentByRegNo(String regNo) Student
        +getActiveBorrowCount(int studentId) int
    }

    class TransactionDAO {
        +createTransaction(BorrowTransaction t) int
        +getTransactionById(int id) BorrowTransaction
        +getActiveTransaction(int studentId, int bookId) BorrowTransaction
        +updateReturn(int transactionId, LocalDate returnDate, double fine) boolean
        +getOverdueTransactions(LocalDate currentDate) List~BorrowTransaction~
        +getStudentHistory(int studentId) List~BorrowTransaction~
    }

    class LibraryService {
        -BookDAO bookDAO
        -StudentDAO studentDAO
        -TransactionDAO transactionDAO
        +issueBook(int studentId, int bookId) BorrowTransaction
        +returnBook(int transactionId, LocalDate returnDate) double
        +checkOverdueFines() List~BorrowTransaction~
    }

    class ReportService {
        -BookDAO bookDAO
        -TransactionDAO transactionDAO
        +displayAvailableBooks() void
        +displayOverdueLoans() void
        +displayStudentHistory(int studentId) void
    }

    Book "many" --> "1" Category : belongs to
    Book "many" --> "1" Author : written by
    BorrowTransaction "many" --> "1" Book : borrows
    BorrowTransaction "many" --> "1" Student : issued to
    LibraryService ..> BookDAO : uses
    LibraryService ..> StudentDAO : uses
    LibraryService ..> TransactionDAO : uses
    ReportService ..> BookDAO : uses
    ReportService ..> TransactionDAO : uses
```

---

## 4. Sequence Diagrams

### 4.1 Book Issue Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Librarian
    participant UI as Main (Console UI)
    participant LS as LibraryService
    participant SDAO as StudentDAO
    participant BDAO as BookDAO
    participant TDAO as TransactionDAO
    participant DB as SQLite Database

    Librarian->>UI: Enter Student ID & Book ID
    UI->>LS: issueBook(studentId, bookId)
    LS->>SDAO: getActiveBorrowCount(studentId)
    SDAO->>DB: SELECT COUNT(*) FROM transactions WHERE student_id=? AND status='ISSUED'
    DB-->>SDAO: count
    SDAO-->>LS: count

    alt count >= 3
        LS-->>UI: Throw StudentLimitExceededException
        UI-->>Librarian: Display "Max 3 books limit reached"
    else count < 3
        LS->>BDAO: getBookById(bookId)
        BDAO->>DB: SELECT * FROM books WHERE book_id=?
        DB-->>BDAO: Book record
        BDAO-->>LS: Book object

        alt availableCopies <= 0
            LS-->>UI: Throw BookUnavailableException
            UI-->>Librarian: Display "Book out of stock"
        else availableCopies > 0
            LS->>BDAO: updateAvailableCopies(bookId, -1)
            BDAO->>DB: UPDATE books SET available_copies = available_copies - 1
            DB-->>BDAO: success
            LS->>TDAO: createTransaction(new BorrowTransaction)
            TDAO->>DB: INSERT INTO transactions VALUES (...)
            DB-->>TDAO: generated transactionId
            TDAO-->>LS: transactionId
            LS-->>UI: Return BorrowTransaction details
            UI-->>Librarian: Print Issue Confirmation Slip (Due Date: +14 days)
        end
    end
```

### 4.2 Book Return & Fine Calculation Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Librarian
    participant UI as Main (Console UI)
    participant LS as LibraryService
    participant TDAO as TransactionDAO
    participant BDAO as BookDAO
    participant DB as SQLite Database

    Librarian->>UI: Enter Transaction ID & Return Date
    UI->>LS: returnBook(transactionId, returnDate)
    LS->>TDAO: getTransactionById(transactionId)
    TDAO->>DB: SELECT * FROM transactions WHERE transaction_id=?
    DB-->>TDAO: Transaction record
    TDAO-->>LS: BorrowTransaction object

    Note over LS: Calculate Overdue Days:<br/>days = max(0, returnDate - dueDate)<br/>fine = days * 2.0

    LS->>TDAO: updateReturn(transactionId, returnDate, fine)
    TDAO->>DB: UPDATE transactions SET status='RETURNED', return_date=?, fine_amount=?
    DB-->>TDAO: success

    LS->>BDAO: updateAvailableCopies(bookId, +1)
    BDAO->>DB: UPDATE books SET available_copies = available_copies + 1
    DB-->>BDAO: success

    LS-->>UI: Return fine amount (e.g. ₹10.00)
    UI-->>Librarian: Display Return Receipt & Fine Notice
```

---

## 5. Software Engineering Design Principles Applied
1. **Separation of Concerns (SoC):** Distinct separation between models (state), DAOs (data access), services (business rules), and UI (interaction).
2. **Design for Testability:** Business logic methods (e.g. `calculateFine`) are pure algorithmic functions independent of console I/O, allowing automated unit testing.
3. **Data Integrity:** SQLite foreign keys with cascading policies guarantee referential integrity across student removals or book deletions.
