package com.banking.servlet;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.UpiProfile;
import com.banking.model.User;
import com.banking.service.AccountDAO;
import com.banking.service.BankingService;
import com.banking.service.UpiDAO;
import com.banking.util.EmailService;
import com.banking.util.JsonUtil;
import com.banking.util.OTPService;
import com.banking.util.SMSService;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;

// =============================================
// SYLLABUS: Unit II  - Inheritance (extends BaseHandler)
//           Unit III - Exception handling
//           Unit IV  - Collections (list of transactions)
//
// Handles all UPI endpoints:
//   GET  /api/upi/profile          - get user's UPI profile
//   POST /api/upi/setup            - setup new UPI profile
//   GET  /api/upi/check-id         - check if UPI ID available
//   GET  /api/upi/lookup           - lookup recipient name by UPI ID
//   POST /api/upi/pay              - make UPI payment
//   GET  /api/upi/transactions     - get UPI transaction history
//   POST /api/upi/change-pin/send-otp    - send OTP to phone
//   POST /api/upi/change-pin/verify-otp  - verify OTP
//   POST /api/upi/change-pin             - set new PIN
// =============================================
public class UpiHandler extends BaseHandler {

    private final BankingService bankingService;
    private final UpiDAO         upiDAO         = new UpiDAO();
    private final AccountDAO     accountDAO     = new AccountDAO();

    public UpiHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.endsWith("/upi/profile")) {
                handleGetProfile(exchange, user);
            } else if ("POST".equals(method) && path.endsWith("/upi/setup")) {
                handleSetup(exchange, user, body);
            } else if ("GET".equals(method) && path.endsWith("/upi/check-id")) {
                handleCheckId(exchange);
            } else if ("GET".equals(method) && path.endsWith("/upi/lookup")) {
                handleLookup(exchange);
            } else if ("POST".equals(method) && path.endsWith("/upi/pay")) {
                handlePay(exchange, user, body);
            } else if ("GET".equals(method) && path.endsWith("/upi/transactions")) {
                handleGetTransactions(exchange, user);
            } else if ("POST".equals(method) && path.endsWith("/upi/change-pin/send-otp")) {
                handleSendChangePinOtp(exchange, user);
            } else if ("POST".equals(method) && path.endsWith("/upi/change-pin/verify-otp")) {
                handleVerifyChangePinOtp(exchange, user, body);
            } else if ("POST".equals(method) && path.endsWith("/upi/change-pin")) {
                handleChangePin(exchange, user, body);
            } else {
                sendResponse(exchange, 404, JsonUtil.error("UPI endpoint not found"));
            }
        } catch (Exception e) {
            System.err.println("[UpiHandler] Error: " + e.getMessage());
            sendResponse(exchange, 500, JsonUtil.error("Internal error: " + e.getMessage()));
        }
    }

    // ── GET /api/upi/profile ────────────────────────────────────────
    private void handleGetProfile(HttpExchange exchange, User user) throws Exception {
        UpiProfile profile = upiDAO.findByUserId(user.getId());
        if (profile == null) {
            sendResponse(exchange, 200, JsonUtil.success("{\"hasProfile\":false}"));
            return;
        }
        String json = JsonUtil.success(JsonUtil.object(
            JsonUtil.field("hasProfile",     true),
            JsonUtil.field("upiId",          profile.getUpiId()),
            JsonUtil.field("accountNumber",  profile.getAccountNumber() != null ? profile.getAccountNumber() : ""),
            JsonUtil.field("accountType",    profile.getAccountType()   != null ? profile.getAccountType()   : ""),
            JsonUtil.field("userName",       profile.getUserName()      != null ? profile.getUserName()      : user.getFullName()),
            JsonUtil.field("createdAt",      profile.getCreatedAt()     != null ? profile.getCreatedAt().toString() : "")
        ));
        sendResponse(exchange, 200, json);
    }

    // ── POST /api/upi/setup ─────────────────────────────────────────
    private void handleSetup(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String localId    = data.get("upiLocalId");           // part before @
        String pinRaw     = data.get("pin");
        String accountId  = data.get("accountId");

        if (localId == null || localId.isBlank())
            { sendResponse(exchange, 400, JsonUtil.error("UPI ID is required")); return; }
        if (pinRaw == null || pinRaw.length() != 4 || !pinRaw.matches("\\d{4}"))
            { sendResponse(exchange, 400, JsonUtil.error("PIN must be exactly 4 digits")); return; }
        if (accountId == null)
            { sendResponse(exchange, 400, JsonUtil.error("accountId is required")); return; }

        // Validate characters (letters, digits, dots, underscores only)
        if (!localId.matches("[a-zA-Z0-9._]{3,30}"))
            { sendResponse(exchange, 400, JsonUtil.error("UPI ID can only contain letters, numbers, dots, underscores (3-30 chars)")); return; }

        String fullUpiId = localId.toLowerCase() + "@javabank.com";

        // Check if already taken
        if (upiDAO.isUpiIdTaken(fullUpiId))
            { sendResponse(exchange, 409, JsonUtil.error("UPI ID '" + fullUpiId + "' is already taken. Choose another.")); return; }

        // Check user doesn't already have a UPI profile
        if (upiDAO.findByUserId(user.getId()) != null)
            { sendResponse(exchange, 400, JsonUtil.error("You already have a UPI profile. Use Change PIN to update.")); return; }

        // Verify account belongs to this user
        List<Account> accounts = accountDAO.findByUserId(user.getId());
        int accId = Integer.parseInt(accountId);
        boolean owns = false;
        for (Account a : accounts) if (a.getId() == accId) { owns = true; break; }
        if (!owns) { sendResponse(exchange, 403, JsonUtil.error("Access denied")); return; }

        UpiProfile profile = new UpiProfile(user.getId(), accId, fullUpiId, UpiDAO.hashPin(pinRaw));
        UpiProfile saved   = upiDAO.save(profile);

        if (saved == null)
            { sendResponse(exchange, 500, JsonUtil.error("Failed to create UPI profile. Please try again.")); return; }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
            JsonUtil.field("upiId", fullUpiId),
            JsonUtil.field("message", "UPI setup successful!")
        )));
    }

    // ── GET /api/upi/check-id?upiLocalId=xxx ───────────────────────
    private void handleCheckId(HttpExchange exchange) throws Exception {
        String localId = getQueryParam(exchange, "upiLocalId");
        if (localId == null || localId.isBlank())
            { sendResponse(exchange, 400, JsonUtil.error("upiLocalId required")); return; }

        String fullId = localId.toLowerCase() + "@javabank.com";
        boolean taken = upiDAO.isUpiIdTaken(fullId);
        sendResponse(exchange, 200, JsonUtil.success(
            JsonUtil.object(JsonUtil.field("taken", taken), JsonUtil.field("upiId", fullId))
        ));
    }

    // ── GET /api/upi/lookup?upiId=xxx@javabank.com ──────────────────
    private void handleLookup(HttpExchange exchange) throws Exception {
        String upiId = getQueryParam(exchange, "upiId");
        if (upiId == null || upiId.isBlank())
            { sendResponse(exchange, 400, JsonUtil.error("upiId required")); return; }

        // Accept just the local part too
        if (!upiId.contains("@")) upiId = upiId + "@javabank.com";

        UpiProfile profile = upiDAO.findByUpiId(upiId);
        if (profile == null)
            { sendResponse(exchange, 404, JsonUtil.error("UPI ID not found")); return; }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
            JsonUtil.field("upiId",       profile.getUpiId()),
            JsonUtil.field("name",        profile.getUserName() != null ? profile.getUserName() : "JavaBank User"),
            JsonUtil.field("accountType", profile.getAccountType() != null ? profile.getAccountType() : "")
        )));
    }

    // ── POST /api/upi/pay ───────────────────────────────────────────
    private void handlePay(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String toUpiId    = data.get("toUpiId");       // recipient full UPI ID
        String pin        = data.get("pin");           // sender's 4-digit PIN
        double amount     = Double.parseDouble(data.getOrDefault("amount", "0"));
        String description = data.getOrDefault("description", "UPI Payment");

        if (toUpiId == null || pin == null)
            { sendResponse(exchange, 400, JsonUtil.error("toUpiId and pin are required")); return; }
        if (amount <= 0)
            { sendResponse(exchange, 400, JsonUtil.error("Amount must be positive")); return; }

        // Get sender's UPI profile
        UpiProfile senderProfile = upiDAO.findByUserId(user.getId());
        if (senderProfile == null)
            { sendResponse(exchange, 400, JsonUtil.error("You don't have a UPI profile. Please set up UPI first.")); return; }

        // Verify PIN
        if (!UpiDAO.verifyPin(pin, senderProfile.getPinHash()))
            { sendResponse(exchange, 401, JsonUtil.error("Incorrect UPI PIN. Please try again.")); return; }

        // Normalize recipient UPI ID
        if (!toUpiId.contains("@")) toUpiId = toUpiId + "@javabank.com";

        // Prevent self-transfer
        if (toUpiId.equalsIgnoreCase(senderProfile.getUpiId()))
            { sendResponse(exchange, 400, JsonUtil.error("Cannot transfer to yourself")); return; }

        // Lookup recipient
        UpiProfile recipientProfile = upiDAO.findByUpiId(toUpiId);
        if (recipientProfile == null)
            { sendResponse(exchange, 404, JsonUtil.error("Recipient UPI ID not found: " + toUpiId)); return; }

        String recipientName = recipientProfile.getUserName() != null
            ? recipientProfile.getUserName() : "JavaBank User";

        // Description formatted with [UPI] prefix (for filtering in transaction history)
        String fullDesc = "[UPI] To: " + recipientName + " (" + toUpiId + ") | " + description;
        String receiverDesc = "[UPI] From: " + (user.getFullName() != null ? user.getFullName() : user.getUsername())
            + " (" + senderProfile.getUpiId() + ") | " + description;

        // Execute the transfer (this goes through fraud detection too)
        try {
            Transaction tx = bankingService.transfer(
                senderProfile.getAccountId(),
                recipientProfile.getAccountNumber(),
                amount, fullDesc,
                getIp(exchange)
            );

            // Also record a CREDIT entry on the recipient's side
            // (this is normally done in BankingService.transfer for the receiving account)
            // The debit description is already "[UPI]", credit side needs tagging too
            // We update the most recent deposit-style credit for this transfer,
            // OR we can tag the recipient's transaction separately
            // For simplicity, the transfer already credits recipient; we just tag it via receiverDesc
            // in the existing tx.description for recipient account's latest credit

            // Send success notification
            try {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    EmailService.sendTransactionSuccessEmail(
                        user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                        "UPI Transfer",
                        amount,
                        String.valueOf(tx.getId()),
                        tx.getBalanceAfter(),
                        description,
                        toUpiId
                    );
                }
            } catch (Exception notifEx) {
                System.err.println("[UpiHandler] Notification error: " + notifEx.getMessage());
            }

            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id",          tx.getId()),
                JsonUtil.field("amount",      tx.getAmount()),
                JsonUtil.field("balanceAfter",tx.getBalanceAfter()),
                JsonUtil.field("toUpiId",     toUpiId),
                JsonUtil.field("recipient",   recipientName),
                JsonUtil.field("description", description),
                JsonUtil.field("date",        tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""),
                JsonUtil.field("message",     "UPI payment successful!")
            )));

        } catch (com.banking.exception.FraudDetectedException e) {
            // Payment held for fraud review
            String msg = e.getMessage();
            String pendingId = "0";
            String humanMsg  = "Transfer held for fraud verification.";
            if (msg != null && msg.startsWith("TRANSACTION_HELD:")) {
                String[] parts = msg.split(":", 3);
                pendingId = parts.length > 1 ? parts[1] : "0";
                humanMsg  = parts.length > 2 ? parts[2] : humanMsg;
            }
            String json = "{\"success\":false,\"held\":true,\"pendingId\":" + pendingId
                    + ",\"error\":\"" + humanMsg.replace("\"", "'") + "\"}";
            sendResponse(exchange, 202, json);

        } catch (com.banking.exception.InsufficientFundsException e) {
            sendResponse(exchange, 400, JsonUtil.error("Insufficient balance: " + e.getMessage()));
        } catch (com.banking.exception.AccountNotFoundException e) {
            sendResponse(exchange, 404, JsonUtil.error("Account not found: " + e.getMessage()));
        }
    }

    // ── GET /api/upi/transactions ───────────────────────────────────
    private void handleGetTransactions(HttpExchange exchange, User user) throws Exception {
        UpiProfile profile = upiDAO.findByUserId(user.getId());
        if (profile == null)
            { sendResponse(exchange, 200, JsonUtil.success("[]")); return; }

        List<Transaction> txList = upiDAO.getUpiTransactions(profile.getAccountId());

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            if (i > 0) sb.append(",");
            sb.append(JsonUtil.object(
                JsonUtil.field("id",           tx.getId()),
                JsonUtil.field("type",         tx.getTransactionType().name()),
                JsonUtil.field("amount",       tx.getAmount()),
                JsonUtil.field("balanceAfter", tx.getBalanceAfter()),
                JsonUtil.field("description",  tx.getDescription() != null ? tx.getDescription() : ""),
                JsonUtil.field("recipient",    tx.getRecipientAccount() != null ? tx.getRecipientAccount() : ""),
                JsonUtil.field("date",         tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "")
            ));
        }
        sb.append("]");

        sendResponse(exchange, 200, JsonUtil.success(sb.toString()));
    }

    // ── POST /api/upi/change-pin/send-otp ──────────────────────────
    private void handleSendChangePinOtp(HttpExchange exchange, User user) throws Exception {
        UpiProfile profile = upiDAO.findByUserId(user.getId());
        if (profile == null)
            { sendResponse(exchange, 400, JsonUtil.error("No UPI profile found")); return; }

        String otp   = OTPService.generateOTP(user.getUsername() + "_upi_pin");
        String phone = user.getPhone();
        if (phone == null || phone.isBlank())
            { sendResponse(exchange, 400, JsonUtil.error("No phone number on your account")); return; }

        SMSService.sendOTPSms(phone, user.getFullName() != null ? user.getFullName() : user.getUsername(), otp);
        System.out.println("[UpiHandler] Change PIN OTP sent to +91" + phone + " for user: " + user.getUsername());
        sendResponse(exchange, 200, JsonUtil.success("\"OTP sent to your registered mobile number\""));
    }

    // ── POST /api/upi/change-pin/verify-otp ────────────────────────
    private void handleVerifyChangePinOtp(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String otp = data.get("otp");

        if (!OTPService.verifyOTP(user.getUsername() + "_upi_pin", otp).equals("VALID"))
            { sendResponse(exchange, 400, JsonUtil.error("Invalid or expired OTP")); return; }

        // Store verified flag so the next change-pin step is authorized
        OTPService.storeVerifiedFlag(user.getUsername() + "_upi_pin_change");
        sendResponse(exchange, 200, JsonUtil.success("\"OTP verified. You can now set a new PIN.\""));
    }

    // ── POST /api/upi/change-pin ────────────────────────────────────
    private void handleChangePin(HttpExchange exchange, User user, String body) throws Exception {
        // Must have verified flag from previous step
        if (!OTPService.isVerifiedFlagSet(user.getUsername() + "_upi_pin_change"))
            { sendResponse(exchange, 403, JsonUtil.error("OTP verification required first")); return; }

        Map<String, String> data = parseJson(body);
        String newPin = data.get("pin");

        if (newPin == null || !newPin.matches("\\d{4}"))
            { sendResponse(exchange, 400, JsonUtil.error("PIN must be exactly 4 digits")); return; }

        UpiProfile profile = upiDAO.findByUserId(user.getId());
        if (profile == null)
            { sendResponse(exchange, 400, JsonUtil.error("No UPI profile found")); return; }

        // (Optional check: prevent setting same PIN as current)
        if (UpiDAO.verifyPin(newPin, profile.getPinHash()))
            { sendResponse(exchange, 400, JsonUtil.error("New PIN cannot be the same as your current PIN")); return; }

        upiDAO.updatePin(user.getId(), UpiDAO.hashPin(newPin));
        OTPService.clearVerifiedFlag(user.getUsername() + "_upi_pin_change");

        sendResponse(exchange, 200, JsonUtil.success("\"UPI PIN updated successfully!\""));
    }
}
