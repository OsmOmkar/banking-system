package com.banking.util;

import java.io.OutputStream;
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

    // Read from Railway environment variable (secure - not hardcoded)
    private static final String BREVO_API_KEY = System.getenv("BREVO_API_KEY");

    private static final String FROM_EMAIL = "omkar.bhagatt@gmail.com";
    private static final String FROM_NAME  = "JavaBank Security";

    public static void sendOTPEmail(String toEmail, String userName, String otp) {
        System.out.println("[EmailService] Sending OTP Email to: " + toEmail + " | OTP: " + otp);

        String subject = "JavaBank - Your Email Verification OTP";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#3b82f6;margin:0 0 8px;'>🏦 JavaBank</h2>"
                + "<p style='color:#94a3b8;margin:0 0 24px;'>Secure · Smart · Protected</p>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>Your OTP for email verification is:</p>"
                + "<div style='font-size:2.5rem;font-weight:bold;letter-spacing:12px;"
                + "background:#1e293b;padding:20px;text-align:center;border-radius:12px;"
                + "color:#60a5fa;border:2px solid #3b82f6;margin:16px 0;'>" + otp + "</div>"
                + "<p style='color:#94a3b8;font-size:0.9rem;'>⏱️ Valid for <strong>10 minutes</strong>.</p>"
                + "<p style='color:#94a3b8;font-size:0.9rem;'>🔒 Do not share this OTP with anyone.</p>"
                + "<hr style='border-color:#1e293b;margin:24px 0;'/>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        sendEmail(toEmail, userName, subject, html);
    }

    public static void sendFraudAlertEmail(String toEmail, String userName,
                                            String txType, double amount,
                                            String pendingTxId) {
        System.out.println("[EmailService] Sending FRAUD ALERT Email to: " + toEmail);

        String subject = "🚨 JavaBank Security Alert - Action Required NOW";
        String html = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#ef4444;margin:0 0 8px;'>🚨 JavaBank Security Alert</h2>"
                + "<p style='color:#94a3b8;margin:0 0 24px;'>Immediate action required</p>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>A <strong>suspicious transaction</strong> has been flagged on your account:</p>"
                + "<div style='background:#1e293b;border:1px solid #ef4444;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<p style='margin:4px 0;'><strong style='color:#94a3b8;'>Type:</strong> "
                + "<span style='color:#f1f5f9;'>" + txType + "</span></p>"
                + "<p style='margin:4px 0;'><strong style='color:#94a3b8;'>Amount:</strong> "
                + "<span style='color:#ef4444;font-size:1.2rem;font-weight:bold;'>₹"
                + String.format("%.2f", amount) + "</span></p>"
                + "<p style='margin:4px 0;'><strong style='color:#94a3b8;'>Pending ID:</strong> "
                + "<span style='color:#60a5fa;'>" + pendingTxId + "</span></p>"
                + "</div>"
                + "<div style='background:#dc2626;border-radius:8px;padding:12px;text-align:center;margin:16px 0;'>"
                + "<strong>⏱️ AUTO-CANCELS IN 5 MINUTES if no response!</strong>"
                + "</div>"
                + "<p>Log in to <strong>JavaBank</strong> → <strong>Pending Confirm</strong> "
                + "to CONFIRM or REJECT this transaction.</p>"
                + "<hr style='border-color:#1e293b;margin:24px 0;'/>"
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

            // Build JSON payload for Brevo API
            String jsonBody = "{"
                    + "\"sender\":{\"name\":\"" + escapeJson(FROM_NAME) + "\","
                    + "\"email\":\"" + FROM_EMAIL + "\"},"
                    + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\","
                    + "\"name\":\"" + escapeJson(toName != null ? toName : toEmail) + "\"}],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"htmlContent\":\"" + escapeJson(htmlBody) + "\""
                    + "}";

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("[EmailService] ✅ Email sent successfully to: " + toEmail);
            } else {
                // Read error response
                String errorResponse = new String(conn.getErrorStream().readAllBytes());
                System.err.println("[EmailService] ❌ Failed. HTTP " + responseCode
                        + " | " + errorResponse);
            }

        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Error: " + e.getMessage());
        }
    }

    // Escape special characters for JSON string
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}