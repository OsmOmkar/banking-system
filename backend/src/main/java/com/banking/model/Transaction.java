package com.banking.model;

import java.sql.Timestamp;

// =============================================
// SYLLABUS: Unit II - Classes and Objects
// Unit IV - Collections (ArrayList of transactions)
// =============================================
public class Transaction {

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, INTEREST
    }

    private int id;
    private int accountId;
    private TransactionType transactionType;
    private double amount;
    private double balanceAfter;
    private String description;
    private String recipientAccount;
    private String ipAddress;
    private Timestamp createdAt;
    private boolean flagged;

    public Transaction() {}

    public Transaction(int accountId, TransactionType type, double amount,
                       double balanceAfter, String description) {
        this.accountId = accountId;
        this.transactionType = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.flagged = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecipientAccount() { return recipientAccount; }
    public void setRecipientAccount(String recipientAccount) { this.recipientAccount = recipientAccount; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}
