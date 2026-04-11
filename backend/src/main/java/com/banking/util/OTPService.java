package com.banking.util;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// =============================================
// SYLLABUS: Unit IV - Collections (ConcurrentHashMap, Set)
//           Unit I  - Random number generation
//           Unit III - Exception handling
// =============================================
public class OTPService {

    // OTP is valid for 10 minutes
    private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L;

    // Max 3 wrong attempts before OTP is invalidated
    private static final int MAX_ATTEMPTS = 3;

    // SYLLABUS: Unit IV - ConcurrentHashMap (thread-safe collection)
    // Key = email or phone, Value = OTPEntry
    private static final Map<String, OTPEntry> otpStore = new ConcurrentHashMap<>();

    // SYLLABUS: Unit IV - Set (thread-safe) for verified emails
    // An email is added here after OTP is successfully verified
    private static final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    // SYLLABUS: Unit IV - Set (thread-safe) for verified phone numbers
    // A phone is added here after SMS OTP is successfully verified
    private static final Set<String> verifiedPhones = ConcurrentHashMap.newKeySet();

    private static final SecureRandom random = new SecureRandom();

    // ----- Inner class to hold OTP data -----
    private static class OTPEntry {
        String otp;
        long expiresAt;
        int attempts;

        OTPEntry(String otp) {
            this.otp = otp;
            this.expiresAt = System.currentTimeMillis() + OTP_VALIDITY_MS;
            this.attempts = 0;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    // ----- Generate a 6-digit OTP -----
    public static String generateOTP(String key) {
        // Generate 6-digit number (secure random)
        int code = 100000 + random.nextInt(900000);
        String otp = String.valueOf(code);

        otpStore.put(key, new OTPEntry(otp));
        System.out.println("[OTPService] Generated OTP for " + key + ": " + otp);
        return otp;
    }

    // ----- Verify the OTP entered by user -----
    // Returns: "VALID", "INVALID", "EXPIRED", "MAX_ATTEMPTS"
    public static String verifyOTP(String key, String enteredOtp) {
        OTPEntry entry = otpStore.get(key);

        if (entry == null) {
            return "INVALID";
        }

        if (entry.isExpired()) {
            otpStore.remove(key);
            return "EXPIRED";
        }

        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(key);
            return "MAX_ATTEMPTS";
        }

        if (entry.otp.equals(enteredOtp)) {
            otpStore.remove(key); // OTP used successfully, remove it
            return "VALID";
        } else {
            entry.attempts++;
            return "INVALID";
        }
    }

    // ----- Check if an OTP exists and is still valid -----
    public static boolean hasValidOTP(String key) {
        OTPEntry entry = otpStore.get(key);
        return entry != null && !entry.isExpired();
    }

    // ----- Remove OTP manually (e.g., on logout) -----
    public static void clearOTP(String key) {
        otpStore.remove(key);
    }

    // =========================================================
    // Email verification tracking
    // Called after verifyOTP returns "VALID" for email registration
    // =========================================================

    /**
     * Mark an email as verified (called after successful OTP check).
     * Registration will check this before creating the account.
     */
    public static void markEmailVerified(String email) {
        verifiedEmails.add(email.toLowerCase().trim());
        System.out.println("[OTPService] Email marked as verified: " + email);
    }

    /**
     * Check if the given email has been verified via OTP.
     */
    public static boolean isEmailVerified(String email) {
        return verifiedEmails.contains(email.toLowerCase().trim());
    }

    /**
     * Remove verification marker (called after successful registration).
     */
    public static void clearVerified(String email) {
        verifiedEmails.remove(email.toLowerCase().trim());
    }

    // =========================================================
    // Phone verification tracking
    // Called after verifyOTP returns "VALID" for phone registration
    // =========================================================

    /**
     * Mark a phone number as verified (called after successful SMS OTP check).
     */
    public static void markPhoneVerified(String phone) {
        verifiedPhones.add(phone.trim());
        System.out.println("[OTPService] Phone marked as verified: +91" + phone);
    }

    /**
     * Check if the given phone number has been verified via OTP.
     */
    public static boolean isPhoneVerified(String phone) {
        return verifiedPhones.contains(phone.trim());
    }

    /**
     * Remove phone verification marker (called after successful registration).
     */
    public static void clearPhoneVerified(String phone) {
        verifiedPhones.remove(phone.trim());
    }

    // =========================================================
    // Short-lived "OTP verified" flags
    // Used by the forgot-password flow so that after OTP verification
    // the user can proceed to set a new password (within 15 minutes).
    // =========================================================

    // key -> expiry timestamp
    private static final Map<String, Long> verifiedFlags = new ConcurrentHashMap<>();
    private static final long FLAG_VALIDITY_MS = 15 * 60 * 1000L; // 15 minutes

    /**
     * Store a short-lived flag indicating the user successfully verified an OTP.
     * Used by the password-reset flow.
     */
    public static void storeVerifiedFlag(String key) {
        verifiedFlags.put(key, System.currentTimeMillis() + FLAG_VALIDITY_MS);
        System.out.println("[OTPService] Stored verified flag for: " + key);
    }

    /**
     * Check if the verified flag is set and still valid.
     */
    public static boolean isVerifiedFlagSet(String key) {
        Long expiry = verifiedFlags.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            verifiedFlags.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Consume (clear) the verified flag after it has been used.
     */
    public static void clearVerifiedFlag(String key) {
        verifiedFlags.remove(key);
        System.out.println("[OTPService] Cleared verified flag for: " + key);
    }
}