package com.banking.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

// =============================================
// SYLLABUS: Unit III - Packages & Utility Classes
//           Unit I  - String operations, regex
// =============================================
public class ValidationUtil {

    // ----- Patterns -----
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    // Indian mobile: starts with 6,7,8,9 and exactly 10 digits
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9][0-9]{9}$");

    // Password: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    // Name: only letters, spaces, apostrophes, hyphens — min 2 chars per word
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z][a-zA-Z'\\- ]{1,}$");

    // Fake / test words that should NOT appear in a real name
    private static final Set<String> FAKE_NAME_KEYWORDS = new HashSet<>(Arrays.asList(
            "test", "abcd", "xyz", "qwerty", "asdf", "dummy",
            "fake", "user", "admin", "null", "none", "name",
            "hello", "world", "foo", "bar", "baz", "abc",
            "temp", "sample", "random", "demo", "example"
    ));

    // ----- Name Validation -----

    /**
     * Returns null if valid, or an error message string if invalid.
     */
    public static String validateFullName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Full name is required.";
        }

        name = name.trim();

        // Minimum 3 characters, maximum 60
        if (name.length() < 3 || name.length() > 60) {
            return "Full name must be between 3 and 60 characters.";
        }

        // Must match name pattern (letters, spaces, hyphens, apostrophes only)
        if (!NAME_PATTERN.matcher(name).matches()) {
            return "Full name can only contain letters, spaces, hyphens, and apostrophes.";
        }

        // Must contain at least one space (first name + last name)
        if (!name.contains(" ")) {
            return "Please enter your full name (first and last name).";
        }

        // Check for fake/test keywords
        String lowerName = name.toLowerCase();
        for (String keyword : FAKE_NAME_KEYWORDS) {
            if (lowerName.contains(keyword)) {
                return "Please enter your real full name. '" + keyword + "' is not allowed.";
            }
        }

        // Each word must be at least 2 characters
        String[] parts = name.split("\\s+");
        for (String part : parts) {
            if (part.length() < 2) {
                return "Each part of your name must be at least 2 characters.";
            }
        }

        // First letter of first word must be uppercase
        if (!Character.isUpperCase(name.charAt(0))) {
            return "Full name must start with a capital letter.";
        }

        return null; // valid
    }

    // ----- Email Validation -----

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }
        email = email.trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        return null;
    }

    // ----- Phone Validation -----

    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number is required.";
        }
        // Remove spaces/dashes
        phone = phone.trim().replaceAll("[\\s\\-]", "");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "Please enter a valid 10-digit Indian mobile number.";
        }
        return null;
    }

    // ----- Password Validation -----

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Password must be at least 8 characters with uppercase, lowercase, and a number.";
        }
        return null;
    }

    // ----- Username Validation -----

    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required.";
        }
        username = username.trim();
        if (username.length() < 4 || username.length() > 30) {
            return "Username must be between 4 and 30 characters.";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Username can only contain letters, numbers, and underscores.";
        }
        return null;
    }
}