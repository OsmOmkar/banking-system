package com.banking;

import com.banking.service.BankingService;
import com.banking.servlet.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class BankingServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("[BankingServer] Starting on port " + port);

        if (!com.banking.util.DatabaseConnection.testConnection()) {
            System.err.println("[Server] WARNING: Database connection failed.");
        } else {
            System.out.println("[Server] Database connected successfully.");
        }

        BankingService bankingService = new BankingService();

        // Health check endpoints
        server.createContext("/api/health", (HttpExchange exchange) -> {
            byte[] response = "OK".getBytes();
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.createContext("/", (HttpExchange exchange) -> {
            // Always add CORS headers for every response from root handler
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");

            // Handle CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();

            // Domain-verification file passthrough (e.g. Fast2SMS)
            if (path.endsWith(".txt") || path.contains("verify") || path.contains("verification")) {
                String filename = path.replaceFirst("^/", "");
                byte[] response = filename.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                System.out.println("[BankingServer] Served verification file: " + path);
                return;
            }

            byte[] response = "JavaBank API is running!".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        // -------------------------------------------------------
        // Auth endpoints
        // -------------------------------------------------------
        server.createContext("/api/auth/login",              new AuthHandler());
        server.createContext("/api/auth/register",             new AuthHandler());
        server.createContext("/api/auth/logout",               new AuthHandler());
        server.createContext("/api/auth/send-otp",             new AuthHandler());
        server.createContext("/api/auth/verify-otp",           new AuthHandler());
        server.createContext("/api/auth/send-email-otp",       new AuthHandler());
        server.createContext("/api/auth/verify-email-otp",     new AuthHandler());
        server.createContext("/api/auth/send-phone-otp",       new AuthHandler());
        server.createContext("/api/auth/verify-phone-otp",     new AuthHandler());
        server.createContext("/api/auth/login-phone-otp",      new AuthHandler()); // Mobile OTP login - send OTP
        server.createContext("/api/auth/verify-phone-login",   new AuthHandler()); // Mobile OTP login - verify
        server.createContext("/api/auth/check-availability",         new AuthHandler()); // Check username/email/phone taken
        server.createContext("/api/auth/forgot-password-send-otp",    new AuthHandler()); // Forgot password - send OTP
        server.createContext("/api/auth/forgot-password-verify-otp",  new AuthHandler()); // Forgot password - verify OTP
        server.createContext("/api/auth/forgot-password-reset",       new AuthHandler()); // Forgot password - set new password

        // -------------------------------------------------------
        // Profile endpoints (require auth)
        // -------------------------------------------------------
        ProfileHandler profileHandler = new ProfileHandler();
        server.createContext("/api/profile",                   profileHandler); // GET profile
        server.createContext("/api/profile/update-username",   profileHandler); // POST change username
        server.createContext("/api/profile/update-email",      profileHandler); // POST initiate email change
        server.createContext("/api/profile/verify-new-email",  profileHandler); // POST verify new email OTP
        server.createContext("/api/profile/update-phone",      profileHandler); // POST initiate phone change
        server.createContext("/api/profile/verify-new-phone",  profileHandler); // POST verify new phone OTP
        server.createContext("/api/profile/delete-account",         profileHandler); // POST delete account
        server.createContext("/api/profile/change-password",         profileHandler); // POST change password
        server.createContext("/api/profile/send-change-email-otp",   profileHandler); // POST send OTP before email change
        server.createContext("/api/profile/send-change-phone-otp",   profileHandler); // POST send OTP before phone change

        // -------------------------------------------------------
        // Account & transaction endpoints
        // -------------------------------------------------------
        server.createContext("/api/accounts",      new AccountHandler(bankingService));
        server.createContext("/api/deposit",       new TransactionHandler(bankingService));
        server.createContext("/api/withdraw",      new TransactionHandler(bankingService));
        server.createContext("/api/transfer",      new TransactionHandler(bankingService));
        server.createContext("/api/transactions",  new TransactionHandler(bankingService));

        // -------------------------------------------------------
        // Fraud endpoints
        // -------------------------------------------------------
        server.createContext("/api/fraud/alerts",  new FraudHandler(bankingService));
        server.createContext("/api/fraud/resolve", new FraudHandler(bankingService));

        // -------------------------------------------------------
        // Proactive fraud — pending transaction confirmation
        // NEW: handles the 5-minute hold & confirm/reject system
        // -------------------------------------------------------
        PendingTransactionHandler pendingHandler = new PendingTransactionHandler(bankingService);
        server.createContext("/api/pending",         pendingHandler); // GET: list pending
        server.createContext("/api/pending/confirm", pendingHandler); // POST: confirm
        server.createContext("/api/pending/reject",  pendingHandler); // POST: reject

        // -------------------------------------------------------
        // UPI endpoints
        // -------------------------------------------------------
        UpiHandler upiHandler = new UpiHandler(bankingService);
        server.createContext("/api/upi/profile",                 upiHandler); // GET: user's UPI profile
        server.createContext("/api/upi/setup",                   upiHandler); // POST: setup UPI
        server.createContext("/api/upi/check-id",               upiHandler); // GET: check UPI ID availability
        server.createContext("/api/upi/lookup",                  upiHandler); // GET: lookup recipient by UPI ID
        server.createContext("/api/upi/pay",                     upiHandler); // POST: make UPI payment
        server.createContext("/api/upi/transactions",            upiHandler); // GET: UPI transaction history
        server.createContext("/api/upi/change-pin/send-otp",    upiHandler); // POST: send OTP for PIN change
        server.createContext("/api/upi/change-pin/verify-otp",  upiHandler); // POST: verify OTP
        server.createContext("/api/upi/change-pin",             upiHandler); // POST: set new PIN

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("[BankingServer] Server running on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[BankingServer] Shutting down...");
            bankingService.shutdown();
            server.stop(0);
        }));
    }
}
