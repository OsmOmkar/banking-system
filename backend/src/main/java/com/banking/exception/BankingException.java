package com.banking.exception;

// =============================================
// SYLLABUS: Unit III - Custom Exceptions
// All custom exceptions for the banking system
// =============================================

// Base banking exception
class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }
    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
