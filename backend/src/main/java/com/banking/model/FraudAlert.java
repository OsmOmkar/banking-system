package com.banking.model;

import java.sql.Timestamp;

public class FraudAlert {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    private int id;
    private int accountId;
    private int transactionId;
    private String alertType;
    private String description;
    private Severity severity;
    private boolean resolved;
<<<<<<< HEAD
=======
    private boolean reversed;
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    private Timestamp createdAt;

    public FraudAlert() {}

    public FraudAlert(int accountId, int transactionId, String alertType, String description, Severity severity) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.alertType = alertType;
        this.description = description;
        this.severity = severity;
        this.resolved = false;
<<<<<<< HEAD
=======
        this.reversed = false;
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
<<<<<<< HEAD
=======
    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
