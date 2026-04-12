package com.banking.util;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe JDBC connection pool for the JavaBank backend.
 * Maintains POOL_SIZE physical connections to PostgreSQL and reuses them
 * across requests.  Every borrower calls close() via try-with-resources;
 * the proxy intercepts close() and returns the raw connection to the pool
 * instead of terminating the socket.
 */
public class DatabaseConnection {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    // 8 connections: plenty for Railway free tier (max_connections = 25).
    private static final int POOL_SIZE = 8;
    private static final ArrayBlockingQueue<Connection> pool =
            new ArrayBlockingQueue<>(POOL_SIZE);

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

            // Pre-populate the pool at startup
            if (DB_URL != null) {
                for (int i = 0; i < POOL_SIZE; i++) {
                    try {
                        pool.offer(DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD));
                    } catch (Exception e) {
                        System.err.println("[DB] Pool init slot " + i + ": " + e.getMessage());
                    }
                }
                System.out.println("[DB] Connection pool ready: "
                        + pool.size() + "/" + POOL_SIZE + " connections.");
            }

        } catch (Exception e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Borrow a connection from the pool (blocks up to 5 s).
     * The caller MUST close it (try-with-resources is fine) — close() returns
     * it to the pool rather than destroying the underlying socket.
     */
    public static Connection getConnection() throws SQLException {
        if (DB_URL == null) throw new SQLException("Database URL not configured.");
        try {
            Connection raw = pool.poll(5, TimeUnit.SECONDS);

            if (raw == null) {
                // Pool temporarily dry — create a short-lived connection
                System.err.println("[DB] Pool exhausted — opening temporary connection");
                return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }

            // Validate; silently replace stale connections
            if (!isValid(raw)) {
                closeQuietly(raw);
                raw = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }

            return wrapForPool(raw);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for DB connection");
        }
    }

    /** Used only at server startup to verify connectivity (bypasses pool). */
    public static boolean testConnection() {
        if (DB_URL == null) return false;
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a raw (unwrapped) connection to the pool after cleaning its state. */
    static void returnToPool(Connection raw) {
        if (raw == null) return;
        try {
            // Always come back with clean transactional state
            if (!raw.isClosed()) {
                if (!raw.getAutoCommit()) {
                    try { raw.rollback(); } catch (Exception ignored) {}
                    raw.setAutoCommit(true);
                }
            }
        } catch (Exception ignored) {}
        if (!pool.offer(raw)) {
            closeQuietly(raw); // Pool full — discard this one
        }
    }

    private static boolean isValid(Connection c) {
        try { return c != null && !c.isClosed() && c.isValid(2); }
        catch (Exception e) { return false; }
    }

    private static void closeQuietly(Connection c) {
        try { if (c != null) c.close(); } catch (Exception ignored) {}
    }

    /**
     * Creates a dynamic proxy so that calling close() on the returned
     * Connection returns it to the pool instead of closing the socket.
     * All other Connection methods delegate directly to the physical connection.
     */
    private static Connection wrapForPool(final Connection raw) {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            (proxy, method, args) -> {
                // Intercept close() → return to pool
                if ("close".equals(method.getName())) {
                    returnToPool(raw);
                    return null;
                }
                // isClosed() should reflect the physical connection
                if ("isClosed".equals(method.getName())) {
                    try { return raw.isClosed(); } catch (Exception e) { return true; }
                }
                // Delegate everything else
                try {
                    return method.invoke(raw, args);
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    throw ite.getCause() != null ? ite.getCause() : ite;
                }
            }
        );
    }
}
