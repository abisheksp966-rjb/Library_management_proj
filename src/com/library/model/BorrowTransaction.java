package com.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Model representing a book borrowing transaction.
 */
public class BorrowTransaction {
    private int transactionId;
    private int studentId;
    private int bookId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private String status; // 'ISSUED' or 'RETURNED'

    // Auxiliary display fields
    private String studentName;
    private String registerNo;
    private String bookTitle;

    public BorrowTransaction() {}

    public BorrowTransaction(int transactionId, int studentId, int bookId, LocalDate issueDate,
                             LocalDate dueDate, LocalDate returnDate, double fineAmount, String status) {
        this.transactionId = transactionId;
        this.studentId = studentId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
    }

    /**
     * Determines whether this transaction is overdue relative to a given date.
     */
    public boolean isOverdue(LocalDate referenceDate) {
        if ("RETURNED".equalsIgnoreCase(status)) {
            return returnDate != null && returnDate.isAfter(dueDate);
        }
        return referenceDate != null && referenceDate.isAfter(dueDate);
    }

    /**
     * Calculates overdue fine based on a fixed rate per day.
     * Overdue Days = max(0, returnDate - dueDate)
     * Fine = Overdue Days * ratePerDay
     */
    public static double calculateFine(LocalDate dueDate, LocalDate actualReturnDate, double ratePerDay) {
        if (actualReturnDate == null || !actualReturnDate.isAfter(dueDate)) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, actualReturnDate);
        return days > 0 ? days * ratePerDay : 0.0;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    @Override
    public String toString() {
        return String.format("[TX #%d] Student: %s (%s) | Book: '%s' | Issued: %s | Due: %s | Status: %s | Fine: ₹%.2f",
                transactionId, studentName, registerNo, bookTitle, issueDate, dueDate, status, fineAmount);
    }
}
