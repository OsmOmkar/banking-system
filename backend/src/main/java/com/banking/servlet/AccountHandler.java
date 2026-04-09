package com.banking.servlet;

import com.banking.model.Account;
import com.banking.model.User;
import com.banking.service.BankingService;
import com.banking.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SYLLABUS: Unit II - Inheritance (extends BaseHandler), Polymorphism (Account subclasses)
public class AccountHandler extends BaseHandler {

    private final BankingService bankingService;

    public AccountHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.endsWith("/accounts")) {
            listAccounts(exchange, user);
        } else if ("POST".equals(method) && path.endsWith("/accounts")) {
            createAccount(exchange, user, body);
        } else if ("GET".equals(method) && path.contains("/accounts/")) {
            getAccount(exchange, user, path);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    private void listAccounts(HttpExchange exchange, User user) throws Exception {
        // SYLLABUS: Unit IV - Collections (ArrayList)
        List<Account> accounts = bankingService.getAccountsByUser(user.getId());
        List<String> items = new ArrayList<>();

        for (Account acc : accounts) {
            // SYLLABUS: Unit II - Polymorphism: calculateInterest() is different per subclass
            items.add(JsonUtil.object(
                    JsonUtil.field("id", acc.getId()),
                    JsonUtil.field("accountNumber", acc.getAccountNumber()),
                    JsonUtil.field("accountType", acc.getAccountType()),
                    JsonUtil.field("balance", acc.getBalance()),
                    JsonUtil.field("interest", acc.calculateInterest()),
                    JsonUtil.field("overdraftLimit", acc.getOverdraftLimit()),
                    JsonUtil.field("summary", acc.getAccountSummary())
            ));
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.array(items)));
    }

    private void createAccount(HttpExchange exchange, User user, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String type = data.getOrDefault("type", "SAVINGS");
        double initial = 0;
        try { initial = Double.parseDouble(data.getOrDefault("initialDeposit", "0")); }
        catch (NumberFormatException ignored) {}

        Account acc = bankingService.createAccount(user.getId(), type, initial);
        if (acc == null) {
            sendResponse(exchange, 500, JsonUtil.error("Failed to create account"));
            return;
        }

        sendResponse(exchange, 201, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id", acc.getId()),
                JsonUtil.field("accountNumber", acc.getAccountNumber()),
                JsonUtil.field("accountType", acc.getAccountType()),
                JsonUtil.field("balance", acc.getBalance())
        )));
    }

    private void getAccount(HttpExchange exchange, User user, String path) throws Exception {
        String[] parts = path.split("/");
        int accountId = Integer.parseInt(parts[parts.length - 1]);
        Account acc = bankingService.getAccount(accountId);

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.object(
                JsonUtil.field("id", acc.getId()),
                JsonUtil.field("accountNumber", acc.getAccountNumber()),
                JsonUtil.field("accountType", acc.getAccountType()),
                JsonUtil.field("balance", acc.getBalance()),
                JsonUtil.field("interest", acc.calculateInterest()),
                JsonUtil.field("overdraftLimit", acc.getOverdraftLimit()),
                JsonUtil.field("summary", acc.getAccountSummary())
        )));
    }
}
