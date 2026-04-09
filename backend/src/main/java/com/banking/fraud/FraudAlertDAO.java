package com.banking.fraud;

import com.banking.model.FraudAlert;
import com.banking.model.FraudAlert.Severity;
import com.banking.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FraudAlertDAO {

    public FraudAlert save(FraudAlert alert) {
        String sql = "INSERT INTO fraud_alerts (account_id, transaction_id, alert_type, description, severity) " +
                     "VALUES (?,?,?,?,?) RETURNING id, created_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, alert.getAccountId());
            ps.setInt(2, alert.getTransactionId());
            ps.setString(3, alert.getAlertType());
            ps.setString(4, alert.getDescription());
            ps.setString(5, alert.getSeverity().name());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                alert.setId(rs.getInt("id"));
                alert.setCreatedAt(rs.getTimestamp("created_at"));
            }

        } catch (SQLException e) {
            System.err.println("[FraudAlertDAO] Save error: " + e.getMessage());
        }
        return alert;
    }

    public List<FraudAlert> findByAccountId(int accountId) {
        List<FraudAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM fraud_alerts WHERE account_id = ? ORDER BY created_at DESC LIMIT 20";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[FraudAlertDAO] Fetch error: " + e.getMessage());
        }
        return list;
    }

    public List<FraudAlert> findAllUnresolved() {
        List<FraudAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM fraud_alerts WHERE is_resolved = FALSE ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[FraudAlertDAO] Error: " + e.getMessage());
        }
        return list;
    }

    public void resolve(int alertId) {
        String sql = "UPDATE fraud_alerts SET is_resolved = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alertId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[FraudAlertDAO] Resolve error: " + e.getMessage());
        }
    }

    private FraudAlert mapRow(ResultSet rs) throws SQLException {
        FraudAlert alert = new FraudAlert();
        alert.setId(rs.getInt("id"));
        alert.setAccountId(rs.getInt("account_id"));
        alert.setTransactionId(rs.getInt("transaction_id"));
        alert.setAlertType(rs.getString("alert_type"));
        alert.setDescription(rs.getString("description"));
        alert.setSeverity(Severity.valueOf(rs.getString("severity")));
        alert.setResolved(rs.getBoolean("is_resolved"));
        alert.setCreatedAt(rs.getTimestamp("created_at"));
        return alert;
    }
}
