package com.banking.service;

import com.banking.exception.*;
import com.banking.fraud.FraudAlertDAO;
import com.banking.fraud.FraudDetectionThread;
import com.banking.model.*;
import com.banking.model.Transaction.TransactionType;

import java.util.List;

// =============================================
// SYLLABUS:
//   Unit III - Implementing Interface (TransactionProcessor)
//   Unit III - Exception handling (try/catch/throw)
//   Unit IV  - Multithreading (submits to FraudDetectionThread)
//   Unit II  - OOP (uses Account subclass polymorphism)
// =============================================
public class BankingService implements TransactionProcessor,
        FraudDetectionThread.FraudAlertCallback {

    private final AccountDAO accountDAO       = new AccountDAO();
    private final TransactionDAO txDAO        = new TransactionDAO();
    private final FraudAlertDAO fraudAlertDAO = new FraudAlertDAO();

    // SYLLABUS: Unit IV - Multithreading: background fraud detection thread
    private final FraudDetectionThread fraudThread;

    public BankingService() {
        fraudThread = new FraudDetectionThread(this);
        fraudThread.start(); // start background thread
        System.out.println("[BankingService] Initialized. Fraud detection thread running.");
    }

    // ---------------------------------------------------------------
    // SYLLABUS: Unit III - Implementing TransactionProcessor interface
    // ---------------------------------------------------------------

    @Override
    public Transaction deposit(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, AccountNotFoundException {

        // SYLLABUS: Unit III - Custom exception throwing
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);

        Account account = accountDAO.findById(accountId);

        // SYLLABUS: Unit II - Polymorphism: works for both SavingsAccount & CurrentAccount
        try {
            account.deposit(amount);
        } catch (InvalidAmountException e) {
            throw e;
        }

        accountDAO.updateBalance(accountId, account.getBalance());

        Transaction tx = new Transaction(accountId, TransactionType.DEPOSIT,
                amount, account.getBalance(), description);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);

        // Submit to background fraud thread (Unit IV - Multithreading)
        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    @Override
    public Transaction withdraw(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException,
                   AccountNotFoundException, FraudDetectedException {

        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);

        Account account = accountDAO.findById(accountId);

        // SYLLABUS: Unit III - try/catch/throw custom exceptions
        try {
            account.withdraw(amount);
        } catch (InsufficientFundsException e) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        } catch (InvalidAmountException e) {
            throw e;
        }

        accountDAO.updateBalance(accountId, account.getBalance());

        Transaction tx = new Transaction(accountId, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), description);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);

        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    @Override
    public Transaction transfer(int fromAccountId, String toAccountNumber, double amount,
                                 String description, String ip)
            throws InvalidAmountException, InsufficientFundsException,
                   AccountNotFoundException, FraudDetectedException {

        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);

        Account from = accountDAO.findById(fromAccountId);
        Account to   = accountDAO.findByAccountNumber(toAccountNumber);

        try {
            from.withdraw(amount);
        } catch (InsufficientFundsException e) {
            throw new InsufficientFundsException(from.getBalance(), amount);
        } catch (InvalidAmountException e) {
            throw e;
        }

        try {
            to.deposit(amount);
        } catch (InvalidAmountException e) {
            // rollback sender
            try { from.deposit(amount); } catch (Exception ignore) {}
            throw e;
        }

        accountDAO.updateBalance(fromAccountId, from.getBalance());
        accountDAO.updateBalance(to.getId(), to.getBalance());

        Transaction tx = new Transaction(fromAccountId, TransactionType.TRANSFER,
                amount, from.getBalance(), description);
        tx.setRecipientAccount(toAccountNumber);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);

        List<Transaction> history = txDAO.findRecentByAccount(fromAccountId, 20);
        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    // ---------------------------------------------------------------
    // SYLLABUS: Unit IV - Multithreading callback (called from FraudDetectionThread)
    // ---------------------------------------------------------------
    @Override
    public void onFraudDetected(List<FraudAlert> alerts, int transactionId) {
        txDAO.markFlagged(transactionId);
        for (FraudAlert alert : alerts) {
            fraudAlertDAO.save(alert);
            System.out.println("[BankingService] FRAUD ALERT [" + alert.getSeverity() + "]: "
                    + alert.getDescription());
        }
    }

    // ---------------------------------------------------------------
    // Additional service methods
    // ---------------------------------------------------------------

    public List<Account> getAccountsByUser(int userId) {
        return accountDAO.findByUserId(userId);
    }

    public Account getAccount(int accountId) throws AccountNotFoundException {
        return accountDAO.findById(accountId);
    }

    public List<Transaction> getTransactions(int accountId) {
        return txDAO.findByAccount(accountId);
    }

    public List<FraudAlert> getFraudAlerts(int accountId) {
        return fraudAlertDAO.findByAccountId(accountId);
    }

    public void resolveFraudAlert(int alertId) {
        fraudAlertDAO.resolve(alertId);
    }

    public Account createAccount(int userId, String type, double initialDeposit) {
        double interestRate  = "SAVINGS".equalsIgnoreCase(type) ? 4.5 : 0.0;
        double overdraftLimit = "CURRENT".equalsIgnoreCase(type) ? 10000.0 : 0.0;
        return accountDAO.createAccount(userId, type, initialDeposit, interestRate, overdraftLimit);
    }

    public void shutdown() {
        fraudThread.shutdown();
    }
}
