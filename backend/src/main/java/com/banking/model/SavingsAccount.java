package com.banking.model;

// =============================================
// SYLLABUS: Unit II - INHERITANCE
// SavingsAccount extends Account (parent class)
// =============================================
public class SavingsAccount extends Account {

    private double interestRate; // Annual interest rate in %

    // Constructor calling super() - Unit II
    public SavingsAccount(int id, int userId, String accountNumber, double balance, double interestRate) {
        super(id, userId, accountNumber, "SAVINGS", balance);
        this.interestRate = interestRate;
    }

    public SavingsAccount() {
        super();
    }

    // OVERRIDING abstract method - POLYMORPHISM (Unit II)
    @Override
    public double calculateInterest() {
        // Simple interest for one month
        return (getBalance() * interestRate) / (100 * 12);
    }

    @Override
    public double getOverdraftLimit() {
        return 0.0; // Savings accounts have no overdraft
    }

    // OVERRIDING parent method - Method Overriding (Unit II)
    @Override
    public String getAccountSummary() {
        return String.format("Savings Account[%s] Balance: ₹%.2f | Interest Rate: %.2f%%",
                getAccountNumber(), getBalance(), interestRate);
    }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
}
