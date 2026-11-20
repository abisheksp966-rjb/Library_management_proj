package com.library.util;

import java.util.regex.Pattern;

/**
 * Validation helpers for user input.
 */
public class InputValidator {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^(97[89])?[0-9\\-]{9,17}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidRegisterNo(String regNo) {
        return regNo != null && regNo.trim().length() >= 4 && regNo.trim().matches("^[A-Za-z0-9]+$");
    }

    public static boolean isValidYear(int year) {
        return year >= 1 && year <= 4;
    }

    public static boolean isValidCopies(int total, int available) {
        return total >= 0 && available >= 0 && available <= total;
    }
}
