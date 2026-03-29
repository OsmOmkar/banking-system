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

        // Health check
        server.createContext("/api/health", (HttpExchange exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            byte[] response = "OK".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.createContext("/", (HttpExchange exchange) -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            byte[] response = "JavaBank API is running!".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        // Auth routes
        server.createContext("/api/auth/login",    new AuthHandler());
        server.createContext("/api/auth/register", new AuthHandler());
        server.createContext("/api/auth/logout",   new AuthHandler());

        // Account routes
        server.createContext("/api/accounts",      new AccountHandler(bankingService));

        // Transaction routes
        server.createContext("/api/deposit",       new TransactionHandler(bankingService));
        server.createContext("/api/withdraw",      new TransactionHandler(bankingService));
        server.createContext("/api/transfer",      new TransactionHandler(bankingService));
        server.createContext("/api/transactions",  new TransactionHandler(bankingService));

        // Fraud routes
        server.createContext("/api/fraud/alerts",  new FraudHandler(bankingService));
        server.createContext("/api/fraud/resolve", new FraudHandler(bankingService));
        server.createContext("/api/fraud/reverse", new FraudHandler(bankingService));

        // UPI routes
        server.createContext("/api/upi/accounts",  new UpiHandler(bankingService));
        server.createContext("/api/upi/lookup",    new UpiHandler(bankingService));
        server.createContext("/api/upi/pay",       new UpiHandler(bankingService));

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
