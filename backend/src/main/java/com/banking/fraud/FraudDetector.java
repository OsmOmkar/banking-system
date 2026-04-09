package com.banking.fraud;

import com.banking.model.Transaction;
import com.banking.model.FraudAlert;
import java.util.List;

// =============================================
// SYLLABUS: Unit III - Interfaces
// Contract for fraud detection implementations
// =============================================
public interface FraudDetector {
    List<FraudAlert> analyze(Transaction transaction, List<Transaction> recentHistory);
    boolean isHighRisk(Transaction transaction);
}
