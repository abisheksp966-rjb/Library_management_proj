package com.library.exception;

/**
 * Thrown when a student attempts to borrow more books than the permitted limit (Max: 3 active loans).
 */
public class StudentLimitExceededException extends LibraryException {
    private final int studentId;
    private final String registerNo;
    private final int currentActiveCount;
    private final int maxLimit;

    public StudentLimitExceededException(int studentId, String registerNo, int currentActiveCount, int maxLimit) {
        super(String.format("Student [ID: %d, Reg: %s] has reached maximum borrow limit (%d/%d books). Return a book before borrowing again.",
                studentId, registerNo, currentActiveCount, maxLimit));
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.currentActiveCount = currentActiveCount;
        this.maxLimit = maxLimit;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public int getCurrentActiveCount() {
        return currentActiveCount;
    }

    public int getMaxLimit() {
        return maxLimit;
    }
}
