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
// Using Fast2SMS route=q (Quick SMS, no DLT needed, Rs.5/msg)
// Toggle SMS on/off via sms.enabled in config.properties
//   enable-sms.bat  → sets sms.enabled=true  then git push
//   disable-sms.bat → sets sms.enabled=false then git push
// =============================================
public class SMSService {

    private static final String API_KEY = ConfigLoader.get("sms.fast2sms.apikey", "YOUR_FAST2SMS_API_KEY");

    // Fast2SMS Quick SMS endpoint (route=q, no DLT, Rs.5/msg)
    private static final String API_URL = "https://www.fast2sms.com/dev/bulkV2";

    // SMS master switch — read from config.properties / env var SMS_ENABLED
    // false = OTP shown in console only, no actual SMS sent (saves credits)
    private static final boolean SMS_ENABLED = "true".equalsIgnoreCase(
            ConfigLoader.get("sms.enabled", "true"));

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ---------------------------------------------------------------
    // Send OTP SMS (used during registration & phone login)
    // ---------------------------------------------------------------
    public static void sendOTPSms(String toPhone, String userName, String otp) {
        System.out.println("========================================");
        System.out.println("[SMSService] OTP for +91" + toPhone + " → " + otp);
        System.out.println("[SMSService] SMS_ENABLED = " + SMS_ENABLED);
        System.out.println("========================================");

        if (!SMS_ENABLED) {
            System.out.println("[SMSService] SMS is DISABLED. OTP shown above in console only.");
            System.out.println("[SMSService] Run enable-sms.bat to activate real SMS.");
            return;
        }

        String message = "Dear " + userName + ", your JavaBank OTP is: "
                + otp + ". Valid for 10 minutes. Do not share this.";
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
                + " needs your approval. Open app → Pending. Ref #" + pendingTxId;

        System.out.println("========================================");
        System.out.println("[SMSService] FRAUD ALERT for +91" + toPhone);
        System.out.println("[SMSService] " + message);
        System.out.println("[SMSService] SMS_ENABLED = " + SMS_ENABLED);
        System.out.println("========================================");

        if (!SMS_ENABLED) {
            System.out.println("[SMSService] SMS is DISABLED. Alert logged above (console only).");
            return;
        }

        sendViaQuickSMS(toPhone, message, null);
    }

    // ---------------------------------------------------------------
    // Core sender — Fast2SMS route=q
    // SYLLABUS: Unit IV - Networking (HttpClient, URI, GET request)
    // ---------------------------------------------------------------
    private static void sendViaQuickSMS(String toPhone, String messageBody, String otpFallback) {
        if (API_KEY == null || API_KEY.equals("YOUR_FAST2SMS_API_KEY") || API_KEY.trim().isEmpty()) {
            System.err.println("[SMSService] API key not configured (env: SMS_FAST2SMS_APIKEY).");
            if (otpFallback != null)
                System.out.println("[SMSService] OTP (console): " + otpFallback);
            return;
        }

        try {
            String encodedMsg = URLEncoder.encode(messageBody, StandardCharsets.UTF_8);
            String url = API_URL
                    + "?authorization=" + API_KEY
                    + "&message="   + encodedMsg
                    + "&language=english"
                    + "&route=q"
                    + "&numbers="  + toPhone;

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
                System.out.println("[SMSService] SMS sent to +91" + toPhone + " ✓");
            } else {
                System.err.println("[SMSService] SMS may have failed — check response above.");
                if (otpFallback != null)
                    System.out.println("[SMSService] OTP (console fallback): " + otpFallback);
            }

        } catch (Exception e) {
            System.err.println("[SMSService] Error: " + e.getMessage());
            if (otpFallback != null)
                System.out.println("[SMSService] OTP (console fallback): " + otpFallback);
        }
    }
}
