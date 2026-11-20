package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.StudentDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.BookUnavailableException;
import com.library.exception.LibraryException;
import com.library.exception.RecordNotFoundException;
import com.library.exception.StudentLimitExceededException;
import com.library.model.Book;
import com.library.model.BorrowTransaction;
import com.library.model.Student;
import com.library.util.DateUtil;

import java.time.LocalDate;

/**
 * Service managing library circulation business logic.
 * Enforces business rules:
 *  - 14-day borrowing duration
 *  - Maximum 3 active borrowed books per student
 *  - Non-negative stock enforcement
 *  - ₹2.00 / day overdue fine
 */
public class LibraryService {
    public static final int MAX_ACTIVE_LOANS_PER_STUDENT = 3;
    public static final double OVERDUE_FINE_PER_DAY = 2.0;

    private final BookDAO bookDAO;
    private final StudentDAO studentDAO;
    private final TransactionDAO transactionDAO;

    public LibraryService(BookDAO bookDAO, StudentDAO studentDAO, TransactionDAO transactionDAO) {
        this.bookDAO = bookDAO;
        this.studentDAO = studentDAO;
        this.transactionDAO = transactionDAO;
    }

    /**
     * Issues a book to a registered student after validating availability and borrow limits.
     */
    public synchronized BorrowTransaction issueBook(int studentId, int bookId, LocalDate issueDate) throws LibraryException {
        // 1. Verify student existence
        Student student = studentDAO.getStudentById(studentId);
        if (student == null) {
            throw new RecordNotFoundException("Student", String.valueOf(studentId));
        }

        // 2. Check active loan limit (FR3: Max 3 books)
        int activeLoans = transactionDAO.getActiveCountForStudent(studentId);
        if (activeLoans >= MAX_ACTIVE_LOANS_PER_STUDENT) {
            throw new StudentLimitExceededException(studentId, student.getRegisterNo(), activeLoans, MAX_ACTIVE_LOANS_PER_STUDENT);
        }

        // 3. Prevent duplicate active issuance of the exact same book
        BorrowTransaction existingActive = transactionDAO.getActiveTransaction(studentId, bookId);
        if (existingActive != null) {
            throw new LibraryException(String.format("Student %s already holds an unreturned copy of this book (Transaction #%d).",
                    student.getRegisterNo(), existingActive.getTransactionId()));
        }

        // 4. Verify book existence and stock (FR3: Prevent issuing unavailable copies)
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new RecordNotFoundException("Book", String.valueOf(bookId));
        }
        if (!book.isAvailable()) {
            throw new BookUnavailableException(bookId, book.getTitle());
        }

        // 5. Decrement stock
        boolean stockUpdated = bookDAO.updateAvailableCopies(bookId, -1);
        if (!stockUpdated) {
            throw new LibraryException("Failed to allocate book copy from inventory.");
        }

        // 6. Record transaction
        LocalDate dueDate = DateUtil.calculateDueDate(issueDate);
        BorrowTransaction transaction = new BorrowTransaction(
                0,
                studentId,
                bookId,
                issueDate,
                dueDate,
                null,
                0.0,
                "ISSUED"
        );
        transaction.setStudentName(student.getName());
        transaction.setRegisterNo(student.getRegisterNo());
        transaction.setBookTitle(book.getTitle());

        int transactionId = transactionDAO.createTransaction(transaction);
        transaction.setTransactionId(transactionId);
        return transaction;
    }

    /**
     * Returns a borrowed book and calculates overdue fine.
     */
    public synchronized double returnBook(int transactionId, LocalDate returnDate) throws LibraryException {
        BorrowTransaction transaction = transactionDAO.getTransactionById(transactionId);
        if (transaction == null) {
            throw new RecordNotFoundException("Transaction", String.valueOf(transactionId));
        }

        if (!"ISSUED".equalsIgnoreCase(transaction.getStatus())) {
            throw new LibraryException("Transaction #" + transactionId + " is already marked as RETURNED.");
        }

        // Calculate fine (FR4: ₹2/day for late returns)
        double fine = DateUtil.calculateFine(transaction.getDueDate(), returnDate);

        // Update transaction status
        boolean updated = transactionDAO.updateReturn(transactionId, returnDate, fine);
        if (!updated) {
            throw new LibraryException("Failed to update transaction return status.");
        }

        // Increment inventory
        bookDAO.updateAvailableCopies(transaction.getBookId(), 1);

        return fine;
    }

    public synchronized double returnBookByStudentAndBook(int studentId, int bookId, LocalDate returnDate) throws LibraryException {
        BorrowTransaction transaction = transactionDAO.getActiveTransaction(studentId, bookId);
        if (transaction == null) {
            throw new RecordNotFoundException("Active Transaction", "Student " + studentId + " / Book " + bookId);
        }
        return returnBook(transaction.getTransactionId(), returnDate);
    }
}
