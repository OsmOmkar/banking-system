package com.banking.service;

import com.banking.model.Transaction;
import com.banking.model.UpiProfile;
import com.banking.util.DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// =============================================
// SYLLABUS: Unit II - OOP Encapsulation
//           Unit IV - Collections (List)
//           Unit IV - JDBC (PreparedStatement)
// UPI Data Access Object — reads/writes upi_profiles table
// =============================================
public class UpiDAO {

    // ── Check if a UPI ID is already taken ──────────────────────────
    public boolean isUpiIdTaken(String upiId) {
        String sql = "SELECT 1 FROM upi_profiles WHERE upi_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, upiId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[UpiDAO] isUpiIdTaken error: " + e.getMessage());
            return false;
        }
    }

    // ── Find by user ID ──────────────────────────────────────────────
    public UpiProfile findByUserId(int userId) {
        String sql = "SELECT u.*, a.account_number, a.account_type, us.full_name " +
                     "FROM upi_profiles u " +
                     "JOIN accounts a ON u.account_id = a.id " +
                     "JOIN users us ON u.user_id = us.id " +
                     "WHERE u.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UpiDAO] findByUserId error: " + e.getMessage());
        }
        return null;
    }

    // ── Find by full UPI ID (e.g. "omkar@javabank.com") ─────────────
    public UpiProfile findByUpiId(String upiId) {
        String sql = "SELECT u.*, a.account_number, a.account_type, us.full_name " +
                     "FROM upi_profiles u " +
                     "JOIN accounts a ON u.account_id = a.id " +
                     "JOIN users us ON u.user_id = us.id " +
                     "WHERE u.upi_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, upiId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UpiDAO] findByUpiId error: " + e.getMessage());
        }
        return null;
    }

    // ── Save new UPI profile ─────────────────────────────────────────
    public UpiProfile save(UpiProfile profile) {
        String sql = "INSERT INTO upi_profiles (user_id, account_id, upi_id, pin_hash) " +
                     "VALUES (?,?,?,?) RETURNING id, created_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profile.getUserId());
            ps.setInt(2, profile.getAccountId());
            ps.setString(3, profile.getUpiId());
            ps.setString(4, profile.getPinHash());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                profile.setId(rs.getInt("id"));
                profile.setCreatedAt(rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) {
            System.err.println("[UpiDAO] save error: " + e.getMessage());
            return null;
        }
        return profile;
    }

    // ── Update PIN hash ──────────────────────────────────────────────
    public void updatePin(int userId, String newPinHash) {
        String sql = "UPDATE upi_profiles SET pin_hash = ?, updated_at = NOW() WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPinHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UpiDAO] updatePin error: " + e.getMessage());
        }
    }

    // ── Get UPI transactions for an account ─────────────────────────
    // UPI transactions are stored with "[UPI]" prefix in description
    public List<Transaction> getUpiTransactions(int accountId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions " +
                     "WHERE account_id = ? AND description LIKE '[UPI]%' " +
                     "ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction tx = new Transaction();
                tx.setId(rs.getInt("id"));
                tx.setAccountId(rs.getInt("account_id"));
                tx.setTransactionType(
                    Transaction.TransactionType.valueOf(rs.getString("transaction_type")));
                tx.setAmount(rs.getDouble("amount"));
                tx.setBalanceAfter(rs.getDouble("balance_after"));
                tx.setDescription(rs.getString("description"));
                tx.setRecipientAccount(rs.getString("recipient_account"));
                tx.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(tx);
            }
        } catch (SQLException e) {
            System.err.println("[UpiDAO] getUpiTransactions error: " + e.getMessage());
        }
        return list;
    }

    // ── Verify PIN (compare hash) ────────────────────────────────────
    public static boolean verifyPin(String rawPin, String storedHash) {
        return hashPin(rawPin).equals(storedHash);
    }

    // ── SHA-256 PIN hashing ──────────────────────────────────────────
    public static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private UpiProfile mapRow(ResultSet rs) throws SQLException {
        UpiProfile p = new UpiProfile();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setAccountId(rs.getInt("account_id"));
        p.setUpiId(rs.getString("upi_id"));
        p.setPinHash(rs.getString("pin_hash"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        // JOIN fields (may not always be present)
        try { p.setAccountNumber(rs.getString("account_number")); } catch (Exception ignored) {}
        try { p.setAccountType(rs.getString("account_type")); }     catch (Exception ignored) {}
        try { p.setUserName(rs.getString("full_name")); }           catch (Exception ignored) {}
        return p;
    }
}
