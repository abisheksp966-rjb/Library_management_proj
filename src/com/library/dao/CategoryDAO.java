package com.library.dao;

import com.library.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Book Categories.
 */
public class CategoryDAO {
    private static final Map<Integer, Category> memoryStore = new ConcurrentHashMap<>();

    public CategoryDAO() {}

    public synchronized void seedMemoryStore(List<Category> list) {
        for (Category c : list) {
            memoryStore.put(c.getCategoryId(), c);
        }
    }

    public List<Category> getAllCategories() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<Category> list = new ArrayList<>();
            String sql = "SELECT category_id, category_name, description FROM categories ORDER BY category_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Category(
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("description")
                    ));
                }
                return list;
            } catch (SQLException ignored) {
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    public Category getCategoryById(int id) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT category_id, category_name, description FROM categories WHERE category_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Category(
                                rs.getInt("category_id"),
                                rs.getString("category_name"),
                                rs.getString("description")
                        );
                    }
                }
            } catch (SQLException ignored) {
            }
        }
        return memoryStore.get(id);
    }

    public int addCategory(Category category) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, category.getCategoryName());
                ps.setString(2, category.getDescription());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        category.setCategoryId(id);
                        return id;
                    }
                }
            } catch (SQLException ignored) {
            }
        }
        int nextId = memoryStore.isEmpty() ? 1 : memoryStore.keySet().stream().max(Integer::compare).orElse(0) + 1;
        category.setCategoryId(nextId);
        memoryStore.put(nextId, category);
        return nextId;
    }
}
