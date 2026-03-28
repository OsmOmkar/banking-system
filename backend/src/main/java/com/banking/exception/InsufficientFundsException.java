package com.banking.exception;

// =============================================
// SYLLABUS: Unit III - Custom Exception Handling
// Thrown when withdrawal exceeds available balance
// =============================================
public class InsufficientFundsException extends Exception {
    private double availableBalance;
    private double requestedAmount;

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(double available, double requested) {
        super(String.format("Insufficient funds. Available: ₹%.2f, Requested: ₹%.2f", available, requested));
        this.availableBalance = available;
        this.requestedAmount = requested;
    }

    public double getAvailableBalance() { return availableBalance; }
    public double getRequestedAmount() { return requestedAmount; }
}
