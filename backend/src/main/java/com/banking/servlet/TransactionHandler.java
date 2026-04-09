package com.banking.servlet;

import com.banking.exception.*;
import com.banking.model.Transaction;
import com.banking.model.User;
import com.banking.service.BankingService;
import com.banking.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SYLLABUS: Unit III - Exception handling (InsufficientFunds, Fraud, etc.)
//           Unit II  - Inheritance (extends BaseHandler)
public class TransactionHandler extends BaseHandler {

    private final BankingService bankingService;

    public TransactionHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("POST".equals(method) && path.endsWith("/deposit")) {
                handleDeposit(exchange, user, body);
            } else if ("POST".equals(method) && path.endsWith("/withdraw")) {
                handleWithdraw(exchange, user, body);
            } else if ("POST".equals(method) && path.endsWith("/transfer")) {
                handleTransfer(exchange, user, body);
            } else if ("GET".equals(method) && path.contains("/transactions")) {
                handleGetTransactions(exchange, user);
            } else {
                sendResponse(exchange, 404, JsonUtil.error("Not found"));
            }

        // SYLLABUS: Unit III - catch custom exceptions separately
        } catch (InsufficientFundsException e) {
            sendResponse(exchange, 400, JsonUtil.error("Insufficient funds: " + e.getMessage()));
<<<<<<< HEAD

        } catch (InvalidAmountException e) {
            sendResponse(exchange, 400, JsonUtil.error("Invalid amount: " + e.getMessage()));

        } catch (AccountNotFoundException e) {
            sendResponse(exchange, 404, JsonUtil.error("Account not found: " + e.getMessage()));

        } catch (FraudDetectedException e) {
            // ----------------------------------------------------------------
            // PROACTIVE FRAUD HOLD — special response format
            // Message format: "TRANSACTION_HELD:<pendingId>:<human message>"
            // The frontend reads this and shows a "Pending Confirmation" UI
            // ----------------------------------------------------------------
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("TRANSACTION_HELD:")) {
                String[] parts = msg.split(":", 3);
                String pendingId    = parts.length > 1 ? parts[1] : "0";
                String humanMessage = parts.length > 2 ? parts[2]
                        : "Transaction held for fraud verification.";

                // Return 202 Accepted (not 403) — transaction is HELD, not blocked
                String json = "{\"success\":false,\"held\":true,\"pendingId\":" + pendingId
                        + ",\"error\":\"" + humanMessage.replace("\"", "'") + "\"}";
                sendResponse(exchange, 202, json);
            } else {
                sendResponse(exchange, 403, JsonUtil.error(
                        "Transaction blocked — fraud detected: " + msg));
            }
=======
        } catch (InvalidAmountException e) {
            sendResponse(exchange, 400, JsonUtil.error("Invalid amount: " + e.getMessage()));
        } catch (AccountNotFoundException e) {
            sendResponse(exchange, 404, JsonUtil.error("Account not found: " + e.getMessage()));
        } catch (FraudDetectedException e) {
            sendResponse(exchange, 403, JsonUtil.error("Transaction blocked - fraud detected: " + e.getMessage()));
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
        }
    }

    private void handleDeposit(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int accountId = Integer.parseInt(data.get("accountId"));
        double amount = Double.parseDouble(data.get("amount"));
        String desc   = data.getOrDefault("description", "Deposit");

        Transaction tx = bankingService.deposit(accountId, amount, desc, getIp(exchange));
        sendResponse(exchange, 200, txToJson(tx));
    }

    private void handleWithdraw(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int accountId = Integer.parseInt(data.get("accountId"));
        double amount = Double.parseDouble(data.get("amount"));
        String desc   = data.getOrDefault("description", "Withdrawal");

        Transaction tx = bankingService.withdraw(accountId, amount, desc, getIp(exchange));
        sendResponse(exchange, 200, txToJson(tx));
    }

    private void handleTransfer(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int fromId    = Integer.parseInt(data.get("fromAccountId"));
        String toAcc  = data.get("toAccountNumber");
        double amount = Double.parseDouble(data.get("amount"));
        String desc   = data.getOrDefault("description", "Transfer");

        Transaction tx = bankingService.transfer(fromId, toAcc, amount, desc, getIp(exchange));
        sendResponse(exchange, 200, txToJson(tx));
    }

    private void handleGetTransactions(HttpExchange exchange, User user) throws Exception {
        String accIdStr = getQueryParam(exchange, "accountId");
        if (accIdStr == null) {
            sendResponse(exchange, 400, JsonUtil.error("accountId required"));
            return;
        }

        int accountId = Integer.parseInt(accIdStr);
        List<Transaction> txList = bankingService.getTransactions(accountId);

        // SYLLABUS: Unit IV - ArrayList iteration (Collections)
        List<String> items = new ArrayList<>();
        for (Transaction tx : txList) {
            items.add(JsonUtil.object(
                    JsonUtil.field("id", tx.getId()),
                    JsonUtil.field("type", tx.getTransactionType().name()),
                    JsonUtil.field("amount", tx.getAmount()),
                    JsonUtil.field("balanceAfter", tx.getBalanceAfter()),
                    JsonUtil.field("description", tx.getDescription() != null ? tx.getDescription() : ""),
                    JsonUtil.field("recipient", tx.getRecipientAccount() != null ? tx.getRecipientAccount() : ""),
                    JsonUtil.field("flagged", tx.isFlagged()),
                    JsonUtil.field("date", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "")
            ));
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.array(items)));
    }

    private String txToJson(Transaction tx) {
        return JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id", tx.getId()),
                JsonUtil.field("type", tx.getTransactionType().name()),
                JsonUtil.field("amount", tx.getAmount()),
                JsonUtil.field("balanceAfter", tx.getBalanceAfter()),
                JsonUtil.field("description", tx.getDescription() != null ? tx.getDescription() : ""),
                JsonUtil.field("flagged", tx.isFlagged()),
                JsonUtil.field("date", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "")
        ));
    }
}
