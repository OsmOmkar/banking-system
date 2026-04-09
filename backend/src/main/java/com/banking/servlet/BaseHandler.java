package com.banking.servlet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.banking.service.UserDAO;
import com.banking.model.User;
import com.banking.util.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// =============================================
// SYLLABUS: Unit II - Abstract class
// Unit III - Interfaces (HttpHandler)
// =============================================
public abstract class BaseHandler implements HttpHandler {

    protected final UserDAO userDAO = new UserDAO();

    // SYLLABUS: Unit II - abstract method (subclasses must implement)
    protected abstract void handleRequest(HttpExchange exchange, User user, String body) throws Exception;
    protected abstract boolean requiresAuth();

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        // CORS headers for frontend communication
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            User user = null;
            if (requiresAuth()) {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.startsWith("Bearer ")) {
                    sendResponse(exchange, 401, JsonUtil.error("Unauthorized"));
                    return;
                }
                user = userDAO.getUserFromToken(auth.substring(7));
                if (user == null) {
                    sendResponse(exchange, 401, JsonUtil.error("Invalid or expired token"));
                    return;
                }
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            handleRequest(exchange, user, body);

        } catch (Exception e) {
            System.err.println("[Handler] Error: " + e.getMessage());
            sendResponse(exchange, 500, JsonUtil.error(e.getMessage()));
        }
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    // SYLLABUS: Unit IV - String manipulation, Unit III - Scanner-like parsing
    protected Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        // Simple JSON parser (pure Java - no libraries)
        json = json.trim().replaceAll("[{}]", "");
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String val = kv[1].trim().replaceAll("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    protected String getQueryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    protected String getIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) return forwarded.split(",")[0].trim();
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }
}
