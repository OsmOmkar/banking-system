package com.banking.exception;

// =============================================
// SYLLABUS: Unit III - Custom Exception
// Thrown when fraud is detected on a transaction
// =============================================
public class FraudDetectedException extends Exception {
    private String fraudType;
    private double transactionAmount;

    // Main constructor (3-arg)
    public FraudDetectedException(String fraudType, String message, double amount) {
        super(message);
        this.fraudType = fraudType;
        this.transactionAmount = amount;
    }

    // Convenience constructor (1-arg) — used for HELD transaction messages
    public FraudDetectedException(String message) {
        super(message);
        this.fraudType = "FRAUD_HOLD";
        this.transactionAmount = 0;
    }

    public String getFraudType() { return fraudType; }
    public double getTransactionAmount() { return transactionAmount; }
}
