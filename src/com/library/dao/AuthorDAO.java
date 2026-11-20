package com.library.dao;

import com.library.model.Author;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Authors.
 */
public class AuthorDAO {
    private static final Map<Integer, Author> memoryStore = new ConcurrentHashMap<>();

    public AuthorDAO() {}

    public synchronized void seedMemoryStore(List<Author> list) {
        for (Author a : list) {
            memoryStore.put(a.getAuthorId(), a);
        }
    }

    public List<Author> getAllAuthors() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<Author> list = new ArrayList<>();
            String sql = "SELECT author_id, author_name, email FROM authors ORDER BY author_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Author(
                            rs.getInt("author_id"),
                            rs.getString("author_name"),
                            rs.getString("email")
                    ));
                }
                return list;
            } catch (SQLException ignored) {
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    public Author getAuthorById(int id) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT author_id, author_name, email FROM authors WHERE author_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Author(
                                rs.getInt("author_id"),
                                rs.getString("author_name"),
                                rs.getString("email")
                        );
                    }
                }
            } catch (SQLException ignored) {
            }
        }
        return memoryStore.get(id);
    }

    public int addAuthor(Author author) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "INSERT INTO authors (author_name, email) VALUES (?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, author.getAuthorName());
                ps.setString(2, author.getEmail());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        author.setAuthorId(id);
                        return id;
                    }
                }
            } catch (SQLException ignored) {
            }
        }
        int nextId = memoryStore.isEmpty() ? 1 : memoryStore.keySet().stream().max(Integer::compare).orElse(0) + 1;
        author.setAuthorId(nextId);
        memoryStore.put(nextId, author);
        return nextId;
    }
}
