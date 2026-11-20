package com.library.exception;

/**
 * Thrown when an operation attempts to issue a book that has zero available copies.
 */
public class BookUnavailableException extends LibraryException {
    private final int bookId;
    private final String title;

    public BookUnavailableException(int bookId, String title) {
        super(String.format("Book [ID: %d, Title: '%s'] is currently UNAVAILABLE (0 copies in stock).", bookId, title));
        this.bookId = bookId;
        this.title = title;
    }

    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
