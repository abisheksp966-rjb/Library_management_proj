package com.library.dao;

import com.library.model.BorrowTransaction;
import com.library.util.DateUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data Access Object for Borrow Transactions.
 */
public class TransactionDAO {

    private static final Map<Integer, BorrowTransaction> memoryStore = new ConcurrentHashMap<>();

    public TransactionDAO() {}

    public synchronized void seedMemoryStore(List<BorrowTransaction> list) {
        for (BorrowTransaction t : list) {
            memoryStore.put(t.getTransactionId(), t);
        }
    }

    public int createTransaction(BorrowTransaction t) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "INSERT INTO transactions (student_id, book_id, issue_date, due_date, return_date, fine_amount, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, t.getStudentId());
                ps.setInt(2, t.getBookId());
                ps.setString(3, DateUtil.format(t.getIssueDate()));
                ps.setString(4, DateUtil.format(t.getDueDate()));
                ps.setString(5, t.getReturnDate() != null ? DateUtil.format(t.getReturnDate()) : null);
                ps.setDouble(6, t.getFineAmount());
                ps.setString(7, t.getStatus());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        t.setTransactionId(id);
                        return id;
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        int nextId = memoryStore.isEmpty() ? 1 : memoryStore.keySet().stream().max(Integer::compare).orElse(0) + 1;
        t.setTransactionId(nextId);
        memoryStore.put(nextId, t);
        return nextId;
    }

    public BorrowTransaction getTransactionById(int transactionId) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT t.*, s.name AS student_name, s.register_no, b.title AS book_title " +
                         "FROM transactions t " +
                         "JOIN students s ON t.student_id = s.student_id " +
                         "JOIN books b ON t.book_id = b.book_id " +
                         "WHERE t.transaction_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        return memoryStore.get(transactionId);
    }

    public BorrowTransaction getActiveTransaction(int studentId, int bookId) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT t.*, s.name AS student_name, s.register_no, b.title AS book_title " +
                         "FROM transactions t " +
                         "JOIN students s ON t.student_id = s.student_id " +
                         "JOIN books b ON t.book_id = b.book_id " +
                         "WHERE t.student_id = ? AND t.book_id = ? AND t.status = 'ISSUED' " +
                         "ORDER BY t.transaction_id DESC LIMIT 1;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        for (BorrowTransaction t : memoryStore.values()) {
            if (t.getStudentId() == studentId && t.getBookId() == bookId && "ISSUED".equalsIgnoreCase(t.getStatus())) {
                return t;
            }
        }
        return null;
    }

    public boolean updateReturn(int transactionId, LocalDate returnDate, double fineAmount) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "UPDATE transactions SET status = 'RETURNED', return_date = ?, fine_amount = ? WHERE transaction_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, DateUtil.format(returnDate));
                ps.setDouble(2, fineAmount);
                ps.setInt(3, transactionId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                // Fall back
            }
        }
        BorrowTransaction t = memoryStore.get(transactionId);
        if (t != null) {
            t.setStatus("RETURNED");
            t.setReturnDate(returnDate);
            t.setFineAmount(fineAmount);
            return true;
        }
        return false;
    }

    public int getActiveCountForStudent(int studentId) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT COUNT(*) FROM transactions WHERE student_id = ? AND status = 'ISSUED';";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        return (int) memoryStore.values().stream()
                .filter(t -> t.getStudentId() == studentId && "ISSUED".equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public List<BorrowTransaction> getStudentHistory(int studentId) {
        if (DatabaseConnection.isDriverAvailable()) {
            List<BorrowTransaction> list = new ArrayList<>();
            String sql = "SELECT t.*, s.name AS student_name, s.register_no, b.title AS book_title " +
                         "FROM transactions t " +
                         "JOIN students s ON t.student_id = s.student_id " +
                         "JOIN books b ON t.book_id = b.book_id " +
                         "WHERE t.student_id = ? " +
                         "ORDER BY t.issue_date DESC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                // Fall back
            }
        }
        return memoryStore.values().stream()
                .filter(t -> t.getStudentId() == studentId)
                .sorted((a, b) -> b.getIssueDate().compareTo(a.getIssueDate()))
                .collect(Collectors.toList());
    }

    public List<BorrowTransaction> getOverdueTransactions(LocalDate referenceDate) {
        if (DatabaseConnection.isDriverAvailable()) {
            List<BorrowTransaction> list = new ArrayList<>();
            String sql = "SELECT t.*, s.name AS student_name, s.register_no, b.title AS book_title " +
                         "FROM transactions t " +
                         "JOIN students s ON t.student_id = s.student_id " +
                         "JOIN books b ON t.book_id = b.book_id " +
                         "WHERE t.status = 'ISSUED' AND date(t.due_date) < date(?) " +
                         "ORDER BY t.due_date ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, DateUtil.format(referenceDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                // Fall back
            }
        }
        return memoryStore.values().stream()
                .filter(t -> "ISSUED".equalsIgnoreCase(t.getStatus()) && t.getDueDate().isBefore(referenceDate))
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .collect(Collectors.toList());
    }

    public List<BorrowTransaction> getAllTransactions() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<BorrowTransaction> list = new ArrayList<>();
            String sql = "SELECT t.*, s.name AS student_name, s.register_no, b.title AS book_title " +
                         "FROM transactions t " +
                         "JOIN students s ON t.student_id = s.student_id " +
                         "JOIN books b ON t.book_id = b.book_id " +
                         "ORDER BY t.transaction_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            } catch (SQLException e) {
                // Fall back
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    private BorrowTransaction mapRow(ResultSet rs) throws SQLException {
        BorrowTransaction t = new BorrowTransaction(
                rs.getInt("transaction_id"),
                rs.getInt("student_id"),
                rs.getInt("book_id"),
                DateUtil.parse(rs.getString("issue_date")),
                DateUtil.parse(rs.getString("due_date")),
                DateUtil.parse(rs.getString("return_date")),
                rs.getDouble("fine_amount"),
                rs.getString("status")
        );
        t.setStudentName(rs.getString("student_name"));
        t.setRegisterNo(rs.getString("register_no"));
        t.setBookTitle(rs.getString("book_title"));
        return t;
    }
}
