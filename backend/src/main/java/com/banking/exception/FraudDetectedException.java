package com.banking.exception;

// =============================================
// SYLLABUS: Unit III - Custom Exception
// Thrown when fraud is detected on a transaction
// =============================================
public class FraudDetectedException extends Exception {
    private String fraudType;
    private double transactionAmount;

    public FraudDetectedException(String fraudType, String message, double amount) {
        super(message);
        this.fraudType = fraudType;
        this.transactionAmount = amount;
    }

    public String getFraudType() { return fraudType; }
    public double getTransactionAmount() { return transactionAmount; }
}
