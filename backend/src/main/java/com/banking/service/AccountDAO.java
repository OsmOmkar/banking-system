package com.banking.service;

import com.banking.exception.AccountNotFoundException;
import com.banking.model.Account;
import com.banking.model.CurrentAccount;
import com.banking.model.SavingsAccount;
import com.banking.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    public Account findById(int id) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
            else throw new AccountNotFoundException("Account not found with id: " + id);
        } catch (SQLException e) {
            throw new RuntimeException("DB error finding account: " + e.getMessage(), e);
        }
    }

    public Account findByAccountNumber(String accountNumber) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE account_number = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
            else throw new AccountNotFoundException("Account not found: " + accountNumber);
        } catch (SQLException e) {
            throw new RuntimeException("DB error: " + e.getMessage(), e);
        }
    }

    public Account findByUpiId(String upiId) throws AccountNotFoundException {
        String sql = "SELECT * FROM accounts WHERE upi_id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, upiId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
            else throw new AccountNotFoundException("No account found with UPI ID: " + upiId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error: " + e.getMessage(), e);
        }
    }

    public List<Account> findByUserId(int userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ? AND is_active = TRUE ORDER BY created_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) accounts.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[AccountDAO] Error: " + e.getMessage());
        }
        return accounts;
    }

    public boolean updateBalance(int accountId, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AccountDAO] Balance update error: " + e.getMessage());
            return false;
        }
    }

    public Account createAccount(int userId, String type, double initialDeposit, double interestRate, double overdraftLimit) {
        String accNumber = "ACC" + System.currentTimeMillis();
        String upiId = accNumber + "@javabank";
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, interest_rate, overdraft_limit, upi_id) VALUES (?,?,?,?,?,?,?) RETURNING *";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, accNumber);
            ps.setString(3, type.toUpperCase());
            ps.setDouble(4, initialDeposit);
            ps.setDouble(5, interestRate);
            ps.setDouble(6, overdraftLimit);
            ps.setString(7, upiId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[AccountDAO] Create error: " + e.getMessage());
        }
        return null;
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        String type = rs.getString("account_type");
        Account account;
        if ("SAVINGS".equals(type)) {
            SavingsAccount sa = new SavingsAccount();
            sa.setInterestRate(rs.getDouble("interest_rate"));
            account = sa;
        } else {
            CurrentAccount ca = new CurrentAccount();
            ca.setOverdraftLimit(rs.getDouble("overdraft_limit"));
            account = ca;
        }
        account.setId(rs.getInt("id"));
        account.setUserId(rs.getInt("user_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setAccountType(type);
        account.setBalance(rs.getDouble("balance"));
        account.setActive(rs.getBoolean("is_active"));
        try { account.setUpiId(rs.getString("upi_id")); } catch (Exception ignored) {}
        return account;
    }
}
