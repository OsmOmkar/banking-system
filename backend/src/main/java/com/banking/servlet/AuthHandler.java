package com.banking.servlet;

import com.banking.exception.AuthenticationException;
import com.banking.model.User;
import com.banking.util.EmailService;
import com.banking.util.JsonUtil;
import com.banking.util.OTPService;
import com.banking.util.SMSService;
import com.banking.util.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

// SYLLABUS: Unit II - Inheritance (extends BaseHandler)
//           Unit III - Exception handling
//           Unit III - Packages (uses ValidationUtil, OTPService)
public class AuthHandler extends BaseHandler {

    @Override
    protected boolean requiresAuth() { return false; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equals(method) && path.endsWith("/login")) {
            handleLogin(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/register")) {
            handleRegister(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/logout")) {
            handleLogout(exchange);
        } else if ("POST".equals(method) && path.endsWith("/send-otp")) {
            handleSendOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-otp")) {
            handleVerifyOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/send-email-otp")) {
            handleSendEmailOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-email-otp")) {
            handleVerifyEmailOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/send-phone-otp")) {
            handleSendPhoneOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-phone-otp")) {
            handleVerifyPhoneOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/login-phone-otp")) {
            handleSendPhoneLoginOTP(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/verify-phone-login")) {
            handleVerifyPhoneLogin(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/check-availability")) {
            handleCheckAvailability(exchange, body);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    // ---------------------------------------------------------------
    // Login (username + password)
    // ---------------------------------------------------------------
    private void handleLogin(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String username = data.get("username");
        String password = data.get("password");

        if (username == null || password == null) {
            sendResponse(exchange, 400, JsonUtil.error("Username and password required"));
            return;
        }

        User authed = userDAO.authenticate(username, password);
        if (authed == null) {
            throw new AuthenticationException("Invalid username or password");
        }

        String token = userDAO.createSession(authed.getId());

        String json = JsonUtil.success(JsonUtil.object(
                JsonUtil.field("token", token),
                JsonUtil.field("userId", authed.getId()),
                JsonUtil.field("username", authed.getUsername()),
                JsonUtil.field("fullName", authed.getFullName()),
                JsonUtil.field("email", authed.getEmail())
        ));
        sendResponse(exchange, 200, json);
    }

    // ---------------------------------------------------------------
    // Register — 4-step: details → email OTP → phone OTP → done
    // ---------------------------------------------------------------
    private void handleRegister(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);

        String username  = data.get("username");
        String email     = data.get("email");
        String fullName  = data.get("fullName");
        String phone     = data.get("phone");
        String password  = data.get("password");
        String otpToken  = data.get("otpToken"); // "verified" => email was OTP-verified

        // ---- Step 1: Basic null check ----
        if (username == null || email == null || password == null || fullName == null) {
            sendResponse(exchange, 400, JsonUtil.error("All fields are required"));
            return;
        }

        // ---- Step 2: Validate full name ----
        String nameError = ValidationUtil.validateFullName(fullName);
        if (nameError != null) {
            sendResponse(exchange, 400, JsonUtil.error(nameError));
            return;
        }

        // ---- Step 3: Validate username ----
        String usernameError = ValidationUtil.validateUsername(username);
        if (usernameError != null) {
            sendResponse(exchange, 400, JsonUtil.error(usernameError));
            return;
        }

        // ---- Step 4: Validate email format ----
        String emailError = ValidationUtil.validateEmail(email);
        if (emailError != null) {
            sendResponse(exchange, 400, JsonUtil.error(emailError));
            return;
        }

        // ---- Step 5: Phone is mandatory now (required for OTP login) ----
        if (phone == null || phone.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Phone number is required"));
            return;
        }
        String phoneError = ValidationUtil.validatePhone(phone);
        if (phoneError != null) {
            sendResponse(exchange, 400, JsonUtil.error(phoneError));
            return;
        }

        // ---- Step 6: Validate password strength ----
        String passwordError = ValidationUtil.validatePassword(password);
        if (passwordError != null) {
            sendResponse(exchange, 400, JsonUtil.error(passwordError));
            return;
        }

        // ---- Step 7: Check email OTP verified ----
        if (otpToken == null || !OTPService.isEmailVerified(email)) {
            sendResponse(exchange, 400, JsonUtil.error(
                    "Email not verified. Please verify your email with OTP before registering."));
            return;
        }

        // ---- Step 7.5: Check phone OTP verified ----
        if (!OTPService.isPhoneVerified(phone)) {
            sendResponse(exchange, 400, JsonUtil.error(
                    "Phone not verified. Please verify your phone number with OTP."));
            return;
        }

        // ---- Step 8: Check for duplicates (username / email / phone) ----
        if (userDAO.existsByUsername(username)) {
            sendResponse(exchange, 409, JsonUtil.error("Username '" + username + "' is already taken."));
            return;
        }
        if (userDAO.existsByEmail(email)) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this email already exists."));
            return;
        }
        if (userDAO.existsByPhone(phone)) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this phone number already exists."));
            return;
        }

        // ---- Step 9: Register ----
        User newUser = userDAO.register(username, email, fullName, phone, password);
        if (newUser == null) {
            sendResponse(exchange, 409, JsonUtil.error("Registration failed. Please try again."));
            return;
        }

        // Mark email + phone as verified in DB
        userDAO.markEmailVerified(email);
        userDAO.markPhoneVerified(phone);

        // Clear OTP verified markers
        OTPService.clearVerified(email);
        OTPService.clearPhoneVerified(phone);

        String token = userDAO.createSession(newUser.getId());
        String json = JsonUtil.success(JsonUtil.object(
                JsonUtil.field("token", token),
                JsonUtil.field("userId", newUser.getId()),
                JsonUtil.field("username", newUser.getUsername()),
                JsonUtil.field("fullName", newUser.getFullName())
        ));
        sendResponse(exchange, 201, json);
    }

    // ---------------------------------------------------------------
    // POST /api/auth/check-availability
    // Checks if username / email / phone is already registered
    // Body: { "type": "username"|"email"|"phone", "value": "..." }
    // ---------------------------------------------------------------
    private void handleCheckAvailability(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String type  = data.get("type");
        String value = data.get("value");

        if (type == null || value == null || value.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("type and value are required"));
            return;
        }

        boolean taken;
        switch (type) {
            case "username" -> taken = userDAO.existsByUsername(value.trim());
            case "email"    -> taken = userDAO.existsByEmail(value.trim());
            case "phone"    -> taken = userDAO.existsByPhone(value.trim());
            default -> {
                sendResponse(exchange, 400, JsonUtil.error("Invalid type. Use: username, email, or phone"));
                return;
            }
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("available", !taken),
                JsonUtil.field("type", type),
                JsonUtil.field("value", value.trim())
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/auth/login-phone-otp
    // Sends OTP to the phone for mobile-number-based login
    // ---------------------------------------------------------------
    private void handleSendPhoneLoginOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String phone = data.get("phone");

        if (phone == null || phone.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Phone number is required"));
            return;
        }
        String phoneError = ValidationUtil.validatePhone(phone);
        if (phoneError != null) {
            sendResponse(exchange, 400, JsonUtil.error(phoneError));
            return;
        }

        // Check the phone exists in DB (can't OTP-login if not registered)
        User user = userDAO.findByPhone(phone.trim());
        if (user == null) {
            sendResponse(exchange, 404, JsonUtil.error("No account found with this phone number."));
            return;
        }

        String otp = OTPService.generateOTP("login:phone:" + phone.trim());
        SMSService.sendOTPSms(phone.trim(), user.getFullName(), otp);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "OTP sent to your phone"),
                JsonUtil.field("phone", phone.trim())
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/auth/verify-phone-login
    // Verifies OTP and logs in
    // ---------------------------------------------------------------
    private void handleVerifyPhoneLogin(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String phone = data.get("phone");
        String otp   = data.get("otp");

        if (phone == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("Phone and OTP are required"));
            return;
        }

        String result = OTPService.verifyOTP("login:phone:" + phone.trim(), otp);
        if ("VALID".equals(result)) {
            User user = userDAO.findByPhone(phone.trim());
            if (user == null) {
                sendResponse(exchange, 404, JsonUtil.error("Account not found"));
                return;
            }
            String token = userDAO.createSession(user.getId());
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("token", token),
                    JsonUtil.field("userId", user.getId()),
                    JsonUtil.field("username", user.getUsername()),
                    JsonUtil.field("fullName", user.getFullName()),
                    JsonUtil.field("email", user.getEmail())
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
    // POST /api/auth/send-otp
    // ---------------------------------------------------------------
    private void handleSendOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String email    = data.get("email");
        String fullName = data.get("fullName");

        if (email == null || email.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Email is required"));
            return;
        }

        String emailError = ValidationUtil.validateEmail(email);
        if (emailError != null) {
            sendResponse(exchange, 400, JsonUtil.error(emailError));
            return;
        }

        // Duplicate check before sending OTP
        if (userDAO.existsByEmail(email.trim())) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this email already exists."));
            return;
        }

        String otp = OTPService.generateOTP(email);
        EmailService.sendOTPEmail(email, fullName != null ? fullName : "User", otp);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "OTP sent to " + email + ". Valid for 10 minutes."),
                JsonUtil.field("email", email)
        )));
    }

    // ---------------------------------------------------------------
    // POST /api/auth/verify-otp
    // ---------------------------------------------------------------
    private void handleVerifyOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String email = data.get("email");
        String otp   = data.get("otp");

        if (email == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("Email and OTP are required"));
            return;
        }

        String result = OTPService.verifyOTP(email, otp);

        switch (result) {
            case "VALID":
                OTPService.markEmailVerified(email);
                sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                        JsonUtil.field("message", "Email verified successfully!"),
                        JsonUtil.field("verified", true)
                )));
                break;
            case "EXPIRED":
                sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
                break;
            case "MAX_ATTEMPTS":
                sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
                break;
            default:
                sendResponse(exchange, 400, JsonUtil.error("Incorrect OTP. Please try again."));
                break;
        }
    }

    // ---------------------------------------------------------------
    // Logout
    // ---------------------------------------------------------------
    private void handleLogout(HttpExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            userDAO.deleteSession(auth.substring(7));
        }
        sendResponse(exchange, 200, JsonUtil.success("\"Logged out\""));
    }

    // ========================================================================
    // OTP Verification Methods (email/phone specific endpoints)
    // ========================================================================

    private void handleSendEmailOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String email    = data.get("email");
        String userName = data.get("userName");

        if (email == null || email.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Valid email is required"));
            return;
        }
        String emailError = ValidationUtil.validateEmail(email);
        if (emailError != null) {
            sendResponse(exchange, 400, JsonUtil.error(emailError));
            return;
        }

        String otp = OTPService.generateOTP(email);
        EmailService.sendOTPEmail(email, userName != null ? userName : "User", otp);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "OTP sent to your email"),
                JsonUtil.field("validFor", "10 minutes"),
                JsonUtil.field("email", email)
        )));
    }

    private void handleVerifyEmailOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String email = data.get("email");
        String otp   = data.get("otp");

        if (email == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("Email and OTP are required"));
            return;
        }

        String result = OTPService.verifyOTP(email, otp);
        if ("VALID".equals(result)) {
            OTPService.markEmailVerified(email);
            userDAO.markEmailVerified(email);
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Email verified successfully"),
                    JsonUtil.field("verified", true)
            )));
        } else if ("EXPIRED".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
        } else if ("MAX_ATTEMPTS".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
        } else {
            sendResponse(exchange, 400, JsonUtil.error("Invalid or expired OTP"));
        }
    }

    private void handleSendPhoneOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String phone    = data.get("phone");
        String userName = data.get("userName");

        if (phone == null || phone.trim().isEmpty()) {
            sendResponse(exchange, 400, JsonUtil.error("Valid phone number is required"));
            return;
        }
        String phoneError = ValidationUtil.validatePhone(phone);
        if (phoneError != null) {
            sendResponse(exchange, 400, JsonUtil.error(phoneError));
            return;
        }

        // Duplicate check — phone must not already be registered
        if (userDAO.existsByPhone(phone.trim())) {
            sendResponse(exchange, 409, JsonUtil.error("An account with this phone number already exists."));
            return;
        }

        String otp = OTPService.generateOTP("phone:" + phone);
        SMSService.sendOTPSms(phone, userName != null ? userName : "User", otp);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "OTP sent to your phone"),
                JsonUtil.field("validFor", "10 minutes"),
                JsonUtil.field("phone", phone)
        )));
    }

    private void handleVerifyPhoneOTP(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String phone = data.get("phone");
        String otp   = data.get("otp");

        if (phone == null || otp == null) {
            sendResponse(exchange, 400, JsonUtil.error("Phone and OTP are required"));
            return;
        }

        String result = OTPService.verifyOTP("phone:" + phone, otp);
        if ("VALID".equals(result)) {
            OTPService.markPhoneVerified(phone);
            userDAO.markPhoneVerified(phone);
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Phone verified successfully"),
                    JsonUtil.field("verified", true)
            )));
        } else if ("EXPIRED".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("OTP has expired. Please request a new one."));
        } else if ("MAX_ATTEMPTS".equals(result)) {
            sendResponse(exchange, 400, JsonUtil.error("Too many wrong attempts. Please request a new OTP."));
        } else {
            sendResponse(exchange, 400, JsonUtil.error("Invalid or expired OTP"));
        }
    }
}
