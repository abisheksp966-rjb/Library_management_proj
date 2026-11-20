package com.library.dao;

import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Books.
 * Supports SQLite JDBC with in-memory caching and fallback for stand-alone execution.
 */
public class BookDAO {

    // Fallback store in case JDBC driver is absent
    private static final Map<Integer, Book> memoryStore = new ConcurrentHashMap<>();

    public BookDAO() {}

    public synchronized void seedMemoryStore(List<Book> books) {
        for (Book b : books) {
            memoryStore.put(b.getBookId(), b);
        }
    }

    public List<Book> getAllBooks() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<Book> list = new ArrayList<>();
            String sql = "SELECT b.book_id, b.isbn, b.title, b.category_id, b.author_id, " +
                         "b.total_copies, b.available_copies, c.category_name, a.author_name " +
                         "FROM books b " +
                         "LEFT JOIN categories c ON b.category_id = c.category_id " +
                         "LEFT JOIN authors a ON b.author_id = a.author_id " +
                         "ORDER BY b.book_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Book b = new Book(
                            rs.getInt("book_id"),
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getInt("category_id"),
                            rs.getInt("author_id"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")
                    );
                    b.setCategoryName(rs.getString("category_name"));
                    b.setAuthorName(rs.getString("author_name"));
                    list.add(b);
                }
                return list;
            } catch (SQLException e) {
                // Fall back to memoryStore
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    public List<Book> getAvailableBooks() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<Book> list = new ArrayList<>();
            String sql = "SELECT b.book_id, b.isbn, b.title, b.category_id, b.author_id, " +
                         "b.total_copies, b.available_copies, c.category_name, a.author_name " +
                         "FROM books b " +
                         "JOIN categories c ON b.category_id = c.category_id " +
                         "JOIN authors a ON b.author_id = a.author_id " +
                         "WHERE b.available_copies > 0 " +
                         "ORDER BY b.book_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Book b = new Book(
                            rs.getInt("book_id"),
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getInt("category_id"),
                            rs.getInt("author_id"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")
                    );
                    b.setCategoryName(rs.getString("category_name"));
                    b.setAuthorName(rs.getString("author_name"));
                    list.add(b);
                }
                return list;
            } catch (SQLException e) {
                // Fall back
            }
        }
        List<Book> avail = new ArrayList<>();
        for (Book b : memoryStore.values()) {
            if (b.getAvailableCopies() > 0) {
                avail.add(b);
            }
        }
        return avail;
    }

    public Book getBookById(int bookId) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT b.book_id, b.isbn, b.title, b.category_id, b.author_id, " +
                         "b.total_copies, b.available_copies, c.category_name, a.author_name " +
                         "FROM books b " +
                         "LEFT JOIN categories c ON b.category_id = c.category_id " +
                         "LEFT JOIN authors a ON b.author_id = a.author_id " +
                         "WHERE b.book_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Book b = new Book(
                                rs.getInt("book_id"),
                                rs.getString("isbn"),
                                rs.getString("title"),
                                rs.getInt("category_id"),
                                rs.getInt("author_id"),
                                rs.getInt("total_copies"),
                                rs.getInt("available_copies")
                        );
                        b.setCategoryName(rs.getString("category_name"));
                        b.setAuthorName(rs.getString("author_name"));
                        return b;
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        return memoryStore.get(bookId);
    }

    public boolean updateAvailableCopies(int bookId, int delta) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "UPDATE books SET available_copies = available_copies + ? " +
                         "WHERE book_id = ? AND (available_copies + ?) >= 0 AND (available_copies + ?) <= total_copies;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, delta);
                ps.setInt(2, bookId);
                ps.setInt(3, delta);
                ps.setInt(4, delta);
                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                // Fall back
            }
        }
        Book b = memoryStore.get(bookId);
        if (b != null) {
            int newCount = b.getAvailableCopies() + delta;
            if (newCount >= 0 && newCount <= b.getTotalCopies()) {
                b.setAvailableCopies(newCount);
                return true;
            }
        }
        return false;
    }

    public boolean addBook(Book book) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "INSERT INTO books (isbn, title, category_id, author_id, total_copies, available_copies) " +
                         "VALUES (?, ?, ?, ?, ?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, book.getIsbn());
                ps.setString(2, book.getTitle());
                ps.setInt(3, book.getCategoryId());
                ps.setInt(4, book.getAuthorId());
                ps.setInt(5, book.getTotalCopies());
                ps.setInt(6, book.getAvailableCopies());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            book.setBookId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        int nextId = memoryStore.isEmpty() ? 1 : memoryStore.keySet().stream().max(Integer::compare).orElse(0) + 1;
        book.setBookId(nextId);
        memoryStore.put(nextId, book);
        return true;
    }
}
