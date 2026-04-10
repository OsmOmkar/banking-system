package com.banking.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// =============================================
// SYLLABUS: Unit IV - Networking (HTTP API call)
//           Unit III - Packages & Utility Classes
// Using Resend.com HTTP API (no SMTP, works on Railway)
// =============================================
public class EmailService {

    private static final String RESEND_API_KEY = System.getenv("RESEND_API_KEY") != null 
    ? System.getenv("RESEND_API_KEY") : "";
    private static final String FROM_EMAIL     = "onboarding@resend.dev";

    public static void sendOTPEmail(String toEmail, String userName, String otp) {
        System.out.println("[EmailService] Sending OTP Email to: " + toEmail + " | OTP: " + otp);

        String subject = "JavaBank - Email Verification OTP";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;'>"
                + "<h2 style='color:#3b82f6;'>🏦 JavaBank</h2>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>Your OTP for email verification is:</p>"
                + "<div style='font-size:2rem;font-weight:bold;letter-spacing:8px;"
                + "background:#f1f5f9;padding:16px;text-align:center;border-radius:8px;"
                + "color:#1e293b;'>" + otp + "</div>"
                + "<p style='color:#64748b;font-size:0.9rem;'>Valid for <strong>10 minutes</strong>. "
                + "Do not share this OTP with anyone.</p>"
                + "<hr/><p style='color:#94a3b8;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        sendEmail(toEmail, subject, html);
    }

    public static void sendFraudAlertEmail(String toEmail, String userName,
                                            String txType, double amount,
                                            String pendingTxId) {
        System.out.println("[EmailService] Sending FRAUD ALERT Email to: " + toEmail);

        String subject = "⚠️ JavaBank Security Alert - Action Required";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;'>"
                + "<h2 style='color:#ef4444;'>🚨 JavaBank Security Alert</h2>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>A suspicious transaction has been detected on your account:</p>"
                + "<div style='background:#fef2f2;border:1px solid #fca5a5;border-radius:8px;padding:16px;'>"
                + "<p><strong>Type:</strong> " + txType + "</p>"
                + "<p><strong>Amount:</strong> ₹" + String.format("%.2f", amount) + "</p>"
                + "<p><strong>Pending ID:</strong> " + pendingTxId + "</p>"
                + "</div>"
                + "<p style='color:#dc2626;'><strong>⏱️ This transaction will be AUTO-CANCELLED in 5 minutes</strong> "
                + "if you do not respond.</p>"
                + "<p>Please log in to your JavaBank account and go to "
                + "<strong>Pending Confirm</strong> to confirm or reject this transaction.</p>"
                + "<p style='color:#94a3b8;font-size:0.8rem;'>If you did NOT make this transaction, "
                + "reject it immediately.</p>"
                + "<hr/><p style='color:#94a3b8;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        sendEmail(toEmail, subject, html);
    }

    // -------------------------------------------------------
    // Core method: calls Resend HTTP API
    // SYLLABUS: Unit IV - Networking (HttpURLConnection)
    // -------------------------------------------------------
    private static void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            URL url = new URL("https://api.resend.com/emails");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + RESEND_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Build JSON payload
            String jsonBody = "{"
                    + "\"from\":\"" + FROM_EMAIL + "\","
                    + "\"to\":[\"" + toEmail + "\"],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"html\":\"" + escapeJson(htmlBody) + "\""
                    + "}";

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("[EmailService] ✅ Email sent successfully to: " + toEmail);
            } else {
                System.err.println("[EmailService] ❌ Failed to send email. HTTP " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Error sending email: " + e.getMessage());
        }
    }

    // Escape special characters for JSON string
    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}