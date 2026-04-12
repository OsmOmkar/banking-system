package com.banking.service;

import com.banking.model.PendingTransaction;
import com.banking.model.PendingTransaction.Status;
import com.banking.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// =============================================
// SYLLABUS: Unit V - JDBC (full CRUD operations)
//           Unit IV - Collections (ArrayList)
//           Unit III - Exception handling
// Manages the pending_transactions table in Supabase.
// =============================================
public class PendingTransactionDAO {

    // ---------------------------------------------------------------
    // Save a new pending transaction (called when fraud is detected)
    // ---------------------------------------------------------------
    public PendingTransaction save(PendingTransaction pt) {
        String sql = "INSERT INTO pending_transactions " +
                "(account_id, account_number, transaction_type, amount, to_account_number, " +
                "description, ip_address, status, expires_at, fraud_alert_type, " +
                "fraud_description, severity, user_id, user_email, user_phone, user_name, " +
                "original_balance, recipient_original_balance) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING *";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pt.getAccountId());
            ps.setString(2, pt.getAccountNumber());
            ps.setString(3, pt.getTransactionType());
            ps.setDouble(4, pt.getAmount());
            ps.setString(5, pt.getToAccountNumber());
            ps.setString(6, pt.getDescription());
            ps.setString(7, pt.getIpAddress());
            ps.setString(8, Status.PENDING_CONFIRMATION.name());
            // Expires in 5 minutes
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis() + 5 * 60 * 1000L));
            ps.setString(10, pt.getFraudAlertType());
            ps.setString(11, pt.getFraudDescription());
            ps.setString(12, pt.getSeverity());
            ps.setInt(13, pt.getUserId());
            ps.setString(14, pt.getUserEmail());
            ps.setString(15, pt.getUserPhone());
            ps.setString(16, pt.getUserName());
            ps.setDouble(17, pt.getOriginalBalance());
            ps.setDouble(18, pt.getRecipientOriginalBalance());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PendingTransaction saved = mapRow(rs);
                if (saved != null) {
                    com.banking.util.DatabaseSyncService.syncCustom(
                        "INSERT INTO pending_transactions (id, account_id, account_number, transaction_type, amount, to_account_number, description, ip_address, status, expires_at, fraud_alert_type, fraud_description, severity, user_id, user_email, user_phone, user_name, original_balance, recipient_original_balance) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING",
                        saved.getId(), saved.getAccountId(), saved.getAccountNumber(), saved.getTransactionType(), saved.getAmount(), saved.getToAccountNumber(), saved.getDescription(), saved.getIpAddress(), saved.getStatus().name(), saved.getExpiresAt(), saved.getFraudAlertType(), saved.getFraudDescription(), saved.getSeverity(), saved.getUserId(), saved.getUserEmail(), saved.getUserPhone(), saved.getUserName(), saved.getOriginalBalance(), saved.getRecipientOriginalBalance()
                    );
                }
                return saved;
            }

        } catch (SQLException e) {
            System.err.println("[PendingTransactionDAO] Save error: " + e.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Find by ID
    // ---------------------------------------------------------------
    public PendingTransaction findById(int id) {
        String sql = "SELECT * FROM pending_transactions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[PendingTransactionDAO] FindById error: " + e.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Find all pending (status = PENDING_CONFIRMATION) for a user
    // ---------------------------------------------------------------
    public List<PendingTransaction> findPendingByUser(int userId) {
        List<PendingTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM pending_transactions WHERE user_id = ? " +
                     "AND status = 'PENDING_CONFIRMATION' ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[PendingTransactionDAO] FindPending error: " + e.getMessage());
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Find ALL expired but still PENDING (for timeout monitor)
    // ---------------------------------------------------------------
    public List<PendingTransaction> findExpiredPending() {
        List<PendingTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM pending_transactions " +
                     "WHERE status = 'PENDING_CONFIRMATION' AND expires_at < NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[PendingTransactionDAO] FindExpired error: " + e.getMessage());
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Update status (CONFIRMED / REJECTED / TIMEOUT)
    // ---------------------------------------------------------------
    public boolean updateStatus(int id, Status status) {
        String sql = "UPDATE pending_transactions SET status = ?, responded_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, id);
            boolean success = ps.executeUpdate() > 0;
            if (success) com.banking.util.DatabaseSyncService.syncCustom("UPDATE pending_transactions SET status = ?, responded_at = NOW() WHERE id = ?", status.name(), id);
            return success;

        } catch (SQLException e) {
            System.err.println("[PendingTransactionDAO] UpdateStatus error: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Map ResultSet row to PendingTransaction object
    // SYLLABUS: Unit V - JDBC ResultSet mapping
    // ---------------------------------------------------------------
    private PendingTransaction mapRow(ResultSet rs) throws SQLException {
        PendingTransaction pt = new PendingTransaction();
        pt.setId(rs.getInt("id"));
        pt.setAccountId(rs.getInt("account_id"));
        pt.setAccountNumber(rs.getString("account_number"));
        pt.setTransactionType(rs.getString("transaction_type"));
        pt.setAmount(rs.getDouble("amount"));
        pt.setToAccountNumber(rs.getString("to_account_number"));
        pt.setDescription(rs.getString("description"));
        pt.setIpAddress(rs.getString("ip_address"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try { pt.setStatus(Status.valueOf(statusStr)); }
            catch (Exception ignore) { pt.setStatus(Status.PENDING_CONFIRMATION); }
        }

        pt.setCreatedAt(rs.getTimestamp("created_at"));
        pt.setExpiresAt(rs.getTimestamp("expires_at"));
        pt.setRespondedAt(rs.getTimestamp("responded_at"));
        pt.setFraudAlertType(rs.getString("fraud_alert_type"));
        pt.setFraudDescription(rs.getString("fraud_description"));
        pt.setSeverity(rs.getString("severity"));
        pt.setUserId(rs.getInt("user_id"));
        pt.setUserEmail(rs.getString("user_email"));
        pt.setUserPhone(rs.getString("user_phone"));
        pt.setUserName(rs.getString("user_name"));
        pt.setOriginalBalance(rs.getDouble("original_balance"));
        pt.setRecipientOriginalBalance(rs.getDouble("recipient_original_balance"));
        return pt;
    }
}
