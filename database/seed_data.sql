-- ============================================================================
-- Seed Data for Library Book Lending and Usage Analysis System
-- Academic Year: 2026-2027 (Odd Semester)
-- Student: Abishek (732925ITR004), Class: 25IT3A, VCET Erode
-- ============================================================================

-- Categories
INSERT INTO categories (category_id, category_name, description) VALUES
(1, 'Database Systems', 'Relational, NoSQL, query processing, and database design'),
(2, 'Java & OOP', 'Object-oriented programming, design patterns, and JVM systems'),
(3, 'Data Science & AI', 'Machine learning, statistics, data analytics, and deep learning'),
(4, 'Software Engineering', 'System architecture, design patterns, testing, and lifecycle models'),
(5, 'Computer Networks', 'Network protocols, socket programming, and distributed systems'),
(6, 'Algorithms & Data Structures', 'Algorithmic design, computational complexity, and abstract data types'),
(7, 'Mathematics for Computing', 'Discrete mathematics, linear algebra, and probability'),
(8, 'Cloud & DevOps', 'Containerization, microservices, CI/CD, and cloud platforms');

-- Authors
INSERT INTO authors (author_id, author_name, email) VALUES
(1, 'Abraham Silberschatz', 'silberschatz@db-book.com'),
(2, 'Herbert Schildt', 'h.schildt@java-complete.org'),
(3, 'Wes McKinney', 'wes@pandas.pydata.org'),
(4, 'Roger S. Pressman', 'pressman@rspa.com'),
(5, 'Thomas H. Cormen', 'cormen@mit.edu'),
(6, 'Andrew S. Tanenbaum', 'ast@cs.vu.nl'),
(7, 'Joshua Bloch', 'joshua.bloch@effectivejava.com'),
(8, 'Stuart Russell & Peter Norvig', 'russell@cs.berkeley.edu'),
(9, 'E. Balagurusamy', 'balagurusamy@edu.org'),
(10, 'Robert C. Martin', 'unclebob@cleancoder.com'),
(11, 'Kenneth H. Rosen', 'rosen@discrete-math.org'),
(12, 'Martin Fowler', 'fowler@martinfowler.com');

-- Books
INSERT INTO books (book_id, isbn, title, category_id, author_id, total_copies, available_copies) VALUES
(1, '978-0078022159', 'Database System Concepts (7th Ed)', 1, 1, 6, 5),
(2, '978-1260440232', 'Java: The Complete Reference (12th Ed)', 2, 2, 8, 6),
(3, '978-1491957661', 'Python for Data Analysis (3rd Ed)', 3, 3, 7, 4),
(4, '978-0078022128', 'Software Engineering: A Practitioner Approach', 4, 4, 5, 4),
(5, '978-0262033848', 'Introduction to Algorithms (CLRS 3rd Ed)', 6, 5, 6, 3),
(6, '978-0132126953', 'Computer Networks (5th Ed)', 5, 6, 5, 5),
(7, '978-0134685991', 'Effective Java (3rd Ed)', 2, 7, 4, 3),
(8, '978-0136042594', 'Artificial Intelligence: A Modern Approach', 3, 8, 4, 1),
(9, '978-9351341772', 'Programming with Java', 2, 9, 7, 6),
(10, '978-0132350884', 'Clean Code: A Handbook of Agile Craftsmanship', 4, 10, 5, 3),
(11, '978-0073383095', 'Discrete Mathematics and Its Applications', 7, 11, 6, 5),
(12, '978-0321127426', 'Patterns of Enterprise Application Architecture', 4, 12, 4, 4),
(13, '978-1449369415', 'Hands-On Machine Learning with Scikit-Learn', 3, 3, 5, 2),
(14, '978-0596009205', 'Head First Design Patterns', 2, 2, 6, 4),
(15, '978-1492040767', 'Kubernetes: Up and Running', 8, 12, 4, 4);

-- Students (Assigned Register No. 4, 34, 64, 94 included)
INSERT INTO students (student_id, register_no, name, department, year, email) VALUES
(1, '732925ITR004', 'Abishek', 'Information Technology', 3, 'abishek.25it@vcet.ac.in'),
(2, '732925ITR034', 'Kavya M', 'Information Technology', 3, 'kavya.25it@vcet.ac.in'),
(3, '732925ITR064', 'Praveen S', 'Information Technology', 3, 'praveen.25it@vcet.ac.in'),
(4, '732925ITR094', 'Sowmya K', 'Information Technology', 3, 'sowmya.25it@vcet.ac.in'),
(5, '732925ITR012', 'Arun Kumar V', 'Information Technology', 3, 'arun.25it@vcet.ac.in'),
(6, '732925ITR045', 'Dharani R', 'Information Technology', 3, 'dharani.25it@vcet.ac.in'),
(7, '732925ITR078', 'Sanjay P', 'Information Technology', 3, 'sanjay.25it@vcet.ac.in'),
(8, '732925ITR088', 'Sneha B', 'Information Technology', 3, 'sneha.25it@vcet.ac.in'),
(9, '732925ITR023', 'Gokul R', 'Information Technology', 3, 'gokul.25it@vcet.ac.in'),
(10, '732925ITR052', 'Monika S', 'Information Technology', 3, 'monika.25it@vcet.ac.in');

-- Historical & Current Borrowing Transactions
-- (Loan period = 14 days, fine rate = ₹2/day)
INSERT INTO transactions (transaction_id, student_id, book_id, issue_date, due_date, return_date, fine_amount, status) VALUES
-- January 2026
(1, 1, 1, '2026-01-05', '2026-01-19', '2026-01-18', 0.0, 'RETURNED'),
(2, 2, 2, '2026-01-08', '2026-01-22', '2026-01-25', 6.0, 'RETURNED'),   -- 3 days late = ₹6
(3, 3, 5, '2026-01-12', '2026-01-26', '2026-01-26', 0.0, 'RETURNED'),

-- February 2026
(4, 4, 3, '2026-02-02', '2026-02-16', '2026-02-15', 0.0, 'RETURNED'),
(5, 5, 4, '2026-02-05', '2026-02-19', '2026-02-23', 8.0, 'RETURNED'),   -- 4 days late = ₹8
(6, 1, 7, '2026-02-10', '2026-02-24', '2026-02-22', 0.0, 'RETURNED'),
(7, 6, 8, '2026-02-15', '2026-03-01', '2026-03-05', 8.0, 'RETURNED'),   -- 4 days late = ₹8

-- March 2026
(8, 2, 10, '2026-03-03', '2026-03-17', '2026-03-16', 0.0, 'RETURNED'),
(9, 7, 3, '2026-03-08', '2026-03-22', '2026-03-27', 10.0, 'RETURNED'),  -- 5 days late = ₹10
(10, 8, 1, '2026-03-12', '2026-03-26', '2026-03-25', 0.0, 'RETURNED'),
(11, 3, 2, '2026-03-15', '2026-03-29', '2026-03-28', 0.0, 'RETURNED'),

-- April 2026
(12, 1, 5, '2026-04-04', '2026-04-18', '2026-04-17', 0.0, 'RETURNED'),
(13, 9, 11, '2026-04-07', '2026-04-21', '2026-04-21', 0.0, 'RETURNED'),
(14, 4, 13, '2026-04-14', '2026-04-28', '2026-05-03', 10.0, 'RETURNED'), -- 5 days late = ₹10

-- May 2026
(15, 10, 14, '2026-05-02', '2026-05-16', '2026-05-15', 0.0, 'RETURNED'),
(16, 5, 3, '2026-05-10', '2026-05-24', '2026-05-24', 0.0, 'RETURNED'),
(17, 2, 1, '2026-05-18', '2026-06-01', '2026-06-06', 10.0, 'RETURNED'), -- 5 days late = ₹10

-- June 2026
(18, 6, 2, '2026-06-03', '2026-06-17', '2026-06-16', 0.0, 'RETURNED'),
(19, 7, 5, '2026-06-11', '2026-06-25', '2026-06-24', 0.0, 'RETURNED'),
(20, 1, 3, '2026-06-20', '2026-07-04', '2026-07-02', 0.0, 'RETURNED'),

-- July 2026
(21, 3, 8, '2026-07-05', '2026-07-19', '2026-07-19', 0.0, 'RETURNED'),
(22, 8, 4, '2026-07-12', '2026-07-26', '2026-07-30', 8.0, 'RETURNED'),   -- 4 days late = ₹8
(23, 4, 10, '2026-07-18', '2026-08-01', '2026-07-31', 0.0, 'RETURNED'),

-- August 2026
(24, 9, 3, '2026-08-02', '2026-08-16', '2026-08-15', 0.0, 'RETURNED'),
(25, 10, 13, '2026-08-10', '2026-08-24', '2026-08-28', 8.0, 'RETURNED'), -- 4 days late = ₹8
(26, 2, 7, '2026-08-19', '2026-09-02', '2026-09-01', 0.0, 'RETURNED'),

-- September 2026
(27, 5, 2, '2026-09-04', '2026-09-18', '2026-09-18', 0.0, 'RETURNED'),
(28, 1, 14, '2026-09-12', '2026-09-26', '2026-09-25', 0.0, 'RETURNED'),
(29, 6, 5, '2026-09-21', '2026-10-05', '2026-10-10', 10.0, 'RETURNED'), -- 5 days late = ₹10

-- October 2026
(30, 3, 3, '2026-10-03', '2026-10-17', '2026-10-16', 0.0, 'RETURNED'),
(31, 7, 8, '2026-10-10', '2026-10-24', '2026-10-23', 0.0, 'RETURNED'),
(32, 4, 1, '2026-10-15', '2026-10-29', '2026-11-03', 10.0, 'RETURNED'), -- 5 days late = ₹10
(33, 8, 10, '2026-10-25', '2026-11-08', '2026-11-12', 8.0, 'RETURNED'), -- 4 days late = ₹8

-- November 2026 (Active & Overdue Transactions)
(34, 1, 5, '2026-11-01', '2026-11-15', NULL, 0.0, 'ISSUED'),   -- Active & Overdue (due Nov 15)
(35, 2, 8, '2026-11-02', '2026-11-16', NULL, 0.0, 'ISSUED'),   -- Active & Overdue (due Nov 16)
(36, 3, 3, '2026-11-08', '2026-11-22', NULL, 0.0, 'ISSUED'),   -- Active (not yet overdue)
(37, 4, 2, '2026-11-10', '2026-11-24', NULL, 0.0, 'ISSUED'),   -- Active (not yet overdue)
(38, 5, 13, '2026-11-12', '2026-11-26', NULL, 0.0, 'ISSUED');  -- Active (not yet overdue)
