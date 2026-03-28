package com.banking.fraud;

import com.banking.model.FraudAlert;
import com.banking.model.FraudAlert.Severity;
import com.banking.model.Transaction;
import com.banking.model.Transaction.TransactionType;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// =============================================
// SYLLABUS:
//   Unit III  - Implementing Interface (FraudDetector)
//   Unit IV   - Collections: ArrayList, HashMap
//   Unit IV   - Multithreading: called from FraudDetectionThread
// =============================================
public class FraudAnalysisEngine implements FraudDetector {

    // Thresholds
    private static final double LARGE_AMOUNT_THRESHOLD   = 50000.0;
    private static final double VELOCITY_AMOUNT_THRESHOLD = 20000.0;
    private static final int    RAPID_TRANSACTION_COUNT   = 5;
    private static final long   RAPID_WINDOW_MS           = 10 * 60 * 1000L; // 10 minutes

    // SYLLABUS Unit IV - HashMap: track per-IP transaction count
    private final Map<String, List<Long>> ipTransactionMap = new HashMap<>();

    // SYLLABUS Unit III - Implementing interface method
    @Override
    public List<FraudAlert> analyze(Transaction tx, List<Transaction> recentHistory) {
        // SYLLABUS Unit IV - ArrayList to collect detected alerts
        List<FraudAlert> alerts = new ArrayList<>();

        try {
            checkLargeAmount(tx, alerts);
            checkRapidTransactions(tx, recentHistory, alerts);
            checkUnusualHour(tx, alerts);
            checkRepeatedRoundAmounts(tx, recentHistory, alerts);
            checkHighFrequencyIP(tx, alerts);
        } catch (Exception e) {
            System.err.println("[FraudEngine] Error during analysis: " + e.getMessage());
        }

        return alerts;
    }

    @Override
    public boolean isHighRisk(Transaction tx) {
        return tx.getAmount() > LARGE_AMOUNT_THRESHOLD
                || tx.getTransactionType() == TransactionType.TRANSFER;
    }

    // Rule 1: Single large transaction
    private void checkLargeAmount(Transaction tx, List<FraudAlert> alerts) {
        if (tx.getTransactionType() != TransactionType.DEPOSIT
                && tx.getAmount() > LARGE_AMOUNT_THRESHOLD) {
            alerts.add(new FraudAlert(
                    tx.getAccountId(), tx.getId(),
                    "LARGE_TRANSACTION",
                    String.format("Large %s of ₹%.2f detected (threshold: ₹%.2f)",
                            tx.getTransactionType(), tx.getAmount(), LARGE_AMOUNT_THRESHOLD),
                    tx.getAmount() > 100000 ? Severity.CRITICAL : Severity.HIGH
            ));
        }
    }

    // Rule 2: Many transactions in a short window
    private void checkRapidTransactions(Transaction tx, List<Transaction> history,
                                         List<FraudAlert> alerts) {
        if (history == null || history.isEmpty()) return;

        long now = System.currentTimeMillis();
        long windowStart = now - RAPID_WINDOW_MS;

        // SYLLABUS Unit IV - ArrayList + loop = Collections usage
        List<Transaction> recent = new ArrayList<>();
        for (Transaction t : history) {
            if (t.getCreatedAt() != null && t.getCreatedAt().getTime() > windowStart) {
                recent.add(t);
            }
        }

        if (recent.size() >= RAPID_TRANSACTION_COUNT) {
            double totalAmount = 0;
            for (Transaction t : recent) totalAmount += t.getAmount();

            alerts.add(new FraudAlert(
                    tx.getAccountId(), tx.getId(),
                    "RAPID_TRANSACTIONS",
                    String.format("%d transactions in 10 minutes totalling ₹%.2f",
                            recent.size(), totalAmount),
                    Severity.HIGH
            ));
        }
    }

    // Rule 3: Transaction at unusual hours (12AM–4AM)
    private void checkUnusualHour(Transaction tx, List<FraudAlert> alerts) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour <= 4) {
            alerts.add(new FraudAlert(
                    tx.getAccountId(), tx.getId(),
                    "UNUSUAL_HOUR",
                    String.format("Transaction at unusual hour: %d:00", hour),
                    Severity.LOW
            ));
        }
    }

    // Rule 4: Repeated round-number amounts (money laundering pattern)
    private void checkRepeatedRoundAmounts(Transaction tx, List<Transaction> history,
                                            List<FraudAlert> alerts) {
        if (history == null || tx.getAmount() % 1000 != 0) return;

        // SYLLABUS Unit IV - HashMap to count matching amounts
        Map<Double, Integer> amountFreq = new HashMap<>();
        for (Transaction t : history) {
            if (t.getAmount() % 1000 == 0) {
                amountFreq.put(t.getAmount(), amountFreq.getOrDefault(t.getAmount(), 0) + 1);
            }
        }

        for (Map.Entry<Double, Integer> entry : amountFreq.entrySet()) {
            if (entry.getValue() >= 3) {
                alerts.add(new FraudAlert(
                        tx.getAccountId(), tx.getId(),
                        "STRUCTURING",
                        String.format("Repeated round-amount transactions of ₹%.2f (%d times) — possible structuring",
                                entry.getKey(), entry.getValue()),
                        Severity.MEDIUM
                ));
                break;
            }
        }
    }

    // Rule 5: Same IP making too many transactions
    private void checkHighFrequencyIP(Transaction tx, List<FraudAlert> alerts) {
        String ip = tx.getIpAddress();
        if (ip == null || ip.isBlank()) return;

        long now = System.currentTimeMillis();
        // SYLLABUS Unit IV - HashMap<String, List<Long>> for IP tracking
        ipTransactionMap.putIfAbsent(ip, new ArrayList<>());
        List<Long> timestamps = ipTransactionMap.get(ip);

        // Clean old entries
        timestamps.removeIf(t -> (now - t) > RAPID_WINDOW_MS);
        timestamps.add(now);

        if (timestamps.size() > 10) {
            alerts.add(new FraudAlert(
                    tx.getAccountId(), tx.getId(),
                    "HIGH_FREQUENCY_IP",
                    String.format("IP %s made %d transactions in 10 minutes", ip, timestamps.size()),
                    Severity.MEDIUM
            ));
        }
    }
}
