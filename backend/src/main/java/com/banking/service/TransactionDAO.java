package com.banking.service;

import com.banking.model.Transaction;
import com.banking.model.Transaction.TransactionType;
import com.banking.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// SYLLABUS: Unit V - JDBC, Unit IV - Collections
public class TransactionDAO {

    public Transaction save(Transaction tx) {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, balance_after, " +
                     "description, recipient_account, ip_address, is_flagged) " +
                     "VALUES (?,?,?,?,?,?,?,?) RETURNING id, created_at";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tx.getAccountId());
            ps.setString(2, tx.getTransactionType().name());
            ps.setDouble(3, tx.getAmount());
            ps.setDouble(4, tx.getBalanceAfter());
            ps.setString(5, tx.getDescription());
            ps.setString(6, tx.getRecipientAccount());
            ps.setString(7, tx.getIpAddress());
            ps.setBoolean(8, tx.isFlagged());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tx.setId(rs.getInt("id"));
                tx.setCreatedAt(rs.getTimestamp("created_at"));
            }

        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Save error: " + e.getMessage());
        }
        return tx;
    }

    public void markFlagged(int transactionId) {
        String sql = "UPDATE transactions SET is_flagged = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Flag error: " + e.getMessage());
        }
    }

    // SYLLABUS: Unit IV - ArrayList (Collections)
    public List<Transaction> findRecentByAccount(int accountId, int limit) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[TransactionDAO] Fetch error: " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> findByAccount(int accountId) {
        return findRecentByAccount(accountId, 50);
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("id"));
        tx.setAccountId(rs.getInt("account_id"));
        tx.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        tx.setAmount(rs.getDouble("amount"));
        tx.setBalanceAfter(rs.getDouble("balance_after"));
        tx.setDescription(rs.getString("description"));
        tx.setRecipientAccount(rs.getString("recipient_account"));
        tx.setIpAddress(rs.getString("ip_address"));
        tx.setCreatedAt(rs.getTimestamp("created_at"));
        tx.setFlagged(rs.getBoolean("is_flagged"));
        return tx;
    }
}
