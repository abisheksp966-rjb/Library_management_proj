package com.library.exception;

/**
 * Thrown when a queried entity (Book, Student, Transaction) is not found in the database.
 */
public class RecordNotFoundException extends LibraryException {
    public RecordNotFoundException(String entityName, String identifier) {
        super(String.format("%s with identifier '%s' was not found in the records.", entityName, identifier));
    }
}
