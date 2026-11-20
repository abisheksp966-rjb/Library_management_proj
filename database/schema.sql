-- ============================================================================
-- Project: Library Book Lending and Usage Analysis System
-- Academic Year: 2026-2027 (Odd Semester)
-- Course: 25ITT31 - Database Management Systems
-- Student: Abishek (732925ITR004), Class: 25IT3A
-- Department: Information Technology, VCET
-- Database Engine: SQLite 3.x
-- ============================================================================

PRAGMA foreign_keys = ON;

-- ----------------------------------------------------------------------------
-- Table: categories
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_name TEXT NOT NULL UNIQUE,
    description TEXT
);

-- ----------------------------------------------------------------------------
-- Table: authors
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS authors (
    author_id INTEGER PRIMARY KEY AUTOINCREMENT,
    author_name TEXT NOT NULL,
    email TEXT UNIQUE
);

-- ----------------------------------------------------------------------------
-- Table: books
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    total_copies INTEGER NOT NULL CHECK (total_copies >= 0),
    available_copies INTEGER NOT NULL CHECK (available_copies >= 0 AND available_copies <= total_copies),
    FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE RESTRICT,
    FOREIGN KEY (author_id) REFERENCES authors (author_id) ON DELETE RESTRICT
);

-- ----------------------------------------------------------------------------
-- Table: students
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    student_id INTEGER PRIMARY KEY AUTOINCREMENT,
    register_no TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    department TEXT NOT NULL,
    year INTEGER NOT NULL CHECK (year BETWEEN 1 AND 4),
    email TEXT NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- Table: transactions (Borrow / Return records)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    issue_date TEXT NOT NULL,       -- ISO-8601 Format: YYYY-MM-DD
    due_date TEXT NOT NULL,         -- ISO-8601 Format: YYYY-MM-DD
    return_date TEXT,               -- ISO-8601 Format: YYYY-MM-DD (NULL if currently issued)
    fine_amount REAL NOT NULL DEFAULT 0.0 CHECK (fine_amount >= 0.0),
    status TEXT NOT NULL CHECK (status IN ('ISSUED', 'RETURNED')),
    FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE RESTRICT,
    FOREIGN KEY (book_id) REFERENCES books (book_id) ON DELETE RESTRICT
);

-- ----------------------------------------------------------------------------
-- Performance Indexes
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_books_category ON books (category_id);
CREATE INDEX IF NOT EXISTS idx_books_author ON books (author_id);
CREATE INDEX IF NOT EXISTS idx_trans_student ON transactions (student_id);
CREATE INDEX IF NOT EXISTS idx_trans_book ON transactions (book_id);
CREATE INDEX IF NOT EXISTS idx_trans_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_trans_issue_date ON transactions (issue_date);

-- ----------------------------------------------------------------------------
-- Analytical Views
-- ----------------------------------------------------------------------------

-- 1. Full Circulation Analytics View (Used by Python Data Science pipeline)
CREATE VIEW IF NOT EXISTS vw_circulation_analytics AS
SELECT 
    t.transaction_id,
    t.issue_date,
    t.due_date,
    t.return_date,
    t.status,
    t.fine_amount,
    s.student_id,
    s.register_no,
    s.name AS student_name,
    s.department,
    s.year,
    b.book_id,
    b.isbn,
    b.title AS book_title,
    c.category_name,
    a.author_name
FROM transactions t
JOIN students s ON t.student_id = s.student_id
JOIN books b ON t.book_id = b.book_id
JOIN categories c ON b.category_id = c.category_id
JOIN authors a ON b.author_id = a.author_id;

-- 2. Available Books View
CREATE VIEW IF NOT EXISTS vw_available_books AS
SELECT 
    b.book_id,
    b.isbn,
    b.title,
    c.category_name,
    a.author_name,
    b.available_copies,
    b.total_copies
FROM books b
JOIN categories c ON b.category_id = c.category_id
JOIN authors a ON b.author_id = a.author_id
WHERE b.available_copies > 0;

-- 3. Currently Overdue Loans View (assuming 2026-11-19 reference date)
CREATE VIEW IF NOT EXISTS vw_overdue_loans AS
SELECT 
    t.transaction_id,
    s.register_no,
    s.name AS student_name,
    b.title AS book_title,
    t.issue_date,
    t.due_date,
    CAST((julianday('now') - julianday(t.due_date)) AS INTEGER) AS overdue_days,
    MAX(0, CAST((julianday('now') - julianday(t.due_date)) AS INTEGER) * 2.0) AS estimated_fine
FROM transactions t
JOIN students s ON t.student_id = s.student_id
JOIN books b ON t.book_id = b.book_id
WHERE t.status = 'ISSUED' AND date(t.due_date) < date('now');
