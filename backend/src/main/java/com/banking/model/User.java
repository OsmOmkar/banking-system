package com.banking.model;

<<<<<<< HEAD
import java.sql.Date;
=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
import java.sql.Timestamp;

// =============================================
// SYLLABUS: Unit II - Classes and Objects, Encapsulation
<<<<<<< HEAD
// Enhanced with KYC (Know Your Customer) fields for real banking simulation
// =============================================
public class User {

    // Basic Information
=======
// =============================================
public class User {

>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private Timestamp createdAt;
    private boolean active;

<<<<<<< HEAD
    // KYC Personal Details
    private Date dateOfBirth;
    private String gender;              // MALE, FEMALE, OTHER
    private String maritalStatus;       // SINGLE, MARRIED, DIVORCED, WIDOWED
    private String nationality;

    // Address Information
    private String residentialAddress;
    private String city;
    private String state;
    private String pinCode;
    private String permanentAddress;

    // Identity Proof
    private String aadhaarNumber;       // 12 digits
    private String panNumber;           // 10 characters
    private String passportNumber;      // Optional

    // Employment Details
    private String occupation;
    private String employerName;
    private String annualIncome;        // Range: <2L, 2-5L, 5-10L, >10L

    // Nominee Information
    private String nomineeName;
    private String nomineeRelationship;
    private String nomineePhone;

    // Verification Status
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean kycVerified;

=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public User() {}

    public User(int id, String username, String email, String fullName, String phone) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.active = true;
<<<<<<< HEAD
        this.emailVerified = false;
        this.phoneVerified = false;
        this.kycVerified = false;
    }

    // Basic Getters and Setters
=======
    }

    // Getters and Setters
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
<<<<<<< HEAD

    // KYC Personal Details Getters and Setters
    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    // Address Getters and Setters
    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    // Identity Proof Getters and Setters
    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    // Employment Getters and Setters
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(String annualIncome) { this.annualIncome = annualIncome; }

    // Nominee Getters and Setters
    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }
    public String getNomineeRelationship() { return nomineeRelationship; }
    public void setNomineeRelationship(String nomineeRelationship) { this.nomineeRelationship = nomineeRelationship; }
    public String getNomineePhone() { return nomineePhone; }
    public void setNomineePhone(String nomineePhone) { this.nomineePhone = nomineePhone; }

    // Verification Status Getters and Setters
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public boolean isKycVerified() { return kycVerified; }
    public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }
=======
>>>>>>> f06de9c560d0aae7f204cd6f9d6eec13caa025a7
}
