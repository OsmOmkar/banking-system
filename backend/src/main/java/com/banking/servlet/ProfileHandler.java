package com.banking.servlet;

import com.banking.model.User;
import com.banking.util.EmailService;
import com.banking.util.JsonUtil;
import com.banking.util.OTPService;
import com.banking.util.PasswordUtil;
import com.banking.util.SMSService;
import com.banking.util.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

// =============================================
// SYLLABUS: Unit II - Inheritance (extends BaseHandler)
//           Unit V  - JDBC (UserDAO profile updates)
//           Unit III - Packages (uses OTPService, EmailService, SMSService)
// =============================================
public class ProfileHandler extends BaseHandler {

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.endsWith("/profile")) {
            handleGetProfile(exchange, user);
        } else if ("POST".equals(method) && path.endsWith("/update-username")) {
            handleUpdateUsername(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/update-email")) {
            handleInitiateEmailChange(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-new-email")) {
            handleVerifyNewEmail(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/update-phone")) {
            handleInitiatePhoneChange(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-new-phone")) {
            handleVerifyNewPhone(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-password")) {
            handleVerifyCurrentPassword(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/change-password")) {
            handleChangePassword(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/forgot-password-otp")) {
            handleForgotPasswordOTP(exchange, user);
        } else if ("POST".equals(method) && path.endsWith("/verify-reset-otp")) {
            handleVerifyResetOTP(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/reset-password-with-otp")) {
            handleResetPasswordWithOTP(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/delete-account")) {
            handleDeleteAccount(exchange, user, body);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Endpoint not found"));
        }
    }

    // ---------------------------------------------------------------
    // GET /api/profile — returns current user's profile
    // ---------------------------------------------------------------
    private void handleGetProfile(HttpExchange exchange, User user) throws Exception {
        User fresh = userDAO.findById(user.getId());
        if (fresh == null) {
            sendResponse(exchange, 404, JsonUtil.error("User not found"));
            return;
        }
        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id",            fresh.getId()),
                JsonUtil.field("username",      fresh.getUsername()),
                JsonUtil.field("email",         fresh.getEmail()),
                JsonUtil.field("fullName",      fresh.getFullName()),
                JsonUtil.field("phone",         fresh.getPhone() != null ? fresh.getPhone() : ""),
                JsonUtil.field("emailVerified", fresh.isEmailVerified()),
                JsonUtil.field("phoneVerified", fresh.isPhoneVerified()),
                JsonUtil.field("kycVerified",   fresh.isKycVerified()),
                JsonUtil.field("createdAt",     fresh.getCreatedAt() != null ? fresh.getCreatedAt().toString() : "")
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/profile/update-username
    // ---------------------------------------------------------------
    private void handleUpdateUsername(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newUsername = data.get("newUsername");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("New username is required")); return;
        }
        newUsername = newUsername.trim();
        String err = ValidationUtil.validateUsername(newUsername);
        if (err != null) { sendResponse(exchange, 400, JsonUtil.error(err)); return; }
        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            sendResponse(exchange, 400, JsonUtil.error("This is already your username")); return;
        }
        if (userDAO.existsByUsername(newUsername)) {
            sendResponse(exchange, 409, JsonUtil.error("Username '" + newUsername + "' is already taken")); return;
        }
        boolean updated = userDAO.updateUsername(user.getId(), newUsername);
        if (updated) {
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message",     "Username updated successfully"),
                    JsonUtil.field("newUsername", newUsername)
            )));
        } else {
            sendResponse(exchange, 500, JsonUtil.error("Failed to update username. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/update-email  (Step 1 — send OTP to new email)
    // ---------------------------------------------------------------
    private void handleInitiateEmailChange(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newEmail = data.get("newEmail");
        if (newEmail == null || newEmail.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("New email is required")); return;
        }
        newEmail = newEmail.trim().toLowerCase();
        String err = ValidationUtil.validateEmail(newEmail);
        if (err != null) { sendResponse(exchange, 400, JsonUtil.error(err)); return; }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            sendResponse(exchange, 400, JsonUtil.error("This is already your email address")); return;
        }
        if (userDAO.existsByEmail(newEmail)) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this email already exists")); return;
        }
        String otpKey = "profile:email:" + user.getId() + ":" + newEmail;
        String otp = OTPService.generateOTP(otpKey);
        EmailService.sendOTPEmail(newEmail, user.getFullName(), otp);
        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message",  "OTP sent to " + newEmail + ". Verify to complete the change."),
                JsonUtil.field("newEmail", newEmail)
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/profile/verify-new-email  (Step 2 — verify OTP, update email)
    // ---------------------------------------------------------------
    private void handleVerifyNewEmail(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newEmail = data.get("newEmail");
        String otp      = data.get("otp");
        if (newEmail == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("newEmail and otp are required")); return;
        }
        newEmail = newEmail.trim().toLowerCase();
        String otpKey = "profile:email:" + user.getId() + ":" + newEmail;
        String result = OTPService.verifyOTP(otpKey, otp);
        if ("VALID".equals(result)) {
            boolean updated = userDAO.updateEmail(user.getId(), newEmail);
            if (updated) {
                sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                        JsonUtil.field("message",  "Email updated successfully to " + newEmail),
                        JsonUtil.field("newEmail", newEmail)
                )));
            } else {
                sendResponse(exchange, 500, JsonUtil.error("Failed to update email. Please try again."));
            }
        } else if ("EXPIRED".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
        } else if ("MAX_ATTEMPTS".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
        } else {
            sendResponse(exchange, 400, JsonUtil.error("Incorrect OTP. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/update-phone  (Step 1 — send OTP to new phone)
    // ---------------------------------------------------------------
    private void handleInitiatePhoneChange(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newPhone = data.get("newPhone");
        if (newPhone == null || newPhone.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("New phone number is required")); return;
        }
        newPhone = newPhone.trim().replaceAll("[\\s-]", "");
        String err = ValidationUtil.validatePhone(newPhone);
        if (err != null) { sendResponse(exchange, 400, JsonUtil.error(err)); return; }
        String currentClean = user.getPhone() != null ? user.getPhone().replaceAll("[\\s-]", "") : "";
        if (newPhone.equals(currentClean)) {
            sendResponse(exchange, 400, JsonUtil.error("This is already your phone number")); return;
        }
        if (userDAO.existsByPhone(newPhone)) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this phone number already exists")); return;
        }
        String otpKey = "profile:phone:" + user.getId() + ":" + newPhone;
        String otp = OTPService.generateOTP(otpKey);
        SMSService.sendOTPSms(newPhone, user.getFullName(), otp);
        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message",  "OTP sent to " + newPhone + ". Verify to complete the change."),
                JsonUtil.field("newPhone", newPhone)
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/profile/verify-new-phone  (Step 2 — verify OTP, update phone)
    // ---------------------------------------------------------------
    private void handleVerifyNewPhone(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newPhone = data.get("newPhone");
        String otp      = data.get("otp");
        if (newPhone == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("newPhone and otp are required")); return;
        }
        newPhone = newPhone.trim().replaceAll("[\\s-]", "");
        String otpKey = "profile:phone:" + user.getId() + ":" + newPhone;
        String result = OTPService.verifyOTP(otpKey, otp);
        if ("VALID".equals(result)) {
            boolean updated = userDAO.updatePhone(user.getId(), newPhone);
            if (updated) {
                sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                        JsonUtil.field("message",  "Phone number updated successfully"),
                        JsonUtil.field("newPhone", newPhone)
                )));
            } else {
                sendResponse(exchange, 500, JsonUtil.error("Failed to update phone. Please try again."));
            }
        } else if ("EXPIRED".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
        } else if ("MAX_ATTEMPTS".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
        } else {
            sendResponse(exchange, 400, JsonUtil.error("Incorrect OTP. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/verify-password
    // Checks that the supplied currentPassword matches the stored hash.
    // Body: { "currentPassword": "..." }
    // ---------------------------------------------------------------
    private void handleVerifyCurrentPassword(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String currentPassword = data.get("currentPassword");
        if (currentPassword == null || currentPassword.isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Current password is required")); return;
        }
        // Fetch fresh user to get up-to-date password hash
        User fresh = userDAO.findById(user.getId());
        if (fresh == null || !PasswordUtil.verify(currentPassword, fresh.getPasswordHash())) {
            sendResponse(exchange, 401, JsonUtil.error("Incorrect current password. Please try again."));
            return;
        }
        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "Password verified")
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/profile/change-password
    // Verifies current password once more, then sets the new password.
    // Body: { "currentPassword": "...", "newPassword": "..." }
    // ---------------------------------------------------------------
    private void handleChangePassword(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String currentPassword = data.get("currentPassword");
        String newPassword     = data.get("newPassword");

        if (currentPassword == null || newPassword == null ||
            currentPassword.isEmpty() || newPassword.isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("currentPassword and newPassword are required"));
            return;
        }

        // Re-verify current password
        User fresh = userDAO.findById(user.getId());
        if (fresh == null || !PasswordUtil.verify(currentPassword, fresh.getPasswordHash())) {
            sendResponse(exchange, 401, JsonUtil.error("Incorrect current password."));
            return;
        }

        // New password must not be the same as the current one
        if (PasswordUtil.verify(newPassword, fresh.getPasswordHash())) {
            sendResponse(exchange, 400, JsonUtil.error("New password must be different from your current password."));
            return;
        }

        // Validate strength server-side as well
        String err = ValidationUtil.validatePassword(newPassword);
        if (err != null) { sendResponse(exchange, 400, JsonUtil.error(err)); return; }

        boolean updated = userDAO.updatePassword(user.getId(), newPassword);
        if (updated) {
            System.out.println("[ProfileHandler] Password changed for userId=" + user.getId());
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Password changed successfully.")
            )));
        } else {
            sendResponse(exchange, 500, JsonUtil.error("Failed to update password. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/forgot-password-otp
    // Sends a reset OTP to the user's registered (verified) email.
    // ---------------------------------------------------------------
    private void handleForgotPasswordOTP(HttpExchange exchange, User user) throws Exception {
        User fresh = userDAO.findById(user.getId());
        if (fresh == null) {
            sendResponse(exchange, 404, JsonUtil.error("User not found")); return;
        }
        String email = fresh.getEmail();
        if (email == null || email.isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("No email address on your account.")); return;
        }

        String otpKey = "profile:pwreset:" + user.getId();
        String otp = OTPService.generateOTP(otpKey);
        EmailService.sendOTPEmail(email, fresh.getFullName(), otp);

        // Mask the email for display: ab***@gmail.com
        String maskedEmail = maskEmail(email);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message",     "OTP sent to your registered email."),
                JsonUtil.field("maskedEmail", maskedEmail)
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/profile/verify-reset-otp
    // Verifies the password-reset OTP (does not change password yet).
    // Body: { "otp": "..." }
    // ---------------------------------------------------------------
    private void handleVerifyResetOTP(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String otp = data.get("otp");
        if (otp == null || otp.length() != 6) {
            sendResponse(exchange, 400, JsonUtil.error("Enter the 6-digit OTP")); return;
        }

        String otpKey = "profile:pwreset:" + user.getId();
        String result = OTPService.verifyOTP(otpKey, otp);

        if ("VALID".equals(result)) {
            // Store a temporary "otp verified" flag in OTPService so the next step can proceed
            // We reuse OTPService with a different key as a short-lived flag
            String verifiedKey = "profile:pwreset:verified:" + user.getId();
            OTPService.storeVerifiedFlag(verifiedKey);

            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "OTP verified. You may now set a new password.")
            )));
        } else if ("EXPIRED".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
        } else if ("MAX_ATTEMPTS".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
        } else {
            sendResponse(exchange, 400, JsonUtil.error("Incorrect OTP. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/reset-password-with-otp
    // Sets a new password after OTP has been verified.
    // Body: { "newPassword": "..." }
    // ---------------------------------------------------------------
    private void handleResetPasswordWithOTP(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String newPassword = data.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("New password is required")); return;
        }

        // Check that the user actually passed OTP verification
        String verifiedKey = "profile:pwreset:verified:" + user.getId();
        if (!OTPService.isVerifiedFlagSet(verifiedKey)) {
            sendResponse(exchange, 403, JsonUtil.error("OTP not verified. Please complete verification first."));
            return;
        }

        // Validate strength
        String err = ValidationUtil.validatePassword(newPassword);
        if (err != null) { sendResponse(exchange, 400, JsonUtil.error(err)); return; }

        boolean updated = userDAO.updatePassword(user.getId(), newPassword);
        if (updated) {
            OTPService.clearVerifiedFlag(verifiedKey);  // consume the flag
            System.out.println("[ProfileHandler] Password reset via OTP for userId=" + user.getId());
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Password reset successfully.")
            )));
        } else {
            sendResponse(exchange, 500, JsonUtil.error("Failed to reset password. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/profile/delete-account
    // ---------------------------------------------------------------
    private void handleDeleteAccount(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String confirmation = data.get("confirmation");
        if (!"DELETE".equals(confirmation)) {
            sendResponse(exchange, 400, JsonUtil.error(
                    "Type 'DELETE' as confirmation to permanently delete your account."));
            return;
        }
        boolean deleted = userDAO.deactivateUser(user.getId());
        if (deleted) {
            System.out.println("[ProfileHandler] Account permanently deleted: userId=" + user.getId()
                    + " username=" + user.getUsername());
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Account permanently deleted. We're sorry to see you go."),
                    JsonUtil.field("deleted", true)
            )));
        } else {
            sendResponse(exchange, 500, JsonUtil.error("Failed to delete account. Please try again."));
        }
    }

    // ---------------------------------------------------------------
    // Helper — mask email for display
    // ---------------------------------------------------------------
    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        String local  = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        String masked = local.substring(0, 2) + "***" + local.charAt(local.length() - 1);
        return masked + domain;
    }
}