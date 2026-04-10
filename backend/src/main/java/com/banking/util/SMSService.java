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
// Using Fast2SMS - route=q (Quick SMS, no DLT needed)
// Requires ₹100+ balance (one-time recharge done)
// =============================================
public class SMSService {

    private static final String API_KEY = ConfigLoader.get("sms.fast2sms.apikey", "YOUR_FAST2SMS_API_KEY");

    // Fast2SMS Quick SMS endpoint — works with ₹100+ recharge, no DLT needed
    private static final String API_URL = "https://www.fast2sms.com/dev/bulkV2";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ---------------------------------------------------------------
    // Send OTP SMS
    // ---------------------------------------------------------------
    public static void sendOTPSms(String toPhone, String userName, String otp) {
        String message = "Dear " + userName + ", your JavaBank OTP is: "
                + otp + ". Valid for 10 minutes. Do not share this.";

        System.out.println("========================================");
        System.out.println("[SMSService] Sending OTP SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  OTP     : " + otp);
        System.out.println("========================================");

        sendViaQuickSMS(toPhone, message, otp);
    }

    // ---------------------------------------------------------------
    // Send Fraud Alert SMS
    // ---------------------------------------------------------------
    public static void sendFraudAlertSms(String toPhone, String userName,
                                          String txType, double amount,
                                          int pendingTxId) {
        String message = "JAVABANK ALERT: A " + txType + " of Rs."
                + String.format("%.2f", amount)
                + " needs your approval. Open app to CONFIRM/REJECT. "
                + "Auto-cancelled in 5 mins. Ref #" + pendingTxId;

        System.out.println("========================================");
        System.out.println("[SMSService] FRAUD ALERT SMS");
        System.out.println("  To      : +91" + toPhone);
        System.out.println("  Message : " + message);
        System.out.println("========================================");

        sendViaQuickSMS(toPhone, message, null);
    }

    // ---------------------------------------------------------------
    // Core sender — Fast2SMS route=q (Quick SMS)
    // No DLT needed. Requires ₹100+ recharge (done ✅).
    // Each SMS costs ~₹0.20–0.50 from balance.
    // SYLLABUS: Unit IV - Networking (HttpClient, URI, HTTP GET)
    // ---------------------------------------------------------------
    private static void sendViaQuickSMS(String toPhone, String messageBody, String otpFallback) {
        if (API_KEY == null || API_KEY.equals("YOUR_FAST2SMS_API_KEY") || API_KEY.trim().isEmpty()) {
            System.err.println("[SMSService] Fast2SMS API key not configured.");
            if (otpFallback != null)
                System.out.println("[SMSService] OTP (console fallback): " + otpFallback + " for +91" + toPhone);
            return;
        }

        try {
            String encodedMsg = URLEncoder.encode(messageBody, StandardCharsets.UTF_8);
            String url = API_URL
                    + "?authorization=" + API_KEY
                    + "&message="  + encodedMsg
                    + "&language=english"
                    + "&route=q"
                    + "&numbers=" + toPhone;   // 10-digit, no +91

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
                System.err.println("[SMSService] SMS delivery failed. Check response above.");
                if (otpFallback != null)
                    System.out.println("[SMSService] OTP (console fallback): " + otpFallback + " for +91" + toPhone);
            }

        } catch (Exception e) {
            System.err.println("[SMSService] SMS error: " + e.getMessage());
            if (otpFallback != null)
                System.out.println("[SMSService] OTP (console fallback): " + otpFallback + " for +91" + toPhone);
        }
    }
}
