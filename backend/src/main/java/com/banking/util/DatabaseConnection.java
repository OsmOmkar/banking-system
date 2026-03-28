package com.banking.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        try {
            DB_URL      = System.getenv("DB_URL");
            DB_USER     = System.getenv("DB_USER");
            DB_PASSWORD = System.getenv("DB_PASSWORD");

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

            Class.forName("org.postgresql.Driver");
            System.out.println("[DB] PostgreSQL driver loaded.");
        } catch (Exception e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (DB_URL == null) {
            throw new SQLException("Database URL not configured.");
        }
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "require");
        props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
        
        // Strip sslmode from URL if present to avoid conflicts
        String url = DB_URL.contains("?") ? DB_URL.split("\\?")[0] : DB_URL;
        return DriverManager.getConnection(url, props);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
