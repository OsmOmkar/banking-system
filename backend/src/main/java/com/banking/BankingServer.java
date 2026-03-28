package com.banking;

import com.banking.service.BankingService;
import com.banking.servlet.*;
import com.banking.util.DatabaseConnection;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// =============================================
// SYLLABUS: Unit IV - Multithreading (thread pool executor)
//           Unit V  - JDBC (DB connection test on startup)
//           Unit II - OOP (all services wired together)
//           Unit I  - Java program entry point (main method)
// =============================================
public class BankingServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Test DB connection on startup
        System.out.println("[Server] Testing database connection...");
        if (!DatabaseConnection.testConnection()) {
            System.err.println("[Server] WARNING: Database connection failed. Check DB_URL, DB_USER, DB_PASSWORD.");
        } else {
            System.out.println("[Server] Database connected successfully.");
        }

        // Create shared BankingService (starts FraudDetectionThread internally)
        BankingService bankingService = new BankingService();

        // SYLLABUS: Unit V - Java HTTP Server (com.sun.net.httpserver)
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register routes
        server.createContext("/api/auth/login",    new AuthHandler());
        server.createContext("/api/auth/register", new AuthHandler());
        server.createContext("/api/auth/logout",   new AuthHandler());
        server.createContext("/api/accounts",      new AccountHandler(bankingService));
        server.createContext("/api/deposit",       new TransactionHandler(bankingService));
        server.createContext("/api/withdraw",      new TransactionHandler(bankingService));
        server.createContext("/api/transfer",      new TransactionHandler(bankingService));
        server.createContext("/api/transactions",  new TransactionHandler(bankingService));
        server.createContext("/api/fraud/alerts",  new FraudHandler(bankingService));
        server.createContext("/api/fraud/resolve", new FraudHandler(bankingService));
        server.createContext("/api/health",        exchange -> {
            byte[] response = "{\"status\":\"UP\",\"service\":\"JavaBankingSystem\"}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        // SYLLABUS: Unit IV - Thread pool for handling concurrent HTTP requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("====================================================");
        System.out.println("  Java Banking System started on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/api/health");
        System.out.println("  Fraud Detection Thread: RUNNING");
        System.out.println("====================================================");

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Server] Shutting down...");
            bankingService.shutdown();
            server.stop(1);
        }));
    }
}
