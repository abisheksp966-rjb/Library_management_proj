package com.library.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connectivity to SQLite.
 */
public class DatabaseConnection {
    private static final String DB_DIR = "database";
    private static final String DB_FILE = "database/library.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection = null;
    private static boolean isSqliteDriverAvailable = true;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            isSqliteDriverAvailable = false;
        }
    }

    public static boolean isDriverAvailable() {
        return isSqliteDriverAvailable;
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (!isSqliteDriverAvailable) {
            throw new SQLException("SQLite JDBC driver (org.sqlite.JDBC) is not present in classpath.");
        }

        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }
}
