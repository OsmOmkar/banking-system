package com.banking.model;

import java.sql.Timestamp;

// =============================================
// SYLLABUS: Unit II - OOP: Encapsulation, Getters/Setters
// UPI Profile model: one per user (linked to one account)
// =============================================
public class UpiProfile {
    private int       id;
    private int       userId;
    private int       accountId;
    private String    upiId;       // full UPI ID: e.g. "omkar@javabank.com"
    private String    pinHash;     // SHA-256 hash of 4-digit PIN
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Optional join fields (not stored in DB)
    private String accountNumber;
    private String accountType;
    private String userName;

    public UpiProfile() {}

    public UpiProfile(int userId, int accountId, String upiId, String pinHash) {
        this.userId    = userId;
        this.accountId = accountId;
        this.upiId     = upiId;
        this.pinHash   = pinHash;
    }

    // Getters
    public int       getId()            { return id; }
    public int       getUserId()        { return userId; }
    public int       getAccountId()     { return accountId; }
    public String    getUpiId()         { return upiId; }
    public String    getPinHash()       { return pinHash; }
    public Timestamp getCreatedAt()     { return createdAt; }
    public Timestamp getUpdatedAt()     { return updatedAt; }
    public String    getAccountNumber() { return accountNumber; }
    public String    getAccountType()   { return accountType; }
    public String    getUserName()      { return userName; }

    // Setters
    public void setId(int id)                       { this.id = id; }
    public void setUserId(int userId)               { this.userId = userId; }
    public void setAccountId(int accountId)         { this.accountId = accountId; }
    public void setUpiId(String upiId)              { this.upiId = upiId; }
    public void setPinHash(String pinHash)          { this.pinHash = pinHash; }
    public void setCreatedAt(Timestamp createdAt)   { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt)   { this.updatedAt = updatedAt; }
    public void setAccountNumber(String n)          { this.accountNumber = n; }
    public void setAccountType(String t)            { this.accountType = t; }
    public void setUserName(String n)               { this.userName = n; }
}
