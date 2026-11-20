package com.library.dao;

import com.library.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Student records and eligibility checks.
 */
public class StudentDAO {

    private static final Map<Integer, Student> memoryStore = new ConcurrentHashMap<>();

    public StudentDAO() {}

    public synchronized void seedMemoryStore(List<Student> students) {
        for (Student s : students) {
            memoryStore.put(s.getStudentId(), s);
        }
    }

    public List<Student> getAllStudents() {
        if (DatabaseConnection.isDriverAvailable()) {
            List<Student> list = new ArrayList<>();
            String sql = "SELECT student_id, register_no, name, department, year, email FROM students ORDER BY student_id ASC;";
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Student(
                            rs.getInt("student_id"),
                            rs.getString("register_no"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getInt("year"),
                            rs.getString("email")
                    ));
                }
                return list;
            } catch (SQLException e) {
                // Fall back
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    public Student getStudentById(int studentId) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT student_id, register_no, name, department, year, email FROM students WHERE student_id = ?;";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Student(
                                rs.getInt("student_id"),
                                rs.getString("register_no"),
                                rs.getString("name"),
                                rs.getString("department"),
                                rs.getInt("year"),
                                rs.getString("email")
                        );
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        return memoryStore.get(studentId);
    }

    public Student getStudentByRegNo(String regNo) {
        if (regNo == null) return null;
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "SELECT student_id, register_no, name, department, year, email FROM students WHERE UPPER(register_no) = UPPER(?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, regNo.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Student(
                                rs.getInt("student_id"),
                                rs.getString("register_no"),
                                rs.getString("name"),
                                rs.getString("department"),
                                rs.getInt("year"),
                                rs.getString("email")
                        );
                    }
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        for (Student s : memoryStore.values()) {
            if (s.getRegisterNo().equalsIgnoreCase(regNo.trim())) {
                return s;
            }
        }
        return null;
    }

    public int getActiveBorrowCount(int studentId) {
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
        // Fall back handled in TransactionDAO
        return 0;
    }

    public boolean addStudent(Student s) {
        if (DatabaseConnection.isDriverAvailable()) {
            String sql = "INSERT INTO students (register_no, name, department, year, email) VALUES (?, ?, ?, ?, ?);";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getRegisterNo());
                ps.setString(2, s.getName());
                ps.setString(3, s.getDepartment());
                ps.setInt(4, s.getYear());
                ps.setString(5, s.getEmail());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            s.setStudentId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                // Fall back
            }
        }
        int nextId = memoryStore.isEmpty() ? 1 : memoryStore.keySet().stream().max(Integer::compare).orElse(0) + 1;
        s.setStudentId(nextId);
        memoryStore.put(nextId, s);
        return true;
    }
}
