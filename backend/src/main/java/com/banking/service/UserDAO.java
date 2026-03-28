package com.banking.service;

import com.banking.model.User;
import com.banking.util.DatabaseConnection;
import com.banking.util.PasswordUtil;

import java.sql.*;

// SYLLABUS: Unit V - JDBC, Unit III - Exception handling
public class UserDAO {

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error: " + e.getMessage());
        }
        return null;
    }

    public User authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user != null && PasswordUtil.verify(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public String createSession(int userId) {
        String token = PasswordUtil.generateToken();
        String sql = "INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, token);
            // Session valid for 24 hours
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis() + 86400000L));
            ps.executeUpdate();
            return token;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Session error: " + e.getMessage());
            return null;
        }
    }

    public User getUserFromToken(String token) {
        String sql = "SELECT u.* FROM users u JOIN sessions s ON u.id = s.user_id " +
                     "WHERE s.token = ? AND s.expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] Token error: " + e.getMessage());
        }
        return null;
    }

    public void deleteSession(String token) {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] Logout error: " + e.getMessage());
        }
    }

    public User register(String username, String email, String fullName, String phone, String password) {
        String sql = "INSERT INTO users (username, email, full_name, phone, password_hash) " +
                     "VALUES (?,?,?,?,?) RETURNING *";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, fullName);
            ps.setString(4, phone);
            ps.setString(5, PasswordUtil.hash(password));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] Register error: " + e.getMessage());
        }
        return null;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
