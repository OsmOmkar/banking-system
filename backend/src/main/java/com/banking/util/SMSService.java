package com.banking.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// =============================================
// SYLLABUS: Unit IV - Networking (HTTP-based SMS API)
//           Unit III - Packages & Utility Classes
// Using Fast2SMS - Free Indian SMS gateway
// No external JAR needed - uses Java 17 HttpClient
// Sign up at: https://www.fast2sms.com
// =============================================
public class SMSService {

    // ------------------------------------
    // Fill this in after Fast2SMS signup:
    // Go to fast2sms.com → Dashboard → Dev API → Copy your API key
    // ------------------------------------
    private static final String API_KEY = ConfigLoader.get("sms.fast2sms.apikey", "YOUR_FAST2SMS_API_KEY");

    // Fast2SMS Quick SMS route — no DLT registration needed for dev/testing
    private static final String API_URL = "https://www.fast2sms.com/dev/bulkV2";

    // Java 17 built-in HTTP client — no extra dependency needed
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ---------------------------------------------------------------
    // Send OTP SMS during phone verification at registration
    // ---------------------------------------------------------------
    public static void sendOTPSms(String toPhone, String userName, String otp) {
        String message = "Dear " + userName + ", your JavaBank OTP is: "
                + otp + ". Valid for 10 minutes. Do not share this with anyone.";

        System.out.println("========================================");
        System.out.println("[SMSService] Sending OTP SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  OTP     : " + otp);
        System.out.println("  Message : " + message);
        System.out.println("========================================");

        sendViaFast2SMS(toPhone, message);
    }

    // ---------------------------------------------------------------
    // Send Fraud Alert SMS — called by FraudDetectionThread
    // ---------------------------------------------------------------
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

        sendViaFast2SMS(toPhone, message);
    }

    // ---------------------------------------------------------------
    // Core method — calls Fast2SMS REST API
    // SYLLABUS: Unit IV - Networking (HttpClient, URI, HTTP GET)
    // ---------------------------------------------------------------
    private static void sendViaFast2SMS(String toPhone, String messageBody) {
        // Guard: if API key is not configured yet, just log and return
        if (API_KEY == null || API_KEY.equals("YOUR_FAST2SMS_API_KEY") || API_KEY.trim().isEmpty()) {
            System.err.println("[SMSService] Fast2SMS API key not configured. " +
                    "Add 'sms.fast2sms.apikey=YOUR_KEY' to config.properties");
            return;
        }

        try {
            // Build the query string
            // route=q  → Quick SMS (no DLT needed, works for all Indian +91 numbers)
            // language=english → plain ASCII text
            // numbers  → 10-digit Indian mobile number (WITHOUT +91 prefix)
            String encodedMessage = URLEncoder.encode(messageBody, StandardCharsets.UTF_8);
            String url = API_URL
                    + "?authorization=" + API_KEY
                    + "&message=" + encodedMessage
                    + "&language=english"
                    + "&route=q"
                    + "&numbers=" + toPhone;   // e.g. 9876543210

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("[SMSService] Fast2SMS response (" + response.statusCode() + "): "
                    + response.body());

            if (response.statusCode() == 200 && response.body().contains("\"return\":true")) {
                System.out.println("[SMSService] SMS sent successfully to: +91" + toPhone);
            } else {
                System.err.println("[SMSService] SMS delivery may have failed. Check Fast2SMS response above.");
            }

        } catch (Exception e) {
            System.err.println("[SMSService] Failed to send SMS: " + e.getMessage());
        }
    }
}
