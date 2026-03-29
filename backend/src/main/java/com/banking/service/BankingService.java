package com.banking.service;

import com.banking.exception.*;
import com.banking.fraud.FraudAlertDAO;
import com.banking.fraud.FraudDetectionThread;
import com.banking.model.*;
import com.banking.model.Transaction.TransactionType;

import java.util.List;

public class BankingService implements TransactionProcessor, FraudDetectionThread.FraudAlertCallback {

    private final AccountDAO accountDAO       = new AccountDAO();
    private final TransactionDAO txDAO        = new TransactionDAO();
    private final FraudAlertDAO fraudAlertDAO = new FraudAlertDAO();
    private final UserDAO userDAO             = new UserDAO();
    private final FraudDetectionThread fraudThread;

    public BankingService() {
        fraudThread = new FraudDetectionThread(this);
        fraudThread.start();
        System.out.println("[BankingService] Initialized. Fraud detection thread running.");
    }

    @Override
    public Transaction deposit(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, AccountNotFoundException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);
        Account account = accountDAO.findById(accountId);
        try { account.deposit(amount); } catch (InvalidAmountException e) { throw e; }
        accountDAO.updateBalance(accountId, account.getBalance());
        Transaction tx = new Transaction(accountId, TransactionType.DEPOSIT, amount, account.getBalance(), description);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);
        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        fraudThread.submitForAnalysis(tx, history);
        return tx;
    }

    @Override
    public Transaction withdraw(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException, AccountNotFoundException, FraudDetectedException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);
        Account account = accountDAO.findById(accountId);
        try { account.withdraw(amount); }
        catch (InsufficientFundsException e) { throw new InsufficientFundsException(account.getBalance(), amount); }
        catch (InvalidAmountException e) { throw e; }
        accountDAO.updateBalance(accountId, account.getBalance());
        Transaction tx = new Transaction(accountId, TransactionType.WITHDRAWAL, amount, account.getBalance(), description);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);
        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        fraudThread.submitForAnalysis(tx, history);
        return tx;
    }

    @Override
    public Transaction transfer(int fromAccountId, String toAccountNumber, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException, AccountNotFoundException, FraudDetectedException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);
        Account from = accountDAO.findById(fromAccountId);
        Account to   = accountDAO.findByAccountNumber(toAccountNumber);
        try { from.withdraw(amount); }
        catch (InsufficientFundsException e) { throw new InsufficientFundsException(from.getBalance(), amount); }
        catch (InvalidAmountException e) { throw e; }
        try { to.deposit(amount); }
        catch (InvalidAmountException e) {
            try { from.deposit(amount); } catch (Exception ignore) {}
            throw e;
        }
        accountDAO.updateBalance(fromAccountId, from.getBalance());
        accountDAO.updateBalance(to.getId(), to.getBalance());
        Transaction tx = new Transaction(fromAccountId, TransactionType.TRANSFER, amount, from.getBalance(), description);
        tx.setRecipientAccount(toAccountNumber);
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);
        List<Transaction> history = txDAO.findRecentByAccount(fromAccountId, 20);
        fraudThread.submitForAnalysis(tx, history);
        return tx;
    }

    public Transaction upiTransfer(int fromAccountId, String toUpiId, double amount, String note, String ip)
            throws Exception {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
        Account from = accountDAO.findById(fromAccountId);
        Account to   = accountDAO.findByUpiId(toUpiId);
        if (from.getId() == to.getId()) throw new Exception("Cannot transfer to the same account");
        try { from.withdraw(amount); }
        catch (InsufficientFundsException e) { throw new InsufficientFundsException(from.getBalance(), amount); }
        try { to.deposit(amount); }
        catch (InvalidAmountException e) {
            try { from.deposit(amount); } catch (Exception ignore) {}
            throw e;
        }
        accountDAO.updateBalance(fromAccountId, from.getBalance());
        accountDAO.updateBalance(to.getId(), to.getBalance());
        String desc = "UPI to " + toUpiId + " | " + note;
        Transaction tx = new Transaction(fromAccountId, TransactionType.TRANSFER, amount, from.getBalance(), desc);
        tx.setRecipientAccount(to.getAccountNumber());
        tx.setIpAddress(ip);
        tx = txDAO.save(tx);
        List<Transaction> history = txDAO.findRecentByAccount(fromAccountId, 20);
        fraudThread.submitForAnalysis(tx, history);
        return tx;
    }

    @Override
    public void onFraudDetected(List<FraudAlert> alerts, int transactionId) {
        txDAO.markFlagged(transactionId);
        for (FraudAlert alert : alerts) {
            fraudAlertDAO.save(alert);
            System.out.println("[BankingService] FRAUD ALERT [" + alert.getSeverity() + "]: " + alert.getDescription());
        }
    }

    public void reverseTransaction(int alertId) throws Exception {
        FraudAlert alert = fraudAlertDAO.findById(alertId);
        if (alert == null) throw new Exception("Alert not found");
        if (alert.isReversed()) throw new Exception("Transaction already reversed");
        Transaction tx = txDAO.findById(alert.getTransactionId());
        if (tx == null) throw new Exception("Transaction not found");
        if (tx.getTransactionType() == Transaction.TransactionType.DEPOSIT)
            throw new Exception("Deposit transactions cannot be reversed");
        Account account = accountDAO.findById(tx.getAccountId());
        try { account.deposit(tx.getAmount()); }
        catch (InvalidAmountException e) { throw new Exception("Reversal failed: " + e.getMessage()); }
        accountDAO.updateBalance(account.getId(), account.getBalance());
        Transaction reversal = new Transaction(tx.getAccountId(), Transaction.TransactionType.DEPOSIT,
            tx.getAmount(), account.getBalance(), "Reversal of flagged transaction #" + tx.getId());
        txDAO.save(reversal);
        fraudAlertDAO.resolveAndReverse(alertId);
    }

    public Account getAccountByUpiId(String upiId) throws AccountNotFoundException {
        return accountDAO.findByUpiId(upiId);
    }

    public String getAccountOwnerName(int userId) {
        User user = userDAO.findById(userId);
        return user != null ? user.getFullName() : "Unknown";
    }

    public List<Account> getAccountsByUser(int userId) { return accountDAO.findByUserId(userId); }
    public Account getAccount(int accountId) throws AccountNotFoundException { return accountDAO.findById(accountId); }
    public List<Transaction> getTransactions(int accountId) { return txDAO.findByAccount(accountId); }
    public List<FraudAlert> getFraudAlerts(int accountId) { return fraudAlertDAO.findByAccountId(accountId); }
    public void resolveFraudAlert(int alertId) { fraudAlertDAO.resolve(alertId); }

    public Account createAccount(int userId, String type, double initialDeposit) {
        double interestRate   = "SAVINGS".equalsIgnoreCase(type) ? 4.5 : 0.0;
        double overdraftLimit = "CURRENT".equalsIgnoreCase(type) ? 10000.0 : 0.0;
        return accountDAO.createAccount(userId, type, initialDeposit, interestRate, overdraftLimit);
    }

    public void shutdown() { fraudThread.shutdown(); }
}
