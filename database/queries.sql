-- ============================================================================
-- SQL Queries Demonstrating DBMS Concepts (Course: 25ITT31)
-- Scenario 4: Library Book Lending and Usage Analysis System
-- Student: Abishek (732925ITR004), Class: 25IT3A
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Available Books Report
--    Retrieves all catalog items with available physical copies
-- ----------------------------------------------------------------------------
SELECT 
    b.book_id,
    b.isbn,
    b.title,
    c.category_name,
    a.author_name,
    b.available_copies,
    b.total_copies
FROM books b
INNER JOIN categories c ON b.category_id = c.category_id
INNER JOIN authors a ON b.author_id = a.author_id
WHERE b.available_copies > 0
ORDER BY c.category_name ASC, b.title ASC;

-- ----------------------------------------------------------------------------
-- 2. Currently Overdue Loans Report with Dynamic Fine Computation
--    (Overdue Days = Current Date - Due Date; Fine = Overdue Days * ₹2.00)
-- ----------------------------------------------------------------------------
SELECT 
    t.transaction_id,
    s.register_no,
    s.name AS student_name,
    s.department,
    b.title AS book_title,
    t.issue_date,
    t.due_date,
    CAST((julianday('2026-11-19') - julianday(t.due_date)) AS INTEGER) AS overdue_days,
    MAX(0, CAST((julianday('2026-11-19') - julianday(t.due_date)) AS INTEGER) * 2.0) AS fine_payable_inr
FROM transactions t
INNER JOIN students s ON t.student_id = s.student_id
INNER JOIN books b ON t.book_id = b.book_id
WHERE t.status = 'ISSUED' AND date(t.due_date) < '2026-11-19'
ORDER BY overdue_days DESC;

-- ----------------------------------------------------------------------------
-- 3. Individual Student Borrowing History
--    Retrieves historical transactions for Abishek (Register No: 732925ITR004)
-- ----------------------------------------------------------------------------
SELECT 
    t.transaction_id,
    b.title AS book_title,
    c.category_name,
    t.issue_date,
    t.due_date,
    COALESCE(t.return_date, 'NOT RETURNED') AS return_date,
    t.fine_amount,
    t.status
FROM transactions t
INNER JOIN students s ON t.student_id = s.student_id
INNER JOIN books b ON t.book_id = b.book_id
INNER JOIN categories c ON b.category_id = c.category_id
WHERE s.register_no = '732925ITR004'
ORDER BY t.issue_date DESC;

-- ----------------------------------------------------------------------------
-- 4. Category-wise Borrowing Aggregation (GROUP BY, HAVING, AGGREGATES)
--    Identifies the most popular subject domains across all semesters
-- ----------------------------------------------------------------------------
SELECT 
    c.category_name,
    COUNT(t.transaction_id) AS total_borrowings,
    COUNT(DISTINCT t.student_id) AS unique_borrowers,
    SUM(t.fine_amount) AS total_fines_collected
FROM categories c
LEFT JOIN books b ON c.category_id = b.category_id
LEFT JOIN transactions t ON b.book_id = t.book_id
GROUP BY c.category_id, c.category_name
HAVING total_borrowings > 0
ORDER BY total_borrowings DESC;

-- ----------------------------------------------------------------------------
-- 5. Monthly Lending Volume Trend (Date Function & Time Series Aggregation)
-- ----------------------------------------------------------------------------
SELECT 
    strftime('%Y-%m', t.issue_date) AS issue_month,
    COUNT(t.transaction_id) AS monthly_loan_count,
    SUM(CASE WHEN t.status = 'RETURNED' AND t.fine_amount > 0 THEN 1 ELSE 0 END) AS late_returns_count,
    SUM(t.fine_amount) AS total_fines_inr
FROM transactions t
GROUP BY strftime('%Y-%m', t.issue_date)
ORDER BY issue_month ASC;

-- ----------------------------------------------------------------------------
-- 6. Top 5 Most Borrowed Books (Subquery with Aggregate Ranking)
-- ----------------------------------------------------------------------------
SELECT 
    b.book_id,
    b.title,
    a.author_name,
    c.category_name,
    (SELECT COUNT(*) FROM transactions t WHERE t.book_id = b.book_id) AS borrow_count
FROM books b
JOIN authors a ON b.author_id = a.author_id
JOIN categories c ON b.category_id = c.category_id
ORDER BY borrow_count DESC
LIMIT 5;

-- ----------------------------------------------------------------------------
-- 7. Active Borrowers Summary (Verification of Max 3 Books Constraint)
-- ----------------------------------------------------------------------------
SELECT 
    s.student_id,
    s.register_no,
    s.name,
    COUNT(t.transaction_id) AS current_active_loans
FROM students s
LEFT JOIN transactions t ON s.student_id = t.student_id AND t.status = 'ISSUED'
GROUP BY s.student_id, s.register_no, s.name
ORDER BY current_active_loans DESC;
