package com.banking.fraud;

import com.banking.model.FraudAlert;
import com.banking.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// =============================================
// SYLLABUS: Unit IV - Multithreading & Concurrency
// Runs as a background thread, continuously checking
// incoming transactions for fraud patterns.
//
// Concepts used:
//   - extends Thread (Unit IV)
//   - run() method override
//   - wait() / notify() via BlockingQueue
//   - Thread synchronization
// =============================================
public class FraudDetectionThread extends Thread {

    // Thread-safe queue — producer-consumer pattern (Unit IV lab example)
    private final BlockingQueue<TransactionJob> jobQueue = new LinkedBlockingQueue<>();
    private final FraudAnalysisEngine engine = new FraudAnalysisEngine();
    private volatile boolean running = true; // volatile for thread visibility

    // Callback interface so services can respond to fraud detection
    public interface FraudAlertCallback {
        void onFraudDetected(List<FraudAlert> alerts, int transactionId);
    }

    private FraudAlertCallback callback;

    public FraudDetectionThread(FraudAlertCallback callback) {
        super("FraudDetectionThread");
        this.setDaemon(true); // background daemon thread
        this.callback = callback;
    }

    // Inner class to hold job data - OOP concept
    public static class TransactionJob {
        public final Transaction transaction;
        public final List<Transaction> recentHistory;

        public TransactionJob(Transaction tx, List<Transaction> history) {
            this.transaction = tx;
            this.recentHistory = history != null ? history : new ArrayList<>();
        }
    }

    // Submit a transaction for async fraud checking
    public void submitForAnalysis(Transaction tx, List<Transaction> history) {
        jobQueue.offer(new TransactionJob(tx, history));
    }

    // SYLLABUS: Unit IV - Multithreading run() method
    @Override
    public void run() {
        System.out.println("[FraudDetectionThread] Started — monitoring transactions...");

        while (running) {
            try {
                // Blocks until a job is available (wait/notify internally)
                TransactionJob job = jobQueue.take();

                System.out.println("[FraudDetectionThread] Analyzing transaction ID: "
                        + job.transaction.getId());

                List<FraudAlert> alerts = engine.analyze(job.transaction, job.recentHistory);

                if (!alerts.isEmpty() && callback != null) {
                    callback.onFraudDetected(alerts, job.transaction.getId());
                    System.out.println("[FraudDetectionThread] " + alerts.size()
                            + " alert(s) raised for transaction " + job.transaction.getId());
                }

            } catch (InterruptedException e) {
                // SYLLABUS: Unit III - Exception Handling
                Thread.currentThread().interrupt();
                System.out.println("[FraudDetectionThread] Interrupted, shutting down.");
                running = false;
            } catch (Exception e) {
                System.err.println("[FraudDetectionThread] Error: " + e.getMessage());
            }
        }

        System.out.println("[FraudDetectionThread] Stopped.");
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    public int getQueueSize() {
        return jobQueue.size();
    }
}
