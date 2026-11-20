package com.library.dao;

import com.library.model.*;
import com.library.util.DateUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures database tables exist and seeds sample records on first run.
 */
public class DatabaseInitializer {

    public static void initialize(BookDAO bookDAO, StudentDAO studentDAO, TransactionDAO transactionDAO) {
        initialize(bookDAO, studentDAO, transactionDAO, null, null);
    }

    public static void initialize(BookDAO bookDAO, StudentDAO studentDAO, TransactionDAO transactionDAO, CategoryDAO categoryDAO, AuthorDAO authorDAO) {
        // Prepare standard seed datasets
        List<Category> categories = getInitialCategories();
        List<Author> authors = getInitialAuthors();
        List<Book> books = getInitialBooks();
        List<Student> students = getInitialStudents();
        List<BorrowTransaction> transactions = getInitialTransactions();

        if (DatabaseConnection.isDriverAvailable()) {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Create Categories
                stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                        "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "category_name TEXT NOT NULL UNIQUE, " +
                        "description TEXT);");

                // Create Authors
                stmt.execute("CREATE TABLE IF NOT EXISTS authors (" +
                        "author_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "author_name TEXT NOT NULL, " +
                        "email TEXT UNIQUE);");

                // Create Books
                stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                        "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "isbn TEXT NOT NULL UNIQUE, " +
                        "title TEXT NOT NULL, " +
                        "category_id INTEGER NOT NULL, " +
                        "author_id INTEGER NOT NULL, " +
                        "total_copies INTEGER NOT NULL CHECK (total_copies >= 0), " +
                        "available_copies INTEGER NOT NULL CHECK (available_copies >= 0 AND available_copies <= total_copies), " +
                        "FOREIGN KEY (category_id) REFERENCES categories (category_id), " +
                        "FOREIGN KEY (author_id) REFERENCES authors (author_id));");

                // Create Students
                stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                        "student_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "register_no TEXT NOT NULL UNIQUE, " +
                        "name TEXT NOT NULL, " +
                        "department TEXT NOT NULL, " +
                        "year INTEGER NOT NULL CHECK (year BETWEEN 1 AND 4), " +
                        "email TEXT NOT NULL UNIQUE);");

                // Create Transactions
                stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                        "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "student_id INTEGER NOT NULL, " +
                        "book_id INTEGER NOT NULL, " +
                        "issue_date TEXT NOT NULL, " +
                        "due_date TEXT NOT NULL, " +
                        "return_date TEXT, " +
                        "fine_amount REAL NOT NULL DEFAULT 0.0 CHECK (fine_amount >= 0.0), " +
                        "status TEXT NOT NULL CHECK (status IN ('ISSUED', 'RETURNED')), " +
                        "FOREIGN KEY (student_id) REFERENCES students (student_id), " +
                        "FOREIGN KEY (book_id) REFERENCES books (book_id));");

                // Create View for Python Analytics
                stmt.execute("CREATE VIEW IF NOT EXISTS vw_circulation_analytics AS " +
                        "SELECT t.transaction_id, t.issue_date, t.due_date, t.return_date, t.status, t.fine_amount, " +
                        "s.student_id, s.register_no, s.name AS student_name, s.department, s.year, " +
                        "b.book_id, b.isbn, b.title AS book_title, c.category_name, a.author_name " +
                        "FROM transactions t " +
                        "JOIN students s ON t.student_id = s.student_id " +
                        "JOIN books b ON t.book_id = b.book_id " +
                        "JOIN categories c ON b.category_id = c.category_id " +
                        "JOIN authors a ON b.author_id = a.author_id;");

                // Seed categories if empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories;")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (Category c : categories) {
                            stmt.execute(String.format("INSERT INTO categories (category_id, category_name, description) VALUES (%d, '%s', '%s');",
                                    c.getCategoryId(), c.getCategoryName().replace("'", "''"), c.getDescription().replace("'", "''")));
                        }
                    }
                }

                // Seed authors if empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM authors;")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (Author a : authors) {
                            stmt.execute(String.format("INSERT INTO authors (author_id, author_name, email) VALUES (%d, '%s', '%s');",
                                    a.getAuthorId(), a.getAuthorName().replace("'", "''"), a.getEmail()));
                        }
                    }
                }

                // Seed books if empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books;")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (Book b : books) {
                            stmt.execute(String.format("INSERT INTO books (book_id, isbn, title, category_id, author_id, total_copies, available_copies) " +
                                            "VALUES (%d, '%s', '%s', %d, %d, %d, %d);",
                                    b.getBookId(), b.getIsbn(), b.getTitle().replace("'", "''"),
                                    b.getCategoryId(), b.getAuthorId(), b.getTotalCopies(), b.getAvailableCopies()));
                        }
                    }
                }

                // Seed students if empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM students;")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (Student s : students) {
                            stmt.execute(String.format("INSERT INTO students (student_id, register_no, name, department, year, email) " +
                                            "VALUES (%d, '%s', '%s', '%s', %d, '%s');",
                                    s.getStudentId(), s.getRegisterNo(), s.getName().replace("'", "''"),
                                    s.getDepartment(), s.getYear(), s.getEmail()));
                        }
                    }
                }

                // Seed transactions if empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions;")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (BorrowTransaction t : transactions) {
                            String retDateVal = t.getReturnDate() != null ? "'" + DateUtil.format(t.getReturnDate()) + "'" : "NULL";
                            stmt.execute(String.format("INSERT INTO transactions (transaction_id, student_id, book_id, issue_date, due_date, return_date, fine_amount, status) " +
                                            "VALUES (%d, %d, %d, '%s', '%s', %s, %.2f, '%s');",
                                    t.getTransactionId(), t.getStudentId(), t.getBookId(),
                                    DateUtil.format(t.getIssueDate()), DateUtil.format(t.getDueDate()),
                                    retDateVal, t.getFineAmount(), t.getStatus()));
                        }
                    }
                }

            } catch (Exception ignored) {
            }
        }

        // Always seed memory stores for instantaneous in-memory execution/fallback
        bookDAO.seedMemoryStore(books);
        studentDAO.seedMemoryStore(students);
        transactionDAO.seedMemoryStore(transactions);
        if (categoryDAO != null) categoryDAO.seedMemoryStore(categories);
        if (authorDAO != null) authorDAO.seedMemoryStore(authors);
    }

    private static List<Category> getInitialCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category(1, "Database Systems", "Relational, NoSQL, query processing, and database design"));
        list.add(new Category(2, "Java & OOP", "Object-oriented programming, design patterns, and JVM systems"));
        list.add(new Category(3, "Data Science & AI", "Machine learning, statistics, data analytics, and deep learning"));
        list.add(new Category(4, "Software Engineering", "System architecture, design patterns, testing, and lifecycle models"));
        list.add(new Category(5, "Computer Networks", "Network protocols, socket programming, and distributed systems"));
        list.add(new Category(6, "Algorithms & Data Structures", "Algorithmic design, computational complexity, and abstract data types"));
        list.add(new Category(7, "Mathematics for Computing", "Discrete mathematics, linear algebra, and probability"));
        list.add(new Category(8, "Cloud & DevOps", "Containerization, microservices, CI/CD, and cloud platforms"));
        return list;
    }

    private static List<Author> getInitialAuthors() {
        List<Author> list = new ArrayList<>();
        list.add(new Author(1, "Abraham Silberschatz", "silberschatz@db-book.com"));
        list.add(new Author(2, "Herbert Schildt", "h.schildt@java-complete.org"));
        list.add(new Author(3, "Wes McKinney", "wes@pandas.pydata.org"));
        list.add(new Author(4, "Roger S. Pressman", "pressman@rspa.com"));
        list.add(new Author(5, "Thomas H. Cormen", "cormen@mit.edu"));
        list.add(new Author(6, "Andrew S. Tanenbaum", "ast@cs.vu.nl"));
        list.add(new Author(7, "Joshua Bloch", "joshua.bloch@effectivejava.com"));
        list.add(new Author(8, "Stuart Russell", "russell@cs.berkeley.edu"));
        list.add(new Author(9, "E. Balagurusamy", "balagurusamy@edu.org"));
        list.add(new Author(10, "Robert C. Martin", "unclebob@cleancoder.com"));
        list.add(new Author(11, "Kenneth H. Rosen", "rosen@discrete-math.org"));
        list.add(new Author(12, "Martin Fowler", "fowler@martinfowler.com"));
        return list;
    }

    private static List<Book> getInitialBooks() {
        List<Book> list = new ArrayList<>();
        list.add(createBook(1, "978-0078022159", "Database System Concepts (7th Ed)", 1, "Database Systems", 1, "Abraham Silberschatz", 6, 5));
        list.add(createBook(2, "978-1260440232", "Java: The Complete Reference (12th Ed)", 2, "Java & OOP", 2, "Herbert Schildt", 8, 6));
        list.add(createBook(3, "978-1491957661", "Python for Data Analysis (3rd Ed)", 3, "Data Science & AI", 3, "Wes McKinney", 7, 4));
        list.add(createBook(4, "978-0078022128", "Software Engineering: A Practitioner Approach", 4, "Software Engineering", 4, "Roger S. Pressman", 5, 4));
        list.add(createBook(5, "978-0262033848", "Introduction to Algorithms (CLRS 3rd Ed)", 6, "Algorithms & Data Structures", 5, "Thomas H. Cormen", 6, 3));
        list.add(createBook(6, "978-0132126953", "Computer Networks (5th Ed)", 5, "Computer Networks", 6, "Andrew S. Tanenbaum", 5, 5));
        list.add(createBook(7, "978-0134685991", "Effective Java (3rd Ed)", 2, "Java & OOP", 7, "Joshua Bloch", 4, 3));
        list.add(createBook(8, "978-0136042594", "Artificial Intelligence: A Modern Approach", 3, "Data Science & AI", 8, "Stuart Russell", 4, 1));
        list.add(createBook(9, "978-9351341772", "Programming with Java", 2, "Java & OOP", 9, "E. Balagurusamy", 7, 6));
        list.add(createBook(10, "978-0132350884", "Clean Code: A Handbook of Agile Craftsmanship", 4, "Software Engineering", 10, "Robert C. Martin", 5, 3));
        list.add(createBook(11, "978-0073383095", "Discrete Mathematics and Its Applications", 7, "Mathematics for Computing", 11, "Kenneth H. Rosen", 6, 5));
        list.add(createBook(12, "978-0321127426", "Patterns of Enterprise Application Architecture", 4, "Software Engineering", 12, "Martin Fowler", 4, 4));
        list.add(createBook(13, "978-1449369415", "Hands-On Machine Learning with Scikit-Learn", 3, "Data Science & AI", 3, "Wes McKinney", 5, 2));
        list.add(createBook(14, "978-0596009205", "Head First Design Patterns", 2, "Java & OOP", 2, "Herbert Schildt", 6, 4));
        list.add(createBook(15, "978-1492040767", "Kubernetes: Up and Running", 8, "Cloud & DevOps", 12, "Martin Fowler", 4, 4));
        return list;
    }

    private static Book createBook(int id, String isbn, String title, int catId, String catName, int authId, String authName, int total, int avail) {
        Book b = new Book(id, isbn, title, catId, authId, total, avail);
        b.setCategoryName(catName);
        b.setAuthorName(authName);
        return b;
    }

    private static List<Student> getInitialStudents() {
        List<Student> list = new ArrayList<>();
        list.add(new Student(1, "732925ITR004", "Abishek", "Information Technology", 3, "abishek.25it@vcet.ac.in"));
        list.add(new Student(2, "732925ITR034", "Kavya M", "Information Technology", 3, "kavya.25it@vcet.ac.in"));
        list.add(new Student(3, "732925ITR064", "Praveen S", "Information Technology", 3, "praveen.25it@vcet.ac.in"));
        list.add(new Student(4, "732925ITR094", "Sowmya K", "Information Technology", 3, "sowmya.25it@vcet.ac.in"));
        list.add(new Student(5, "732925ITR012", "Arun Kumar V", "Information Technology", 3, "arun.25it@vcet.ac.in"));
        list.add(new Student(6, "732925ITR045", "Dharani R", "Information Technology", 3, "dharani.25it@vcet.ac.in"));
        list.add(new Student(7, "732925ITR078", "Sanjay P", "Information Technology", 3, "sanjay.25it@vcet.ac.in"));
        list.add(new Student(8, "732925ITR088", "Sneha B", "Information Technology", 3, "sneha.25it@vcet.ac.in"));
        list.add(new Student(9, "732925ITR023", "Gokul R", "Information Technology", 3, "gokul.25it@vcet.ac.in"));
        list.add(new Student(10, "732925ITR052", "Monika S", "Information Technology", 3, "monika.25it@vcet.ac.in"));
        return list;
    }

    private static List<BorrowTransaction> getInitialTransactions() {
        List<BorrowTransaction> list = new ArrayList<>();
        list.add(createTx(1, 1, "Abishek", "732925ITR004", 1, "Database System Concepts (7th Ed)", "2026-01-05", "2026-01-19", "2026-01-18", 0.0, "RETURNED"));
        list.add(createTx(2, 2, "Kavya M", "732925ITR034", 2, "Java: The Complete Reference (12th Ed)", "2026-01-08", "2026-01-22", "2026-01-25", 6.0, "RETURNED"));
        list.add(createTx(3, 3, "Praveen S", "732925ITR064", 5, "Introduction to Algorithms (CLRS 3rd Ed)", "2026-01-12", "2026-01-26", "2026-01-26", 0.0, "RETURNED"));
        list.add(createTx(4, 4, "Sowmya K", "732925ITR094", 3, "Python for Data Analysis (3rd Ed)", "2026-02-02", "2026-02-16", "2026-02-15", 0.0, "RETURNED"));
        list.add(createTx(5, 5, "Arun Kumar V", "732925ITR012", 4, "Software Engineering: A Practitioner Approach", "2026-02-05", "2026-02-19", "2026-02-23", 8.0, "RETURNED"));
        list.add(createTx(6, 1, "Abishek", "732925ITR004", 7, "Effective Java (3rd Ed)", "2026-02-10", "2026-02-24", "2026-02-22", 0.0, "RETURNED"));
        list.add(createTx(7, 6, "Dharani R", "732925ITR045", 8, "Artificial Intelligence: A Modern Approach", "2026-02-15", "2026-03-01", "2026-03-05", 8.0, "RETURNED"));
        list.add(createTx(8, 2, "Kavya M", "732925ITR034", 10, "Clean Code: A Handbook of Agile Craftsmanship", "2026-03-03", "2026-03-17", "2026-03-16", 0.0, "RETURNED"));
        list.add(createTx(9, 7, "Sanjay P", "732925ITR078", 3, "Python for Data Analysis (3rd Ed)", "2026-03-08", "2026-03-22", "2026-03-27", 10.0, "RETURNED"));
        list.add(createTx(10, 8, "Sneha B", "732925ITR088", 1, "Database System Concepts (7th Ed)", "2026-03-12", "2026-03-26", "2026-03-25", 0.0, "RETURNED"));
        list.add(createTx(11, 3, "Praveen S", "732925ITR064", 2, "Java: The Complete Reference (12th Ed)", "2026-03-15", "2026-03-29", "2026-03-28", 0.0, "RETURNED"));
        list.add(createTx(12, 1, "Abishek", "732925ITR004", 5, "Introduction to Algorithms (CLRS 3rd Ed)", "2026-04-04", "2026-04-18", "2026-04-17", 0.0, "RETURNED"));
        list.add(createTx(13, 9, "Gokul R", "732925ITR023", 11, "Discrete Mathematics and Its Applications", "2026-04-07", "2026-04-21", "2026-04-21", 0.0, "RETURNED"));
        list.add(createTx(14, 4, "Sowmya K", "732925ITR094", 13, "Hands-On Machine Learning with Scikit-Learn", "2026-04-14", "2026-04-28", "2026-05-03", 10.0, "RETURNED"));
        list.add(createTx(15, 10, "Monika S", "732925ITR052", 14, "Head First Design Patterns", "2026-05-02", "2026-05-16", "2026-05-15", 0.0, "RETURNED"));
        list.add(createTx(16, 5, "Arun Kumar V", "732925ITR012", 3, "Python for Data Analysis (3rd Ed)", "2026-05-10", "2026-05-24", "2026-05-24", 0.0, "RETURNED"));
        list.add(createTx(17, 2, "Kavya M", "732925ITR034", 1, "Database System Concepts (7th Ed)", "2026-05-18", "2026-06-01", "2026-06-06", 10.0, "RETURNED"));
        list.add(createTx(18, 6, "Dharani R", "732925ITR045", 2, "Java: The Complete Reference (12th Ed)", "2026-06-03", "2026-06-17", "2026-06-16", 0.0, "RETURNED"));
        list.add(createTx(19, 7, "Sanjay P", "732925ITR078", 5, "Introduction to Algorithms (CLRS 3rd Ed)", "2026-06-11", "2026-06-25", "2026-06-24", 0.0, "RETURNED"));
        list.add(createTx(20, 1, "Abishek", "732925ITR004", 3, "Python for Data Analysis (3rd Ed)", "2026-06-20", "2026-07-04", "2026-07-02", 0.0, "RETURNED"));
        list.add(createTx(21, 3, "Praveen S", "732925ITR064", 8, "Artificial Intelligence: A Modern Approach", "2026-07-05", "2026-07-19", "2026-07-19", 0.0, "RETURNED"));
        list.add(createTx(22, 8, "Sneha B", "732925ITR088", 4, "Software Engineering: A Practitioner Approach", "2026-07-12", "2026-07-26", "2026-07-30", 8.0, "RETURNED"));
        list.add(createTx(23, 4, "Sowmya K", "732925ITR094", 10, "Clean Code: A Handbook of Agile Craftsmanship", "2026-07-18", "2026-08-01", "2026-07-31", 0.0, "RETURNED"));
        list.add(createTx(24, 9, "Gokul R", "732925ITR023", 3, "Python for Data Analysis (3rd Ed)", "2026-08-02", "2026-08-16", "2026-08-15", 0.0, "RETURNED"));
        list.add(createTx(25, 10, "Monika S", "732925ITR052", 13, "Hands-On Machine Learning with Scikit-Learn", "2026-08-10", "2026-08-24", "2026-08-28", 8.0, "RETURNED"));
        list.add(createTx(26, 2, "Kavya M", "732925ITR034", 7, "Effective Java (3rd Ed)", "2026-08-19", "2026-09-02", "2026-09-01", 0.0, "RETURNED"));
        list.add(createTx(27, 5, "Arun Kumar V", "732925ITR012", 2, "Java: The Complete Reference (12th Ed)", "2026-09-04", "2026-09-18", "2026-09-18", 0.0, "RETURNED"));
        list.add(createTx(28, 1, "Abishek", "732925ITR004", 14, "Head First Design Patterns", "2026-09-12", "2026-09-26", "2026-09-25", 0.0, "RETURNED"));
        list.add(createTx(29, 6, "Dharani R", "732925ITR045", 5, "Introduction to Algorithms (CLRS 3rd Ed)", "2026-09-21", "2026-10-05", "2026-10-10", 10.0, "RETURNED"));
        list.add(createTx(30, 3, "Praveen S", "732925ITR064", 3, "Python for Data Analysis (3rd Ed)", "2026-10-03", "2026-10-17", "2026-10-16", 0.0, "RETURNED"));
        list.add(createTx(31, 7, "Sanjay P", "732925ITR078", 8, "Artificial Intelligence: A Modern Approach", "2026-10-10", "2026-10-24", "2026-10-23", 0.0, "RETURNED"));
        list.add(createTx(32, 4, "Sowmya K", "732925ITR094", 1, "Database System Concepts (7th Ed)", "2026-10-15", "2026-10-29", "2026-11-03", 10.0, "RETURNED"));
        list.add(createTx(33, 8, "Sneha B", "732925ITR088", 10, "Clean Code: A Handbook of Agile Craftsmanship", "2026-10-25", "2026-11-08", "2026-11-12", 8.0, "RETURNED"));

        // Active / Overdue loans
        list.add(createTx(34, 1, "Abishek", "732925ITR004", 5, "Introduction to Algorithms (CLRS 3rd Ed)", "2026-11-01", "2026-11-15", null, 0.0, "ISSUED"));
        list.add(createTx(35, 2, "Kavya M", "732925ITR034", 8, "Artificial Intelligence: A Modern Approach", "2026-11-02", "2026-11-16", null, 0.0, "ISSUED"));
        list.add(createTx(36, 3, "Praveen S", "732925ITR064", 3, "Python for Data Analysis (3rd Ed)", "2026-11-08", "2026-11-22", null, 0.0, "ISSUED"));
        list.add(createTx(37, 4, "Sowmya K", "732925ITR094", 2, "Java: The Complete Reference (12th Ed)", "2026-11-10", "2026-11-24", null, 0.0, "ISSUED"));
        list.add(createTx(38, 5, "Arun Kumar V", "732925ITR012", 13, "Hands-On Machine Learning with Scikit-Learn", "2026-11-12", "2026-11-26", null, 0.0, "ISSUED"));
        return list;
    }

    private static BorrowTransaction createTx(int id, int sId, String sName, String regNo, int bId, String bTitle,
                                              String issue, String due, String ret, double fine, String status) {
        BorrowTransaction t = new BorrowTransaction(id, sId, bId,
                DateUtil.parse(issue), DateUtil.parse(due), DateUtil.parse(ret), fine, status);
        t.setStudentName(sName);
        t.setRegisterNo(regNo);
        t.setBookTitle(bTitle);
        return t;
    }
}
