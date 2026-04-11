package com.banking.servlet;

import com.banking.model.PendingTransaction;
import com.banking.model.PendingTransaction.Status;
import com.banking.model.User;
import com.banking.service.BankingService;
import com.banking.service.PendingTransactionDAO;
import com.banking.util.EmailService;
import com.banking.util.JsonUtil;
import com.banking.util.SMSService;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// =============================================
// SYLLABUS: Unit II - Inheritance (extends BaseHandler)
//           Unit IV - Collections (List of pending transactions)
//           Unit III - Exception handling
//
// Handles API endpoints for the 5-minute fraud hold system:
//   GET  /api/pending           — list pending transactions for user
//   POST /api/pending/confirm   — user says "Yes, I made this"
//   POST /api/pending/reject    — user says "No! This is fraud!"
// =============================================
public class PendingTransactionHandler extends BaseHandler {

    private final BankingService bankingService;
    private final PendingTransactionDAO pendingDAO = new PendingTransactionDAO();

    public PendingTransactionHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.endsWith("/pending")) {
            handleListPending(exchange, user);
        } else if ("POST".equals(method) && path.endsWith("/confirm")) {
            handleConfirm(exchange, user, body);
        } else if ("POST".equals(method) && path.endsWith("/reject")) {
            handleReject(exchange, user, body);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    // ---------------------------------------------------------------
    // GET /api/pending — user sees their pending fraud-hold transactions
    // ---------------------------------------------------------------
    private void handleListPending(HttpExchange exchange, User user) throws Exception {
        // SYLLABUS: Unit IV - Collections: returns List
        List<PendingTransaction> list = pendingDAO.findPendingByUser(user.getId());

        List<String> items = new ArrayList<>();
        for (PendingTransaction pt : list) {
            items.add(ptToJson(pt));
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.array(items)));
    }

    // ---------------------------------------------------------------
    // POST /api/pending/confirm — user confirms they made the transaction
    // ---------------------------------------------------------------
    private void handleConfirm(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String idStr = data.get("pendingId");

        if (idStr == null) {
            sendResponse(exchange, 400, JsonUtil.error("pendingId is required"));
            return;
        }

        int pendingId = Integer.parseInt(idStr);
        PendingTransaction pt = pendingDAO.findById(pendingId);

        if (pt == null) {
            sendResponse(exchange, 404, JsonUtil.error("Pending transaction not found"));
            return;
        }

        // Security: only the account owner can confirm
        if (pt.getUserId() != user.getId()) {
            sendResponse(exchange, 403, JsonUtil.error("Access denied"));
            return;
        }

        if (pt.getStatus() != Status.PENDING_CONFIRMATION) {
            sendResponse(exchange, 400, JsonUtil.error(
                    "Transaction already " + pt.getStatus().name().toLowerCase()));
            return;
        }

        if (pt.isExpired()) {
            pendingDAO.updateStatus(pendingId, Status.TIMEOUT);
            sendResponse(exchange, 400, JsonUtil.error(
                    "Confirmation window expired. Transaction was auto-cancelled for safety."));
            return;
        }

        // User confirmed → complete the transaction now
        try {
            bankingService.completeHeldTransaction(pt);
            pendingDAO.updateStatus(pendingId, Status.CONFIRMED);

            System.out.println("[PendingHandler] ✅ Transaction ID " + pendingId
                    + " CONFIRMED by user " + user.getUsername());

            // Send CONFIRMED notification (email + SMS)
            try {
                String name  = user.getFullName() != null ? user.getFullName() : user.getUsername();
                String txType = pt.getTransactionType() != null ? pt.getTransactionType() : "Transaction";
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    EmailService.sendTransactionSuccessEmail(
                        user.getEmail(), name,
                        txType + " (Confirmed)",
                        pt.getAmount(),
                        String.valueOf(pendingId),
                        0, pt.getDescription(),
                        pt.getToAccountNumber());
                }
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    SMSService.sendTransactionSms(
                        user.getPhone(), name,
                        txType, pt.getAmount(), "CONFIRMED", null);
                }
            } catch (Exception notifEx) {
                System.err.println("[PendingHandler] Confirm notification error: " + notifEx.getMessage());
            }

            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                    JsonUtil.field("message", "Transaction confirmed and completed successfully"),
                    JsonUtil.field("pendingId", pendingId),
                    JsonUtil.field("amount", pt.getAmount()),
                    JsonUtil.field("type", pt.getTransactionType())
            )));

        } catch (Exception e) {
            sendResponse(exchange, 500, JsonUtil.error("Failed to complete transaction: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/pending/reject — user reports fraud, transaction cancelled
    // ---------------------------------------------------------------
    private void handleReject(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String idStr = data.get("pendingId");

        if (idStr == null) {
            sendResponse(exchange, 400, JsonUtil.error("pendingId is required"));
            return;
        }

        int pendingId = Integer.parseInt(idStr);
        PendingTransaction pt = pendingDAO.findById(pendingId);

        if (pt == null) {
            sendResponse(exchange, 404, JsonUtil.error("Pending transaction not found"));
            return;
        }

        // Security: only the account owner can reject
        if (pt.getUserId() != user.getId()) {
            sendResponse(exchange, 403, JsonUtil.error("Access denied"));
            return;
        }

        if (pt.getStatus() != Status.PENDING_CONFIRMATION) {
            sendResponse(exchange, 400, JsonUtil.error(
                    "Transaction already " + pt.getStatus().name().toLowerCase()));
            return;
        }

        // Mark as rejected → funds NOT deducted
        pendingDAO.updateStatus(pendingId, Status.REJECTED);

        System.out.println("[PendingHandler] 🚨 FRAUD REPORTED! Pending transaction ID "
                + pendingId + " REJECTED by user " + user.getUsername()
                + " — Amount ₹" + pt.getAmount() + " is SAFE.");

        // Send FRAUD REJECTED notification (email + SMS)
        try {
            String name   = user.getFullName() != null ? user.getFullName() : user.getUsername();
            String txType = pt.getTransactionType() != null ? pt.getTransactionType() : "Transaction";
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                EmailService.sendTransactionFailedEmail(
                    user.getEmail(), name,
                    txType + " (Fraud Rejected)",
                    pt.getAmount(),
                    "You reported this as fraud. Transaction cancelled, money is safe.");
            }
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                SMSService.sendTransactionSms(
                    user.getPhone(), name,
                    txType, pt.getAmount(), "FRAUD_REJECTED", null);
            }
        } catch (Exception notifEx) {
            System.err.println("[PendingHandler] Reject notification error: " + notifEx.getMessage());
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("message", "Transaction rejected. Your money is safe. Fraud report logged."),
                JsonUtil.field("pendingId", pendingId),
                JsonUtil.field("amount", pt.getAmount()),
                JsonUtil.field("type", pt.getTransactionType())
        )));
    }

    // ---------------------------------------------------------------
    // Helper: convert PendingTransaction to JSON string
    // ---------------------------------------------------------------
    private String ptToJson(PendingTransaction pt) {
        long millisLeft = pt.getExpiresAt() != null
                ? pt.getExpiresAt().getTime() - System.currentTimeMillis()
                : 0;
        long secondsLeft = Math.max(0, millisLeft / 1000);

        return JsonUtil.object(
                JsonUtil.field("id", pt.getId()),
                JsonUtil.field("accountId", pt.getAccountId()),
                JsonUtil.field("accountNumber", pt.getAccountNumber() != null ? pt.getAccountNumber() : ""),
                JsonUtil.field("type", pt.getTransactionType() != null ? pt.getTransactionType() : ""),
                JsonUtil.field("amount", pt.getAmount()),
                JsonUtil.field("toAccount", pt.getToAccountNumber() != null ? pt.getToAccountNumber() : ""),
                JsonUtil.field("description", pt.getDescription() != null ? pt.getDescription() : ""),
                JsonUtil.field("fraudReason", pt.getFraudDescription() != null ? pt.getFraudDescription() : ""),
                JsonUtil.field("severity", pt.getSeverity() != null ? pt.getSeverity() : ""),
                JsonUtil.field("secondsLeft", (int) secondsLeft),
                JsonUtil.field("createdAt", pt.getCreatedAt() != null ? pt.getCreatedAt().toString() : ""),
                JsonUtil.field("expiresAt", pt.getExpiresAt() != null ? pt.getExpiresAt().toString() : "")
        );
    }
}
