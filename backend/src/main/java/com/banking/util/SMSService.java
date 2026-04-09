package com.banking.util;

// =============================================
// SYLLABUS: Unit IV - Networking (HTTP-based SMS API)
//           Unit III - Packages & Utility Classes
// Using Twilio - FREE $15 trial credit (~500 SMS)
// Sign up at: https://www.twilio.com/try-twilio
// =============================================
public class SMSService {

    // ------------------------------------
    // Fill these in after Twilio signup:
    // ------------------------------------
    private static final String ACCOUNT_SID = "YOUR_TWILIO_ACCOUNT_SID";
    private static final String AUTH_TOKEN  = "YOUR_TWILIO_AUTH_TOKEN";
    private static final String FROM_PHONE  = "YOUR_TWILIO_PHONE_NUMBER"; // e.g. +12345678901

    public static void sendOTPSms(String toPhone, String userName, String otp) {
        String message = "Dear " + userName + ", your JavaBank verification OTP is: "
                + otp + ". Valid for 10 minutes. Do not share this with anyone.";

        System.out.println("========================================");
        System.out.println("[SMSService] Sending OTP SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  OTP     : " + otp);
        System.out.println("  Message : " + message);
        System.out.println("========================================");

        // Uncomment below when Twilio credentials are filled in above
        // sendViaTwilio("+91" + toPhone, message);
    }

    public static void sendFraudAlertSms(String toPhone, String userName,
                                          String txType, double amount,
                                          int pendingTxId) {
        String message = "JAVABANK ALERT: A " + txType + " of Rs."
                + String.format("%.2f", amount)
                + " is pending your approval. "
                + "Reply via app to CONFIRM or REJECT. "
                + "Auto-cancelled in 5 mins if no response. "
                + "Pending ID: " + pendingTxId;

        System.out.println("========================================");
        System.out.println("[SMSService] FRAUD ALERT SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  Message : " + message);
        System.out.println("========================================");

        // Uncomment below when Twilio credentials are filled in above
        // sendViaTwilio("+91" + toPhone, message);
    }

    // -------------------------------------------------------
    // TWILIO SEND METHOD
    // Uncomment this entire method when credentials are ready
    // Also add Twilio JAR to your project dependencies
    // Download from: https://github.com/twilio/twilio-java/releases
    // -------------------------------------------------------
    /*
    private static void sendViaTwilio(String toPhone, String messageBody) {
        try {
            com.twilio.Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            com.twilio.rest.api.v2010.account.Message.creator(
                new com.twilio.type.PhoneNumber(toPhone),
                new com.twilio.type.PhoneNumber(FROM_PHONE),
                messageBody
            ).create();
            System.out.println("[SMSService] SMS sent successfully to: " + toPhone);
        } catch (Exception e) {
            System.err.println("[SMSService] Failed to send SMS: " + e.getMessage());
        }
    }
    */
}