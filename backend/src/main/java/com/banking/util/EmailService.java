package com.banking.util;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// =============================================
// SYLLABUS: Unit IV - Networking (HTTP API call)
//           Unit III - Packages & Utility Classes
// Using Brevo (Sendinblue) HTTP API
// Free tier: 300 emails/day to ANY email address
// =============================================
public class EmailService {

    private static final String BREVO_API_KEY = System.getenv("BREVO_API_KEY") != null
            ? System.getenv("BREVO_API_KEY") : "";

    private static final String FROM_EMAIL = "omkar.bhagatt@gmail.com";
    private static final String FROM_NAME  = "JavaBank Security";

    public static void sendOTPEmail(String toEmail, String userName, String otp) {
        System.out.println("[EmailService] Sending OTP Email to: " + toEmail + " | OTP: " + otp);

        String subject = "JavaBank - Email Verification OTP";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#3b82f6;'>JavaBank</h2>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>Your OTP for email verification is:</p>"
                + "<div style='font-size:2.5rem;font-weight:bold;letter-spacing:12px;"
                + "background:#1e293b;padding:20px;text-align:center;border-radius:12px;"
                + "color:#60a5fa;border:2px solid #3b82f6;margin:16px 0;'>" + otp + "</div>"
                + "<p style='color:#94a3b8;'>Valid for <strong>10 minutes</strong>. "
                + "Do not share this OTP with anyone.</p>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        sendEmail(toEmail, userName, subject, html);
    }

    public static void sendFraudAlertEmail(String toEmail, String userName,
                                            String txType, double amount,
                                            String pendingTxId) {
        System.out.println("[EmailService] Sending FRAUD ALERT Email to: " + toEmail);

        String subject = "JAVABANK SECURITY ALERT - Action Required";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#ef4444;'>JavaBank Security Alert</h2>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>A suspicious transaction has been flagged on your account:</p>"
                + "<div style='background:#1e293b;border:1px solid #ef4444;"
                + "border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<p><strong>Type:</strong> " + txType + "</p>"
                + "<p><strong>Amount:</strong> Rs." + String.format("%.2f", amount) + "</p>"
                + "<p><strong>Pending ID:</strong> " + pendingTxId + "</p>"
                + "</div>"
                + "<div style='background:#dc2626;border-radius:8px;padding:12px;"
                + "text-align:center;margin:16px 0;color:white;font-weight:bold;'>"
                + "AUTO-CANCELS IN 5 MINUTES if no response!"
                + "</div>"
                + "<p>Log in to JavaBank and go to Pending Confirm to CONFIRM or REJECT.</p>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        sendEmail(toEmail, userName, subject, html);
    }

    // -------------------------------------------------------
    // Core method: calls Brevo HTTP API
    // SYLLABUS: Unit IV - Networking (HttpURLConnection)
    // -------------------------------------------------------
    private static void sendEmail(String toEmail, String toName,
                                   String subject, String htmlBody) {
        if (BREVO_API_KEY.isEmpty()) {
            System.err.println("[EmailService] BREVO_API_KEY not set in environment variables!");
            return;
        }

        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Build JSON payload
            String safeToName = toName != null ? escapeJson(toName) : escapeJson(toEmail);
            String jsonBody = "{"
                    + "\"sender\":{\"name\":\"" + FROM_NAME + "\","
                    + "\"email\":\"" + FROM_EMAIL + "\"},"
                    + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\","
                    + "\"name\":\"" + safeToName + "\"}],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"htmlContent\":\"" + escapeJson(htmlBody) + "\""
                    + "}";

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("[EmailService] Email sent successfully to: " + toEmail);
            } else {
                InputStream err = conn.getErrorStream();
                String errorResponse = err != null
                        ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "no error body";
                System.err.println("[EmailService] Failed. HTTP " + responseCode + " | " + errorResponse);
            }

        } catch (Exception e) {
            System.err.println("[EmailService] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Escape special characters for JSON string
    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Skip non-ASCII characters (emojis etc) that break JSON
            if (c > 127) continue;
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}