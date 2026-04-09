package com.banking.service;

import com.banking.model.PendingTransaction;
import com.banking.model.PendingTransaction.Status;

import java.util.List;

// =============================================
// SYLLABUS: Unit IV - Multithreading (extends Thread)
//           Unit III - Exception handling (InterruptedException)
//           Unit II  - OOP (single responsibility, encapsulation)
//
// This thread runs every 30 seconds in the background.
// It finds all pending transactions that have expired (> 5 min)
// and automatically cancels them — protecting user's money.
// =============================================
public class TransactionTimeoutMonitor extends Thread {

    private final PendingTransactionDAO pendingDAO;
    private volatile boolean running = true; // SYLLABUS: Unit IV - volatile visibility

    // Callback so BankingService can rollback balances on timeout
    public interface TimeoutCallback {
        void onTransactionTimeout(PendingTransaction pt);
    }

    private final TimeoutCallback callback;

    public TransactionTimeoutMonitor(PendingTransactionDAO dao, TimeoutCallback callback) {
        super("TransactionTimeoutMonitor");
        this.setDaemon(true); // background daemon thread
        this.pendingDAO = dao;
        this.callback = callback;
    }

    // SYLLABUS: Unit IV - Multithreading: run() override
    @Override
    public void run() {
        System.out.println("[TimeoutMonitor] Started — checking every 30 seconds...");

        while (running) {
            try {
                // SYLLABUS: Unit IV - Thread.sleep() (pauses current thread)
                Thread.sleep(30_000); // check every 30 seconds

                checkAndCancelExpired();

            } catch (InterruptedException e) {
                // SYLLABUS: Unit III - Handle InterruptedException properly
                Thread.currentThread().interrupt();
                System.out.println("[TimeoutMonitor] Interrupted — shutting down.");
                running = false;
            } catch (Exception e) {
                System.err.println("[TimeoutMonitor] Error during check: " + e.getMessage());
            }
        }

        System.out.println("[TimeoutMonitor] Stopped.");
    }

    // ---------------------------------------------------------------
    // Core logic: find expired pending transactions, cancel them
    // ---------------------------------------------------------------
    private void checkAndCancelExpired() {
        // SYLLABUS: Unit IV - Collections: iterating a List
        List<PendingTransaction> expired = pendingDAO.findExpiredPending();

        if (!expired.isEmpty()) {
            System.out.println("[TimeoutMonitor] Found " + expired.size()
                    + " expired pending transaction(s). Auto-cancelling...");
        }

        for (PendingTransaction pt : expired) {
            System.out.println("[TimeoutMonitor] TIMEOUT: Cancelling pending transaction ID "
                    + pt.getId() + " — amount: ₹" + pt.getAmount()
                    + " — account: " + pt.getAccountNumber());

            // Update status in DB
            pendingDAO.updateStatus(pt.getId(), Status.TIMEOUT);

            // Notify BankingService to rollback the held funds (if any were deducted)
            if (callback != null) {
                callback.onTransactionTimeout(pt);
            }

            System.out.println("[TimeoutMonitor] ✅ Transaction ID " + pt.getId()
                    + " auto-cancelled. Money is safe.");
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
