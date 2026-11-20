package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.StudentDAO;
import com.library.dao.TransactionDAO;
import com.library.model.Book;
import com.library.model.BorrowTransaction;
import com.library.model.Student;
import com.library.util.ConsoleUI;
import com.library.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Service generating operational and analytical reports.
 */
public class ReportService {
    private final BookDAO bookDAO;
    private final StudentDAO studentDAO;
    private final TransactionDAO transactionDAO;

    public ReportService(BookDAO bookDAO, StudentDAO studentDAO, TransactionDAO transactionDAO) {
        this.bookDAO = bookDAO;
        this.studentDAO = studentDAO;
        this.transactionDAO = transactionDAO;
    }

    public void displayAvailableBooks() {
        ConsoleUI.printHeader("Available Books Report (Copies > 0)");
        List<Book> books = bookDAO.getAvailableBooks();
        if (books.isEmpty()) {
            System.out.println("No books are currently available in the catalog.");
            return;
        }

        System.out.printf("%-6s | %-16s | %-42s | %-18s | %-8s\n", "ID", "ISBN", "Title", "Category", "Copies");
        ConsoleUI.printDivider();
        for (Book b : books) {
            String cat = b.getCategoryName() != null ? b.getCategoryName() : "General";
            String title = b.getTitle().length() > 40 ? b.getTitle().substring(0, 39) + "." : b.getTitle();
            System.out.printf("%-6d | %-16s | %-42s | %-18s | %d/%d\n",
                    b.getBookId(), b.getIsbn(), title, cat, b.getAvailableCopies(), b.getTotalCopies());
        }
        ConsoleUI.printDivider();
        System.out.println("Total Available Titles: " + books.size());
    }

    public void displayOverdueLoans(LocalDate referenceDate) {
        ConsoleUI.printHeader("Overdue Borrowing Records (Reference Date: " + DateUtil.format(referenceDate) + ")");
        List<BorrowTransaction> overdueList = transactionDAO.getOverdueTransactions(referenceDate);
        if (overdueList.isEmpty()) {
            System.out.println("Great news! There are currently no overdue books.");
            return;
        }

        System.out.printf("%-6s | %-14s | %-18s | %-32s | %-11s | %-8s | %-9s\n",
                "TX ID", "Reg No", "Student Name", "Book Title", "Due Date", "Overdue", "Fine (₹)");
        ConsoleUI.printDivider();
        double totalAccruedFine = 0.0;
        for (BorrowTransaction t : overdueList) {
            long days = DateUtil.calculateOverdueDays(t.getDueDate(), referenceDate);
            double fine = days * DateUtil.FINE_RATE_PER_DAY;
            totalAccruedFine += fine;
            String title = t.getBookTitle() != null && t.getBookTitle().length() > 30 ?
                    t.getBookTitle().substring(0, 29) + "." : (t.getBookTitle() != null ? t.getBookTitle() : "Book #" + t.getBookId());
            System.out.printf("%-6d | %-14s | %-18s | %-32s | %-11s | %-8d | ₹%-8.2f\n",
                    t.getTransactionId(),
                    t.getRegisterNo() != null ? t.getRegisterNo() : "-",
                    t.getStudentName() != null ? t.getStudentName() : "-",
                    title,
                    DateUtil.format(t.getDueDate()),
                    days,
                    fine);
        }
        ConsoleUI.printDivider();
        System.out.printf("Total Overdue Loans: %d | Total Estimated Fine Payable: ₹%.2f\n", overdueList.size(), totalAccruedFine);
    }

    public void displayStudentHistory(int studentId) {
        Student s = studentDAO.getStudentById(studentId);
        if (s == null) {
            ConsoleUI.printError("Student with ID " + studentId + " not found.");
            return;
        }
        ConsoleUI.printHeader("Borrowing History for: " + s.getName() + " (" + s.getRegisterNo() + ")");
        System.out.println("Department: " + s.getDepartment() + " | Year: " + s.getYear() + " | Email: " + s.getEmail());
        ConsoleUI.printDivider();

        List<BorrowTransaction> history = transactionDAO.getStudentHistory(studentId);
        if (history.isEmpty()) {
            System.out.println("No borrowing history found for this student.");
            return;
        }

        System.out.printf("%-6s | %-34s | %-11s | %-11s | %-11s | %-9s | %-8s\n",
                "TX ID", "Book Title", "Issue Date", "Due Date", "Return Date", "Status", "Fine (₹)");
        ConsoleUI.printDivider();
        for (BorrowTransaction t : history) {
            String title = t.getBookTitle() != null && t.getBookTitle().length() > 32 ?
                    t.getBookTitle().substring(0, 31) + "." : (t.getBookTitle() != null ? t.getBookTitle() : "Book #" + t.getBookId());
            String retDate = t.getReturnDate() != null ? DateUtil.format(t.getReturnDate()) : "ACTIVE";
            System.out.printf("%-6d | %-34s | %-11s | %-11s | %-11s | %-9s | ₹%-7.2f\n",
                    t.getTransactionId(), title, DateUtil.format(t.getIssueDate()),
                    DateUtil.format(t.getDueDate()), retDate, t.getStatus(), t.getFineAmount());
        }
        ConsoleUI.printDivider();
    }

    public void displayAllBooks() {
        ConsoleUI.printHeader("Master Book Catalog");
        List<Book> books = bookDAO.getAllBooks();
        System.out.printf("%-5s | %-16s | %-40s | %-18s | %-7s\n", "ID", "ISBN", "Title", "Category", "Copies");
        ConsoleUI.printDivider();
        for (Book b : books) {
            String title = b.getTitle().length() > 38 ? b.getTitle().substring(0, 37) + "." : b.getTitle();
            String cat = b.getCategoryName() != null ? b.getCategoryName() : "General";
            System.out.printf("%-5d | %-16s | %-40s | %-18s | %d/%d\n",
                    b.getBookId(), b.getIsbn(), title, cat, b.getAvailableCopies(), b.getTotalCopies());
        }
        ConsoleUI.printDivider();
        System.out.println("Total Catalog Titles: " + books.size());
    }

    public void displayAllStudents() {
        ConsoleUI.printHeader("Registered Student Members");
        List<Student> students = studentDAO.getAllStudents();
        System.out.printf("%-5s | %-15s | %-22s | %-24s | %-5s\n", "ID", "Register No", "Name", "Department", "Year");
        ConsoleUI.printDivider();
        for (Student s : students) {
            System.out.printf("%-5d | %-15s | %-22s | %-24s | %-5d\n",
                    s.getStudentId(), s.getRegisterNo(), s.getName(), s.getDepartment(), s.getYear());
        }
        ConsoleUI.printDivider();
        System.out.println("Total Students Registered: " + students.size());
    }
}
