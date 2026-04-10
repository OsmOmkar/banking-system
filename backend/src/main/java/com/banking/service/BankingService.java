package com.banking.service;

import com.banking.exception.*;
import com.banking.fraud.FraudAlertDAO;
import com.banking.fraud.FraudAnalysisEngine;
import com.banking.fraud.FraudDetectionThread;
import com.banking.model.*;
import com.banking.model.FraudAlert.Severity;
import com.banking.model.Transaction.TransactionType;
import com.banking.util.EmailService;
import com.banking.util.SMSService;

import java.util.List;

// =============================================
// SYLLABUS:
//   Unit III - Implementing Interface (TransactionProcessor)
//   Unit III - Exception handling (try/catch/throw)
//   Unit IV  - Multithreading (FraudDetectionThread, TimeoutMonitor)
//   Unit II  - OOP (Account subclass polymorphism)
// =============================================
public class BankingService implements TransactionProcessor,
        FraudDetectionThread.FraudAlertCallback,
        TransactionTimeoutMonitor.TimeoutCallback {

    private final AccountDAO            accountDAO     = new AccountDAO();
    private final TransactionDAO        txDAO          = new TransactionDAO();
    private final FraudAlertDAO         fraudAlertDAO  = new FraudAlertDAO();
    private final PendingTransactionDAO pendingDAO     = new PendingTransactionDAO();

    // Quick fraud engine for BEFORE-transaction check
    private final FraudAnalysisEngine   quickEngine    = new FraudAnalysisEngine();

    // SYLLABUS: Unit IV - Multithreading: two background threads
    private final FraudDetectionThread  fraudThread;
    private final TransactionTimeoutMonitor timeoutMonitor;

    public BankingService() {
        fraudThread    = new FraudDetectionThread(this);
        timeoutMonitor = new TransactionTimeoutMonitor(pendingDAO, this);

        fraudThread.start();
        timeoutMonitor.start();

        System.out.println("[BankingService] Initialized. Fraud detection + Timeout monitor running.");
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

        // SYLLABUS: Unit II - Polymorphism: works SavingsAccount & CurrentAccount
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

        // Submit to background fraud thread (POST-transaction, for monitoring)
        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    // ---------------------------------------------------------------
    // PROACTIVE FRAUD PREVENTION — Withdrawal
    // SYLLABUS: Unit IV - Multithreading (fraud checked before money moves)
    // ---------------------------------------------------------------
    @Override
    public Transaction withdraw(int accountId, double amount, String description, String ip)
            throws InvalidAmountException, InsufficientFundsException,
                   AccountNotFoundException, FraudDetectedException {

        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);

        Account account = accountDAO.findById(accountId);

        // ---- PROACTIVE FRAUD CHECK (BEFORE money moves) ----
        Transaction tempTx = new Transaction(accountId, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), description);
        tempTx.setIpAddress(ip);

        List<Transaction> history = txDAO.findRecentByAccount(accountId, 20);
        List<FraudAlert> preAlerts = quickEngine.analyze(tempTx, history);

        // If HIGH or CRITICAL risk → HOLD, don't process yet
        if (hasHighRisk(preAlerts)) {
            FraudAlert topAlert = getTopAlert(preAlerts);

            // Find the user info (for notification)
            UserDAO userDAO = new UserDAO();
            // We need userId — look it up from the account
            com.banking.model.User owner = userDAO.findById(account.getUserId());

            // Build the PendingTransaction record
            PendingTransaction pt = new PendingTransaction();
            pt.setAccountId(accountId);
            pt.setAccountNumber(account.getAccountNumber());
            pt.setTransactionType("WITHDRAWAL");
            pt.setAmount(amount);
            pt.setDescription(description);
            pt.setIpAddress(ip);
            pt.setOriginalBalance(account.getBalance());
            pt.setFraudAlertType(topAlert != null ? topAlert.getAlertType() : "SUSPICIOUS");
            pt.setFraudDescription(topAlert != null ? topAlert.getDescription() : "Suspicious withdrawal detected");
            pt.setSeverity(topAlert != null ? topAlert.getSeverity().name() : "HIGH");

            if (owner != null) {
                pt.setUserId(owner.getId());
                pt.setUserEmail(owner.getEmail());
                pt.setUserPhone(owner.getPhone());
                pt.setUserName(owner.getFullName());
            }

            PendingTransaction saved = pendingDAO.save(pt);

            if (saved != null) {
                // Send notifications (email + SMS)
                if (owner != null) {
                    EmailService.sendFraudAlertEmail(owner.getEmail(), owner.getFullName(),
                            "WITHDRAWAL", amount, String.valueOf(saved.getId()));
                    if (owner.getPhone() != null) {
                        SMSService.sendFraudAlertSms(owner.getPhone(), owner.getFullName(),
                                "WITHDRAWAL", amount, saved.getId());
                    }
                }

                System.out.println("[BankingService] 🚨 FRAUD HOLD: Withdrawal of ₹" + amount
                        + " held for confirmation. Pending ID: " + saved.getId());

                // Throw special exception so the handler knows it's HELD (not blocked)
                throw new FraudDetectedException("TRANSACTION_HELD:" + saved.getId()
                        + ":Transaction flagged as suspicious. A confirmation request has been sent "
                        + "to your registered email and phone. You have 5 minutes to confirm or reject. "
                        + "Reason: " + (topAlert != null ? topAlert.getDescription() : "Unusual activity"));
            }
        }

        // No high-risk fraud → process normally
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

        // Submit to background fraud thread for logging
        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    // ---------------------------------------------------------------
    // PROACTIVE FRAUD PREVENTION — Transfer
    // ---------------------------------------------------------------
    @Override
    public Transaction transfer(int fromAccountId, String toAccountNumber, double amount,
                                 String description, String ip)
            throws InvalidAmountException, InsufficientFundsException,
                   AccountNotFoundException, FraudDetectedException {

        if (amount <= 0) throw new InvalidAmountException("Amount must be positive: " + amount);

        Account from = accountDAO.findById(fromAccountId);
        Account to   = accountDAO.findByAccountNumber(toAccountNumber);

        // ---- PROACTIVE FRAUD CHECK (BEFORE money moves) ----
        Transaction tempTx = new Transaction(fromAccountId, TransactionType.TRANSFER,
                amount, from.getBalance(), description);
        tempTx.setIpAddress(ip);
        tempTx.setRecipientAccount(toAccountNumber);

        List<Transaction> history = txDAO.findRecentByAccount(fromAccountId, 20);
        List<FraudAlert> preAlerts = quickEngine.analyze(tempTx, history);

        // Transfers are always higher risk — hold them if any alert triggers
        if (hasHighRisk(preAlerts)) {
            FraudAlert topAlert = getTopAlert(preAlerts);

            UserDAO userDAO = new UserDAO();
            com.banking.model.User owner = userDAO.findById(from.getUserId());

            PendingTransaction pt = new PendingTransaction();
            pt.setAccountId(fromAccountId);
            pt.setAccountNumber(from.getAccountNumber());
            pt.setTransactionType("TRANSFER");
            pt.setAmount(amount);
            pt.setToAccountNumber(toAccountNumber);
            pt.setDescription(description);
            pt.setIpAddress(ip);
            pt.setOriginalBalance(from.getBalance());
            pt.setRecipientOriginalBalance(to.getBalance());
            pt.setFraudAlertType(topAlert != null ? topAlert.getAlertType() : "SUSPICIOUS");
            pt.setFraudDescription(topAlert != null ? topAlert.getDescription() : "Suspicious transfer detected");
            pt.setSeverity(topAlert != null ? topAlert.getSeverity().name() : "HIGH");

            if (owner != null) {
                pt.setUserId(owner.getId());
                pt.setUserEmail(owner.getEmail());
                pt.setUserPhone(owner.getPhone());
                pt.setUserName(owner.getFullName());
            }

            PendingTransaction saved = pendingDAO.save(pt);

            if (saved != null) {
                if (owner != null) {
                    EmailService.sendFraudAlertEmail(owner.getEmail(), owner.getFullName(),
                            "TRANSFER to " + toAccountNumber, amount, String.valueOf(saved.getId()));
                    if (owner.getPhone() != null) {
                        SMSService.sendFraudAlertSms(owner.getPhone(), owner.getFullName(),
                                "TRANSFER", amount, saved.getId());
                    }
                }

                System.out.println("[BankingService] 🚨 FRAUD HOLD: Transfer of ₹" + amount
                        + " to " + toAccountNumber + " held. Pending ID: " + saved.getId());

                throw new FraudDetectedException("TRANSACTION_HELD:" + saved.getId()
                        + ":Transfer flagged as suspicious. A confirmation request has been sent "
                        + "to your registered email and phone. You have 5 minutes to confirm or reject. "
                        + "Reason: " + (topAlert != null ? topAlert.getDescription() : "Unusual activity"));
            }
        }

        // No high-risk fraud → process normally
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

        fraudThread.submitForAnalysis(tx, history);

        return tx;
    }

    // ---------------------------------------------------------------
    // Called by PendingTransactionHandler when user CONFIRMS a held tx
    // ---------------------------------------------------------------
    public void completeHeldTransaction(PendingTransaction pt)
            throws InvalidAmountException, InsufficientFundsException, AccountNotFoundException {

        String type = pt.getTransactionType();

        if ("WITHDRAWAL".equals(type)) {
            Account account = accountDAO.findById(pt.getAccountId());
            try {
                account.withdraw(pt.getAmount());
            } catch (InsufficientFundsException e) {
                throw new InsufficientFundsException(account.getBalance(), pt.getAmount());
            }
            accountDAO.updateBalance(pt.getAccountId(), account.getBalance());

            Transaction tx = new Transaction(pt.getAccountId(), TransactionType.WITHDRAWAL,
                    pt.getAmount(), account.getBalance(),
                    (pt.getDescription() != null ? pt.getDescription() : "Withdrawal") + " [Confirmed]");
            tx.setIpAddress(pt.getIpAddress());
            txDAO.save(tx);

            System.out.println("[BankingService] ✅ Held WITHDRAWAL completed: ₹" + pt.getAmount());

        } else if ("TRANSFER".equals(type)) {
            Account from = accountDAO.findById(pt.getAccountId());
            Account to   = accountDAO.findByAccountNumber(pt.getToAccountNumber());

            try {
                from.withdraw(pt.getAmount());
            } catch (InsufficientFundsException e) {
                throw new InsufficientFundsException(from.getBalance(), pt.getAmount());
            }
            to.deposit(pt.getAmount());

            accountDAO.updateBalance(from.getId(), from.getBalance());
            accountDAO.updateBalance(to.getId(), to.getBalance());

            Transaction tx = new Transaction(from.getId(), TransactionType.TRANSFER,
                    pt.getAmount(), from.getBalance(),
                    (pt.getDescription() != null ? pt.getDescription() : "Transfer") + " [Confirmed]");
            tx.setRecipientAccount(pt.getToAccountNumber());
            tx.setIpAddress(pt.getIpAddress());
            txDAO.save(tx);

            System.out.println("[BankingService] ✅ Held TRANSFER completed: ₹" + pt.getAmount()
                    + " → " + pt.getToAccountNumber());
        }
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
    // Called by TransactionTimeoutMonitor when a pending tx times out
    // ---------------------------------------------------------------
    @Override
    public void onTransactionTimeout(PendingTransaction pt) {
        // The money was NEVER actually deducted (we held the transaction BEFORE processing)
        // So we just need to log the event — no balance rollback needed
        System.out.println("[BankingService] ⏰ TIMEOUT: Pending transaction ID " + pt.getId()
                + " auto-cancelled. Amount ₹" + pt.getAmount() + " stays in account.");
    }

    // ---------------------------------------------------------------
    // Helper: check if any alert is HIGH or CRITICAL severity
    // ---------------------------------------------------------------
    private boolean hasHighRisk(List<FraudAlert> alerts) {
        for (FraudAlert a : alerts) {
            if (a.getSeverity() == Severity.HIGH || a.getSeverity() == Severity.CRITICAL) {
                return true;
            }
        }
        return false;
    }

    // Helper: get the highest severity alert
    private FraudAlert getTopAlert(List<FraudAlert> alerts) {
        FraudAlert top = null;
        for (FraudAlert a : alerts) {
            if (top == null || a.getSeverity().ordinal() > top.getSeverity().ordinal()) {
                top = a;
            }
        }
        return top;
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
        double interestRate   = "SAVINGS".equalsIgnoreCase(type) ? 4.5 : 0.0;
        double overdraftLimit = "CURRENT".equalsIgnoreCase(type) ? 10000.0 : 0.0;
        return accountDAO.createAccount(userId, type, initialDeposit, interestRate, overdraftLimit);
    }

    public void shutdown() {
        fraudThread.shutdown();
        timeoutMonitor.shutdown();
    }
}
