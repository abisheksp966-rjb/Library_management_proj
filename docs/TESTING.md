# Software Testing Document
## Library Book Lending and Usage Analysis System

---

**Academic Year:** 2026–2027 (Odd Semester)  
**Course:** `25ITT33` - Software Engineering  
**Department:** Information Technology, Velalar College of Engineering and Technology  
**Student:** Abishek (`732925ITR004`), Class: 25IT3A  
**Scenario:** Scenario 4 — Library Book Lending and Usage Analysis System  

---

## 1. Testing Strategy and Methodology

### 1.1 Overview
The testing phase ensures that the Library Book Lending and Usage Analysis System meets all functional specifications, performs reliable calculations (overdue fines, loan periods, stock availability), and guarantees data consistency across both the Java application and the Python analytical pipeline.

### 1.2 Verification & Validation (V&V)
- **Verification:** Ensures that each tier was built to specifications through static analysis, code walkthroughs, design review (SRS to Class diagram traceability), and schema constraint verification.
- **Validation:** Ensures through dynamic execution that the software satisfies the library staff's real-world functional requirements.

### 1.3 Testing Levels
1. **Unit Testing:** Validates isolated Java service methods (`calculateFine`, `isAvailable`, `isOverdue`, input validation).
2. **Integration Testing:** Tests interactions between the Service layer, DAO layer, and SQLite database.
3. **System Testing:** Validates complete end-to-end user workflows (issuing, returning, reporting, and Python data extraction).

---

## 2. Test Design Techniques Applied

### 2.1 Black-Box Testing

#### A. Equivalence Partitioning (Fine Calculation)
- **Partition 1 (Returned on or before Due Date):** $\text{Return Date} \le \text{Due Date} \implies \text{Fine} = ₹0.00$.
- **Partition 2 (Returned past Due Date):** $\text{Return Date} > \text{Due Date} \implies \text{Fine} = (\text{Return Date} - \text{Due Date}) \times ₹2.00$.

#### B. Boundary Value Analysis (Borrowing Limits)
- Max allowed active books = 3.
  - Active loans = 0: Allowed $\checkmark$
  - Active loans = 2: Allowed $\checkmark$
  - Active loans = 3: Rejected with `StudentLimitExceededException` $\checkmark$
  - Active loans = 4: Rejected with `StudentLimitExceededException` $\checkmark$

#### C. Boundary Value Analysis (Book Copies)
- `available_copies`:
  - `available_copies = 1`: Can issue, stock drops to 0 $\checkmark$
  - `available_copies = 0`: Rejected with `BookUnavailableException` $\checkmark$

### 2.2 White-Box Testing (Basis Path & Cyclomatic Complexity)

Consider the core fine calculation logic:

```java
public double calculateFine(LocalDate dueDate, LocalDate returnDate, double ratePerDay) {
    if (returnDate == null || !returnDate.isAfter(dueDate)) { // Decision 1
        return 0.0;
    }
    long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate); // Node 2
    if (overdueDays > 0) { // Decision 2
        return overdueDays * ratePerDay;
    }
    return 0.0;
}
```

- **Predicate Nodes ($P$):** 2 (Date condition and positive overdue days check).
- **Cyclomatic Complexity ($V(G)$):** $V(G) = P + 1 = 2 + 1 = 3$.
- **Independent Execution Paths:**
  - **Path 1:** `returnDate <= dueDate` $\implies$ Returns `0.0`.
  - **Path 2:** `returnDate > dueDate` and `overdueDays > 0` $\implies$ Returns `overdueDays * 2.0`.
  - **Path 3:** `returnDate == null` $\implies$ Evaluated as active loan, returns `0.0` or current day overdue fine.

---

## 3. Detailed Test Cases and Results

| Test ID | Module / Feature | Test Description | Test Inputs | Expected Result | Actual Result | Status |
|:---:|:---|:---|:---|:---|:---|:---:|
| **TC-01** | Stock Check | Issue book when copies are available | Student ID: 1, Book ID: 1 (`available_copies = 5`) | Book issued, copies updated to 4, Due Date = +14 days | Book issued, copies = 4, Due Date set to +14 days | **PASS** |
| **TC-02** | Stock Check | Prevent issuing book when copies are 0 | Student ID: 2, Book ID: 8 (`available_copies = 0`) | Throws `BookUnavailableException`, issue denied | `BookUnavailableException` caught, error displayed | **PASS** |
| **TC-03** | Borrower Limit | Enforce maximum 3 active books limit | Student ID with 3 active loans tries to borrow 4th book | Throws `StudentLimitExceededException`, issue denied | `StudentLimitExceededException` caught, transaction rolled back | **PASS** |
| **TC-04** | Fine Calculation | Return book on exact due date (Boundary) | Due Date: 2026-10-15, Return Date: 2026-10-15 | Overdue days = 0, Fine = ₹0.00 | Overdue days = 0, Fine = ₹0.00 | **PASS** |
| **TC-05** | Fine Calculation | Return book before due date | Due Date: 2026-10-20, Return Date: 2026-10-18 | Overdue days = 0, Fine = ₹0.00 | Overdue days = 0, Fine = ₹0.00 | **PASS** |
| **TC-06** | Fine Calculation | Return book 5 days overdue | Due Date: 2026-10-10, Return Date: 2026-10-15 | Overdue days = 5, Fine = $5 \times 2 = ₹10.00$ | Overdue days = 5, Fine = ₹10.00 | **PASS** |
| **TC-07** | Fine Calculation | Return book 15 days overdue | Due Date: 2026-09-01, Return Date: 2026-09-16 | Overdue days = 15, Fine = $15 \times 2 = ₹30.00$ | Overdue days = 15, Fine = ₹30.00 | **PASS** |
| **TC-08** | Stock Increment | Return book increments available copies | Return Book ID: 1 (`available_copies = 4`) | `available_copies` increments to 5 | `available_copies` successfully updated to 5 | **PASS** |
| **TC-09** | Report Generation| Available books report filtering | Request catalog list | Displays only books with `available_copies > 0` | Accurate list displayed | **PASS** |
| **TC-10** | Report Generation| Overdue books report calculation | Request overdue list | Lists active loans where `due_date < current_date` | Accurately identifies overdue loans with live fine estimate | **PASS** |
| **TC-11** | Report Generation| Student borrowing history | Query history for Student ID: 1 | Displays all historical and active transactions for student 1 | Complete log displayed | **PASS** |
| **TC-12** | Input Validation | Non-existent Student ID handling | Student ID: 9999 (Not in DB) | Displays "Student not found" message | Handled cleanly with `RecordNotFoundException` | **PASS** |
| **TC-13** | Database Integrity| Foreign key constraint check | Direct insert of transaction with invalid `book_id` | SQLite rejects insert with FOREIGN KEY constraint failed | Insert blocked by SQLite | **PASS** |
| **TC-14** | Python Pipeline | Data extraction and view reading | Run `analysis.py` | Successfully extracts 30+ records from `vw_circulation_analytics` | 32 rows extracted into Pandas DataFrame | **PASS** |
| **TC-15** | Visualizations | Visual chart generation | Execute analytics script | Generates 4 PNG charts in `visualizations/` folder | 4 high-res charts successfully generated and saved | **PASS** |

---

## 4. Test Summary and Conclusion
- **Total Test Cases Executed:** 15
- **Passed:** 15
- **Failed:** 0
- **Pass Rate:** 100%
- **Evaluation:** The circulation business rules, database integrity, error-handling mechanisms, and Python data science pipelines perform strictly in accordance with the project requirements.
