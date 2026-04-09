package com.banking.servlet;

import com.banking.exception.AccountNotFoundException;
import com.banking.model.Account;
import com.banking.model.User;
import com.banking.service.BankingService;
import com.banking.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class UpiHandler extends BaseHandler {

    private final BankingService bankingService;

    public UpiHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.contains("/upi/accounts")) {
            getUpiAccounts(exchange, user);
        } else if ("GET".equals(method) && path.contains("/upi/lookup")) {
            lookupUpi(exchange);
        } else if ("POST".equals(method) && path.contains("/upi/pay")) {
            upiPay(exchange, user, body);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    private void getUpiAccounts(HttpExchange exchange, User user) throws Exception {
        var accounts = bankingService.getAccountsByUser(user.getId());
        var items = new java.util.ArrayList<String>();
        for (Account acc : accounts) {
            items.add(JsonUtil.object(
                JsonUtil.field("id", acc.getId()),
                JsonUtil.field("accountNumber", acc.getAccountNumber()),
                JsonUtil.field("accountType", acc.getAccountType()),
                JsonUtil.field("balance", acc.getBalance()),
                JsonUtil.field("upiId", acc.getUpiId() != null ? acc.getUpiId() : acc.getAccountNumber() + "@javabank")
            ));
        }
        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.array(items)));
    }

    private void lookupUpi(HttpExchange exchange) throws Exception {
        String upiId = getQueryParam(exchange, "upiId");
        if (upiId == null) {
            sendResponse(exchange, 400, JsonUtil.error("upiId required"));
            return;
        }
        try {
            Account acc = bankingService.getAccountByUpiId(upiId);
            String ownerName = bankingService.getAccountOwnerName(acc.getUserId());
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("upiId", upiId),
                JsonUtil.field("name", ownerName),
                JsonUtil.field("accountType", acc.getAccountType())
            )));
        } catch (AccountNotFoundException e) {
            sendResponse(exchange, 404, JsonUtil.error("UPI ID not found: " + upiId));
        }
    }

    private void upiPay(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int fromAccountId = Integer.parseInt(data.get("fromAccountId"));
        String toUpiId   = data.get("toUpiId");
        double amount    = Double.parseDouble(data.get("amount"));
        String note      = data.getOrDefault("note", "UPI Payment");

        try {
            var tx = bankingService.upiTransfer(fromAccountId, toUpiId, amount, note, getIp(exchange));
            sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id", tx.getId()),
                JsonUtil.field("amount", tx.getAmount()),
                JsonUtil.field("balanceAfter", tx.getBalanceAfter()),
                JsonUtil.field("flagged", tx.isFlagged()),
                JsonUtil.field("date", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "")
            )));
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonUtil.error(e.getMessage()));
        }
    }
}
