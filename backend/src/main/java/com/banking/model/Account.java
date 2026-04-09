package com.banking.model;

<<<<<<< HEAD
// =============================================
// SYLLABUS: Unit II - OOP, Encapsulation, Abstract class
// Base class for all account types
// =============================================
public abstract class Account {

    // Private fields - ENCAPSULATION (Unit II)
=======
public abstract class Account {

>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    private int id;
    private int userId;
    private String accountNumber;
    private String accountType;
    private double balance;
    private boolean active;
<<<<<<< HEAD

    // Constructor - Unit II concept
=======
    private String upiId;

>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public Account(int id, int userId, String accountNumber, String accountType, double balance) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.active = true;
    }

    public Account() {}

<<<<<<< HEAD
    // Abstract method - POLYMORPHISM (Unit II)
    // Each subclass implements differently
    public abstract double calculateInterest();

    public abstract double getOverdraftLimit();

    // Method OVERRIDING will happen in subclasses
=======
    public abstract double calculateInterest();
    public abstract double getOverdraftLimit();

>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public String getAccountSummary() {
        return String.format("Account[%s] Type: %s Balance: %.2f", accountNumber, accountType, balance);
    }

<<<<<<< HEAD
    // Deposit method with validation
    public void deposit(double amount) throws com.banking.exception.InvalidAmountException {
        if (amount <= 0) {
            throw new com.banking.exception.InvalidAmountException("Deposit amount must be positive. Got: " + amount);
        }
        this.balance += amount;
    }

    // Withdraw with exception handling - Unit III
    public void withdraw(double amount) throws com.banking.exception.InsufficientFundsException,
            com.banking.exception.InvalidAmountException {
        if (amount <= 0) {
            throw new com.banking.exception.InvalidAmountException("Withdrawal amount must be positive. Got: " + amount);
        }
        if (balance + getOverdraftLimit() < amount) {
            throw new com.banking.exception.InsufficientFundsException(
                "Insufficient funds. Balance: " + balance + ", Requested: " + amount
            );
=======
    public void deposit(double amount) throws com.banking.exception.InvalidAmountException {
        if (amount <= 0) throw new com.banking.exception.InvalidAmountException("Deposit amount must be positive. Got: " + amount);
        this.balance += amount;
    }

    public void withdraw(double amount) throws com.banking.exception.InsufficientFundsException, com.banking.exception.InvalidAmountException {
        if (amount <= 0) throw new com.banking.exception.InvalidAmountException("Withdrawal amount must be positive. Got: " + amount);
        if (balance + getOverdraftLimit() < amount) {
            throw new com.banking.exception.InsufficientFundsException("Insufficient funds. Balance: " + balance + ", Requested: " + amount);
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
        }
        this.balance -= amount;
    }

<<<<<<< HEAD
    // Getters and Setters - ENCAPSULATION
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return getAccountSummary();
    }
=======
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    @Override
    public String toString() { return getAccountSummary(); }
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
}
