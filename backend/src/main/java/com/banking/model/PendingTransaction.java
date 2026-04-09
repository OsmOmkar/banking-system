package com.banking.model;

import java.sql.Timestamp;

// =============================================
// SYLLABUS: Unit II - Classes & Objects, Encapsulation
// Represents a transaction held for user confirmation
// when fraud is detected BEFORE it completes.
// =============================================
public class PendingTransaction {

    public enum Status {
        PENDING_CONFIRMATION,   // Waiting for user to respond
        CONFIRMED,              // User said "Yes, I made this"
        REJECTED,               // User said "No, this is fraud"
        TIMEOUT                 // No response in 5 minutes — auto cancelled
    }

    private int id;
    private int accountId;
    private String accountNumber;
    private String transactionType;   // WITHDRAWAL or TRANSFER
    private double amount;
    private String toAccountNumber;   // for transfers
    private String description;
    private String ipAddress;
    private Status status;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private Timestamp respondedAt;

    // Fraud info
    private String fraudAlertType;
    private String fraudDescription;
    private String severity;

    // User info (to send notifications)
    private int userId;
    private String userEmail;
    private String userPhone;
    private String userName;

    // Balance snapshots (for rollback if needed)
    private double originalBalance;
    private double recipientOriginalBalance;

    public PendingTransaction() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public Timestamp getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Timestamp respondedAt) { this.respondedAt = respondedAt; }

    public String getFraudAlertType() { return fraudAlertType; }
    public void setFraudAlertType(String fraudAlertType) { this.fraudAlertType = fraudAlertType; }

    public String getFraudDescription() { return fraudDescription; }
    public void setFraudDescription(String fraudDescription) { this.fraudDescription = fraudDescription; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getOriginalBalance() { return originalBalance; }
    public void setOriginalBalance(double originalBalance) { this.originalBalance = originalBalance; }

    public double getRecipientOriginalBalance() { return recipientOriginalBalance; }
    public void setRecipientOriginalBalance(double recipientOriginalBalance) {
        this.recipientOriginalBalance = recipientOriginalBalance;
    }

    // Helper: check if this pending transaction has expired (5 minutes)
    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt.getTime();
    }
}