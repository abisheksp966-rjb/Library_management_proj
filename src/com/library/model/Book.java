package com.library.model;

/**
 * Model representing a Book entity in the library catalog.
 */
public class Book {
    private int bookId;
    private String isbn;
    private String title;
    private int categoryId;
    private int authorId;
    private String categoryName;
    private String authorName;
    private int totalCopies;
    private int availableCopies;

    public Book() {}

    public Book(int bookId, String isbn, String title, int categoryId, int authorId, int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public boolean isAvailable() {
        return this.availableCopies > 0;
    }

    public void decrementCopies() {
        if (this.availableCopies > 0) {
            this.availableCopies--;
        }
    }

    public void incrementCopies() {
        if (this.availableCopies < this.totalCopies) {
            this.availableCopies++;
        }
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] '%s' | Author: %s | Cat: %s | Avail: %d/%d",
                bookId, title, (authorName != null ? authorName : authorId),
                (categoryName != null ? categoryName : categoryId), availableCopies, totalCopies);
    }
}
