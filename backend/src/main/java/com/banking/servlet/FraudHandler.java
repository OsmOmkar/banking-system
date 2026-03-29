package com.banking.servlet;

import com.banking.model.FraudAlert;
import com.banking.model.User;
import com.banking.service.BankingService;
import com.banking.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FraudHandler extends BaseHandler {

    private final BankingService bankingService;

    public FraudHandler(BankingService service) {
        this.bankingService = service;
    }

    @Override
    protected boolean requiresAuth() { return true; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.contains("/fraud/alerts")) {
            getAlerts(exchange, user);
        } else if ("PUT".equals(method) && path.contains("/fraud/resolve")) {
            resolveAlert(exchange, body);
        } else if ("POST".equals(method) && path.contains("/fraud/reverse")) {
            reverseTransaction(exchange, body);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    private void getAlerts(HttpExchange exchange, User user) throws Exception {
        String accIdStr = getQueryParam(exchange, "accountId");
        if (accIdStr == null) {
            sendResponse(exchange, 400, JsonUtil.error("accountId required"));
            return;
        }

        int accountId = Integer.parseInt(accIdStr);
        List<FraudAlert> alerts = bankingService.getFraudAlerts(accountId);

        List<String> items = new ArrayList<>();
        for (FraudAlert alert : alerts) {
            items.add(JsonUtil.object(
                    JsonUtil.field("id", alert.getId()),
                    JsonUtil.field("alertType", alert.getAlertType()),
                    JsonUtil.field("description", alert.getDescription()),
                    JsonUtil.field("severity", alert.getSeverity().name()),
                    JsonUtil.field("resolved", alert.isResolved()),
                    JsonUtil.field("reversed", alert.isReversed()),
                    JsonUtil.field("transactionId", alert.getTransactionId()),
                    JsonUtil.field("date", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : "")
            ));
        }

        sendResponse(exchange, 200, JsonUtil.success(JsonUtil.array(items)));
    }

    private void resolveAlert(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int alertId = Integer.parseInt(data.get("alertId"));
        bankingService.resolveFraudAlert(alertId);
        sendResponse(exchange, 200, JsonUtil.success("\"Alert resolved\""));
    }

    private void reverseTransaction(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        int alertId = Integer.parseInt(data.get("alertId"));
        try {
            bankingService.reverseTransaction(alertId);
            sendResponse(exchange, 200, JsonUtil.success("\"Transaction reversed and amount credited back\""));
        } catch (Exception e) {
            sendResponse(exchange, 400, JsonUtil.error(e.getMessage()));
        }
    }
}
