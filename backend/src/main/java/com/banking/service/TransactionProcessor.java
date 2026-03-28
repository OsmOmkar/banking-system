package com.banking.service;

import com.banking.exception.*;
import com.banking.model.Transaction;

// =============================================
// SYLLABUS: Unit III - Interfaces
// Defines the contract for processing transactions
// =============================================
public interface TransactionProcessor {
    Transaction deposit(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, AccountNotFoundException;

    Transaction withdraw(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException, AccountNotFoundException, FraudDetectedException;

    Transaction transfer(int fromAccountId, String toAccountNumber, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException, AccountNotFoundException, FraudDetectedException;
}
