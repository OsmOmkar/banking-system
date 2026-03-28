package com.banking;

import com.banking.service.BankingService;
import com.banking.servlet.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// =============================================
// SYLLABUS: Unit I - Java program entry point (main method)
// =============================================
public class BankingServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("[BankingServer] Starting on port " + port);

        // Test DB connection on startup
        System.out.println("[Server] Testing database connection...");
        if (!com.banking.util.DatabaseConnection.testConnection()) {
            System.err.println("[Server] WARNING: Database connection failed. Check DB_URL, DB_USER, DB_PASSWORD.");
        } else {
            System.out.println("[Server] Database connected successfully.");
        }

        BankingService bankingService = new BankingService();

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

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("[BankingServer] Server running on port " + port);

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[BankingServer] Shutting down...");
            bankingService.shutdown();
            server.stop(0);
        }));
    }
}
