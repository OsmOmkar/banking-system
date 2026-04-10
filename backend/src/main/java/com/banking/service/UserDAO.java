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
        try { user.setEmailVerified(rs.getBoolean("email_verified")); } catch (SQLException ignored) {}
        try { user.setPhoneVerified(rs.getBoolean("phone_verified")); } catch (SQLException ignored) {}
        try { user.setKycVerified(rs.getBoolean("kyc_verified")); } catch (SQLException ignored) {}
        return user;
    }

    // ========================================================================
    // Verification Methods
    // ========================================================================

    public boolean markEmailVerified(String email) {
        String sql = "UPDATE users SET email_verified = TRUE WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[UserDAO] Email verified for: " + email);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error marking email verified: " + e.getMessage());
        }
        return false;
    }

    public boolean markPhoneVerified(String phone) {
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        String sql = "UPDATE users SET phone_verified = TRUE WHERE REPLACE(REPLACE(phone, ' ', ''), '-', '') = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cleanPhone);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("[UserDAO] Phone verified for: " + phone);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error marking phone verified: " + e.getMessage());
        }
        return false;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding by email: " + e.getMessage());
        }
        return null;
    }

    // ========================================================================
    // Duplicate / Availability checks (registration + profile edit)
    // ========================================================================

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[UserDAO] existsByUsername error: " + e.getMessage());
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[UserDAO] existsByEmail error: " + e.getMessage());
        }
        return false;
    }

    public boolean existsByPhone(String phone) {
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        String sql = "SELECT 1 FROM users WHERE REPLACE(REPLACE(phone, ' ', ''), '-', '') = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanPhone);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[UserDAO] existsByPhone error: " + e.getMessage());
        }
        return false;
    }

    // ========================================================================
    // Phone-based login
    // ========================================================================

    public User findByPhone(String phone) {
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        String sql = "SELECT * FROM users WHERE REPLACE(REPLACE(phone, ' ', ''), '-', '') = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanPhone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] findByPhone error: " + e.getMessage());
        }
        return null;
    }

    // ========================================================================
    // Profile update methods
    // ========================================================================

    public boolean updateUsername(int userId, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateUsername error: " + e.getMessage());
        }
        return false;
    }

    public boolean updateEmail(int userId, String newEmail) {
        // Also marks email_verified = TRUE since we just OTP-verified the new email
        String sql = "UPDATE users SET email = ?, email_verified = TRUE WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateEmail error: " + e.getMessage());
        }
        return false;
    }

    public boolean updatePhone(int userId, String newPhone) {
        // Also marks phone_verified = TRUE since we just OTP-verified the new phone
        String sql = "UPDATE users SET phone = ?, phone_verified = TRUE WHERE id = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPhone);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updatePhone error: " + e.getMessage());
        }
        return false;
    }

    // ========================================================================
    // Account deletion (soft delete)
    // ========================================================================

    public boolean deactivateUser(int userId) {
        String deleteSessions = "DELETE FROM sessions WHERE user_id = ?";
        String deactivate     = "UPDATE users SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(deleteSessions);
                 PreparedStatement ps2 = conn.prepareStatement(deactivate)) {
                ps1.setInt(1, userId);
                ps1.executeUpdate();
                ps2.setInt(1, userId);
                int rows = ps2.executeUpdate();
                conn.commit();
                System.out.println("[UserDAO] Deactivated user id=" + userId);
                return rows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] deactivateUser error: " + e.getMessage());
        }
        return false;
    }
}
