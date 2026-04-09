package com.banking.model;

// =============================================
// SYLLABUS: Unit II - INHERITANCE (Types of Inheritance)
// CurrentAccount extends Account
// Both SavingsAccount and CurrentAccount extend Account = Hierarchical Inheritance
// =============================================
public class CurrentAccount extends Account {

    private double overdraftLimit;

    public CurrentAccount(int id, int userId, String accountNumber, double balance, double overdraftLimit) {
        super(id, userId, accountNumber, "CURRENT", balance);
        this.overdraftLimit = overdraftLimit;
    }

    public CurrentAccount() {
        super();
    }

    // OVERRIDING abstract method - different behavior than SavingsAccount
    @Override
    public double calculateInterest() {
        return 0.0; // Current accounts typically earn no interest
    }

    @Override
    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    // OVERRIDING parent method - POLYMORPHISM
    @Override
    public String getAccountSummary() {
        return String.format("Current Account[%s] Balance: ₹%.2f | Overdraft: ₹%.2f",
                getAccountNumber(), getBalance(), overdraftLimit);
    }

    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}
