package com.banking.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// =============================================
// SYLLABUS: Unit V - JDBC (Java Database Connectivity)
// Manages connection to Supabase PostgreSQL
// =============================================
public class DatabaseConnection {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        // SYLLABUS: Unit IV - File I/O (reading config from properties file)
        try {
            // Try environment variables first (for Railway deployment)
            DB_URL      = System.getenv("DB_URL");
            DB_USER     = System.getenv("DB_USER");
            DB_PASSWORD = System.getenv("DB_PASSWORD");

            // Fallback to config.properties for local development
            if (DB_URL == null) {
                InputStream in = DatabaseConnection.class
                        .getClassLoader().getResourceAsStream("config.properties");
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    DB_URL      = props.getProperty("db.url");
                    DB_USER     = props.getProperty("db.user");
                    DB_PASSWORD = props.getProperty("db.password");
                }
            }

            // Load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            System.out.println("[DB] PostgreSQL driver loaded.");

        } catch (Exception e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
        }
    }

    // SYLLABUS: Unit V - Connecting to a database (JDBC)
    public static Connection getConnection() throws SQLException {
        if (DB_URL == null) {
            throw new SQLException("Database URL not configured. Set DB_URL environment variable.");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Test the connection
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
