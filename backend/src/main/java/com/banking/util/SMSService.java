package com.banking.util;

import java.net.URI;
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
// Route: "otp" -> FREE, designed for 6-digit OTPs, no DLT needed
// =============================================
public class SMSService {

    private static final String API_KEY = ConfigLoader.get("sms.fast2sms.apikey", "YOUR_FAST2SMS_API_KEY");

    // Fast2SMS OTP route endpoint — COMPLETELY FREE, no recharge needed
    private static final String OTP_API_URL = "https://www.fast2sms.com/dev/bulkV2";

    // Java 17 built-in HTTP client
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ---------------------------------------------------------------
    // Send OTP SMS during phone verification / login
    // ---------------------------------------------------------------
    public static void sendOTPSms(String toPhone, String userName, String otp) {
        System.out.println("========================================");
        System.out.println("[SMSService] Sending OTP SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  OTP     : " + otp);
        System.out.println("========================================");

        sendOTPRoute(toPhone, otp);
    }

    // ---------------------------------------------------------------
    // Send Fraud Alert SMS — called by FraudDetectionThread
    // (Falls back to OTP route with a short code, or logs only if unsupported)
    // ---------------------------------------------------------------
    public static void sendFraudAlertSms(String toPhone, String userName,
                                          String txType, double amount,
                                          int pendingTxId) {
        // Fraud alerts are transactional messages — these require a paid route.
        // For now, log the alert clearly so it appears in Railway logs.
        // The user can review it in the app's Pending Transactions page.
        System.out.println("========================================");
        System.out.println("[SMSService] FRAUD ALERT (logged only — transactional SMS needs paid route)");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  User    : " + userName);
        System.out.println("  Type    : " + txType);
        System.out.println("  Amount  : Rs." + String.format("%.2f", amount));
        System.out.println("  Pending : #" + pendingTxId);
        System.out.println("  Action  : User must review via app → Pending Transactions");
        System.out.println("========================================");
        // Note: To enable real SMS fraud alerts, top up Fast2SMS and switch to route=q
    }

    // ---------------------------------------------------------------
    // Core OTP sender — uses Fast2SMS route=otp (FREE)
    //
    // Fast2SMS OTP route docs:
    //   POST https://www.fast2sms.com/dev/bulkV2
    //   Body (JSON): { "variables_values": "123456", "route": "otp", "numbers": "9876543210" }
    //   Authorization: <API_KEY> header
    //
    // SYLLABUS: Unit IV - Networking (HttpClient, POST request, JSON body)
    // ---------------------------------------------------------------
    private static void sendOTPRoute(String toPhone, String otp) {
        if (API_KEY == null || API_KEY.equals("YOUR_FAST2SMS_API_KEY") || API_KEY.trim().isEmpty()) {
            System.err.println("[SMSService] Fast2SMS API key not configured. " +
                    "Set SMS_FAST2SMS_APIKEY environment variable in Railway dashboard.");
            System.out.println("[SMSService] OTP for " + toPhone + " (console fallback): " + otp);
            return;
        }

        try {
            // JSON body for OTP route — no DLT template, no paid balance needed
            // variables_values = the 6-digit OTP
            // route = "otp" → Free OTP route
            // numbers = 10-digit phone (without +91)
            String jsonBody = "{\"variables_values\":\"" + otp + "\"," +
                              "\"route\":\"otp\"," +
                              "\"numbers\":\"" + toPhone + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OTP_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("[SMSService] Fast2SMS OTP response (" + response.statusCode() + "): "
                    + response.body());

            if (response.statusCode() == 200 && response.body().contains("\"return\":true")) {
                System.out.println("[SMSService] OTP SMS sent successfully to: +91" + toPhone);
            } else {
                System.err.println("[SMSService] OTP SMS may have failed. Response above.");
                // Print OTP to console as fallback (helpful during dev)
                System.out.println("[SMSService] OTP fallback (console): " + otp + " for +91" + toPhone);
            }

        } catch (Exception e) {
            System.err.println("[SMSService] Failed to send OTP SMS: " + e.getMessage());
            System.out.println("[SMSService] OTP fallback (console): " + otp + " for +91" + toPhone);
        }
    }
}
