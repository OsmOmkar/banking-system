package com.banking.exception;

// =============================================
// SYLLABUS: Unit III - Custom Exception
// Thrown when fraud is detected on a transaction
// =============================================
public class FraudDetectedException extends Exception {
    private String fraudType;
    private double transactionAmount;

<<<<<<< HEAD
    // Main constructor (3-arg)
=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public FraudDetectedException(String fraudType, String message, double amount) {
        super(message);
        this.fraudType = fraudType;
        this.transactionAmount = amount;
    }

<<<<<<< HEAD
    // Convenience constructor (1-arg) — used for HELD transaction messages
    public FraudDetectedException(String message) {
        super(message);
        this.fraudType = "FRAUD_HOLD";
        this.transactionAmount = 0;
    }

=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public String getFraudType() { return fraudType; }
    public double getTransactionAmount() { return transactionAmount; }
}
