package com.banking.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// =============================================
// SYLLABUS: Unit IV - Multithreading (background worker thread)
//           Unit V - JDBC (database operations)
//           Unit IV - Collections (BlockingQueue)
// Syncs data to local PostgreSQL database in real-time
// =============================================
public class DatabaseSyncService implements Runnable {

    // Local PostgreSQL connection details
    // CONFIGURE THESE for your local database
    private static final String LOCAL_DB_URL      = "jdbc:postgresql://localhost:5432/banking_local";
    private static final String LOCAL_DB_USER     = "postgres";
    private static final String LOCAL_DB_PASSWORD = "your_password_here";

    // SYLLABUS: Unit IV - BlockingQueue for thread-safe sync queue
    private static final BlockingQueue<SyncOperation> syncQueue = new LinkedBlockingQueue<>();

    private static Thread  syncThread     = null;
    private static boolean running        = false;
    private static Connection localConnection = null;

    /**
     * SYLLABUS: Unit II - Inner class (encapsulation)
     * Represents a database operation to sync.
     */
    public static class SyncOperation {
        String   sql;
        Object[] params;

        public SyncOperation(String sql, Object[] params) {
            this.sql    = sql;
            this.params = params;
        }
    }

    /**
     * Starts the sync service.
     * SYLLABUS: Unit IV - Thread creation and management
     */
    public static synchronized void start() {
        if (running) {
            System.out.println("[DatabaseSync] Already running");
            return;
        }
        if (!testLocalConnection()) {
            System.err.println("[DatabaseSync] WARNING: Cannot connect to local database");
            System.err.println("[DatabaseSync] Please ensure PostgreSQL is running on localhost:5432");
            System.err.println("[DatabaseSync] and database 'banking_local' exists");
            System.err.println("[DatabaseSync] Sync service will NOT start");
            return;
        }
        running      = true;
        syncThread   = new Thread(new DatabaseSyncService(), "DatabaseSyncThread");
        syncThread.setDaemon(true);
        syncThread.start();
        System.out.println("[DatabaseSync] Started - syncing to local database");
    }

    private static boolean testLocalConnection() {
        try (Connection conn = DriverManager.getConnection(LOCAL_DB_URL, LOCAL_DB_USER, LOCAL_DB_PASSWORD)) {
            System.out.println("[DatabaseSync] Connected to local database successfully");
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseSync] Failed to connect to local database: " + e.getMessage());
            return false;
        }
    }

    private static Connection getLocalConnection() throws SQLException {
        if (localConnection == null || localConnection.isClosed()) {
            localConnection = DriverManager.getConnection(LOCAL_DB_URL, LOCAL_DB_USER, LOCAL_DB_PASSWORD);
        }
        return localConnection;
    }

    /**
     * SYLLABUS: Unit IV - Runnable implementation
     */
    @Override
    public void run() {
        System.out.println("[DatabaseSync] Worker thread started");
        while (running) {
            try {
                SyncOperation op = syncQueue.take();
                executeOnLocal(op);
            } catch (InterruptedException e) {
                System.out.println("[DatabaseSync] Thread interrupted");
                break;
            } catch (Exception e) {
                System.err.println("[DatabaseSync] Error processing sync: " + e.getMessage());
            }
        }
        System.out.println("[DatabaseSync] Worker thread stopped");
    }

    private void executeOnLocal(SyncOperation op) {
        try (Connection conn = getLocalConnection();
             PreparedStatement pstmt = conn.prepareStatement(op.sql)) {

            if (op.params != null) {
                for (int i = 0; i < op.params.length; i++) {
                    pstmt.setObject(i + 1, op.params[i]);
                }
            }
            if (op.sql.trim().toUpperCase().startsWith("SELECT")) {
                pstmt.executeQuery();
            } else {
                int rows = pstmt.executeUpdate();
                System.out.println("[DatabaseSync] Synced to local DB: " + rows + " row(s) affected");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseSync] Error executing on local DB: " + e.getMessage());
            System.err.println("[DatabaseSync] SQL: " + op.sql);
        }
    }

    // ========================================================================
    // Public API for queueing sync operations
    // ========================================================================

    /** Syncs a user INSERT operation. */
    public static void syncUserInsert(int id, String username, String email,
                                      String fullName, String phone, String passwordHash) {
        if (!running) return;
        String sql = """
            INSERT INTO users (id, username, email, full_name, phone, password_hash, is_active, email_verified, phone_verified)
            VALUES (?, ?, ?, ?, ?, ?, TRUE, FALSE, FALSE)
            ON CONFLICT (id) DO UPDATE
            SET username = EXCLUDED.username,
                email    = EXCLUDED.email,
                full_name = EXCLUDED.full_name,
                phone    = EXCLUDED.phone
            """;
        syncQueue.offer(new SyncOperation(sql, new Object[]{id, username, email, fullName, phone, passwordHash}));
    }

    /** Syncs an account INSERT operation. */
    public static void syncAccountInsert(int id, int userId, String accountNumber,
                                         String accountType, double balance) {
        if (!running) return;
        String sql = """
            INSERT INTO accounts (id, user_id, account_number, account_type, balance, is_active)
            VALUES (?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO UPDATE SET balance = EXCLUDED.balance
            """;
        syncQueue.offer(new SyncOperation(sql, new Object[]{id, userId, accountNumber, accountType, balance}));
    }

    /** Syncs an account balance UPDATE. */
    public static void syncAccountBalanceUpdate(int accountId, double newBalance) {
        if (!running) return;
        syncQueue.offer(new SyncOperation(
                "UPDATE accounts SET balance = ? WHERE id = ?",
                new Object[]{newBalance, accountId}));
    }

    /** Syncs a transaction INSERT. */
    public static void syncTransactionInsert(int id, int accountId, String txType,
                                             double amount, double balanceAfter,
                                             String description, String recipientAccount) {
        if (!running) return;
        String sql = """
            INSERT INTO transactions (id, account_id, transaction_type, amount, balance_after,
                                     description, recipient_account, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS')
            ON CONFLICT (id) DO NOTHING
            """;
        syncQueue.offer(new SyncOperation(sql,
                new Object[]{id, accountId, txType, amount, balanceAfter, description, recipientAccount}));
    }

    /** Syncs email verification update. */
    public static void syncEmailVerified(String email) {
        if (!running) return;
        syncQueue.offer(new SyncOperation(
                "UPDATE users SET email_verified = TRUE WHERE LOWER(email) = LOWER(?)",
                new Object[]{email}));
    }

    /** Syncs phone verification update. */
    public static void syncPhoneVerified(String phone) {
        if (!running) return;
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        syncQueue.offer(new SyncOperation(
                "UPDATE users SET phone_verified = TRUE WHERE REPLACE(REPLACE(phone, ' ', ''), '-', '') = ?",
                new Object[]{cleanPhone}));
    }

    /** Stops the sync service. */
    public static synchronized void stop() {
        if (!running) return;
        running = false;
        if (syncThread != null) {
            syncThread.interrupt();
            try { syncThread.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (localConnection != null) {
            try { localConnection.close(); } catch (SQLException ignored) {}
        }
        System.out.println("[DatabaseSync] Stopped");
    }

    /** Returns the number of pending sync operations. */
    public static int getPendingCount() {
        return syncQueue.size();
    }
}
