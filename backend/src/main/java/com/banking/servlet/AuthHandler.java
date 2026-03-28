package com.banking.servlet;

import com.banking.exception.AuthenticationException;
import com.banking.model.User;
import com.banking.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

// SYLLABUS: Unit II - Inheritance (extends BaseHandler)
//           Unit III - Exception handling
public class AuthHandler extends BaseHandler {

    @Override
    protected boolean requiresAuth() { return false; }

    @Override
    protected void handleRequest(HttpExchange exchange, User user, String body) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equals(method) && path.endsWith("/login")) {
            handleLogin(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/register")) {
            handleRegister(exchange, body);
        } else if ("POST".equals(method) && path.endsWith("/logout")) {
            handleLogout(exchange);
        } else {
            sendResponse(exchange, 404, JsonUtil.error("Not found"));
        }
    }

    private void handleLogin(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);
        String username = data.get("username");
        String password = data.get("password");

        if (username == null || password == null) {
            sendResponse(exchange, 400, JsonUtil.error("Username and password required"));
            return;
        }

        // SYLLABUS: Unit III - Custom exception
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

    private void handleRegister(HttpExchange exchange, String body) throws Exception {
        Map<String, String> data = parseJson(body);

        String username = data.get("username");
        String email    = data.get("email");
        String fullName = data.get("fullName");
        String phone    = data.get("phone");
        String password = data.get("password");

        if (username == null || email == null || password == null || fullName == null) {
            sendResponse(exchange, 400, JsonUtil.error("All fields are required"));
            return;
        }

        User newUser = userDAO.register(username, email, fullName, phone, password);
        if (newUser == null) {
            sendResponse(exchange, 409, JsonUtil.error("Username or email already exists"));
            return;
        }

        String token = userDAO.createSession(newUser.getId());
        String json = JsonUtil.success(JsonUtil.object(
                JsonUtil.field("token", token),
                JsonUtil.field("userId", newUser.getId()),
                JsonUtil.field("username", newUser.getUsername()),
                JsonUtil.field("fullName", newUser.getFullName())
        ));
        sendResponse(exchange, 201, json);
    }

    private void handleLogout(HttpExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            userDAO.deleteSession(auth.substring(7));
        }
        sendResponse(exchange, 200, JsonUtil.success("\"Logged out\""));
    }
}
