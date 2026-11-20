package com.library.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for date parsing, validation, and overdue computations.
 */
public class DateUtil {
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final int STANDARD_LOAN_DAYS = 14;
    public static final double FINE_RATE_PER_DAY = 2.0; // ₹2.00 per day

    public static String format(LocalDate date) {
        return date != null ? date.format(ISO_FORMATTER) : "N/A";
    }

    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDate calculateDueDate(LocalDate issueDate) {
        return issueDate.plusDays(STANDARD_LOAN_DAYS);
    }

    public static long calculateOverdueDays(LocalDate dueDate, LocalDate returnDate) {
        if (returnDate == null || !returnDate.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, returnDate);
    }

    public static double calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long days = calculateOverdueDays(dueDate, returnDate);
        return days * FINE_RATE_PER_DAY;
    }
}
