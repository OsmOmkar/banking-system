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
            byte[] response = "JavaBank API is running!".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        // -------------------------------------------------------
        // Auth endpoints
        // -------------------------------------------------------
        server.createContext("/api/auth/login",           new AuthHandler());
        server.createContext("/api/auth/register",        new AuthHandler());
        server.createContext("/api/auth/logout",          new AuthHandler());
        server.createContext("/api/auth/send-otp",        new AuthHandler()); // verify email before registration
        server.createContext("/api/auth/verify-otp",      new AuthHandler()); // verify OTP before registration
        server.createContext("/api/auth/send-email-otp",  new AuthHandler()); // NEW: post-registration email OTP
        server.createContext("/api/auth/verify-email-otp",new AuthHandler()); // NEW: verify email OTP
        server.createContext("/api/auth/send-phone-otp",  new AuthHandler()); // NEW: phone OTP
        server.createContext("/api/auth/verify-phone-otp",new AuthHandler()); // NEW: verify phone OTP

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
