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
    // Transaction SUCCESS email (deposit, withdraw, transfer)
    // -------------------------------------------------------
    public static void sendTransactionSuccessEmail(
            String toEmail, String userName,
            String txType, double amount,
            String txId, double balanceAfter,
            String description, String toAccount) {

        String amtStr  = String.format("%.2f", amount);
        String balStr  = String.format("%.2f", balanceAfter);
        String subject = "JavaBank: " + txType + " of Rs." + amtStr + " Successful";

        String toRow = (toAccount != null && !toAccount.isEmpty())
            ? "<p><strong>To Account:</strong> " + toAccount + "</p>" : "";

        String descRow = (description != null && !description.isEmpty())
            ? "<p><strong>Description:</strong> " + description + "</p>" : "";

        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#22c55e;margin-bottom:4px;'>Transaction Successful</h2>"
                + "<p style='color:#64748b;margin-top:0;'>JavaBank Notification</p>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>Your transaction has been completed successfully.</p>"
                + "<div style='background:#1e293b;border:1px solid #22c55e;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<div style='text-align:center;margin-bottom:12px;'>"
                + "<span style='font-size:2rem;font-weight:800;color:#22c55e;'>Rs." + amtStr + "</span><br>"
                + "<span style='font-size:0.8rem;color:#64748b;text-transform:uppercase;'>" + txType + "</span>"
                + "</div>"
                + "<p><strong>Transaction ID:</strong> TXN-" + txId + "</p>"
                + "<p><strong>Type:</strong> " + txType + "</p>"
                + toRow + descRow
                + "<p><strong>Balance After:</strong> Rs." + balStr + "</p>"
                + "<p><strong>Date & Time:</strong> " + new java.util.Date() + "</p>"
                + "</div>"
                + "<p style='color:#475569;font-size:0.8rem;'>If you did not initiate this transaction, "
                + "please contact JavaBank support immediately.</p>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        new Thread(() -> sendEmail(toEmail, userName, subject, html)).start();
    }

    // -------------------------------------------------------
    // Transaction HELD email (fraud hold — needs confirmation)
    // -------------------------------------------------------
    public static void sendTransactionHeldEmail(
            String toEmail, String userName,
            String txType, double amount,
            int pendingId, String fraudReason) {

        String amtStr  = String.format("%.2f", amount);
        String subject = "ACTION REQUIRED: JavaBank Transaction Held — Rs." + amtStr;
        String pendingUrl = "https://java-banking-webapp.vercel.app/pages/pending.html";
        String reasonStr = (fraudReason != null && !fraudReason.isEmpty())
            ? fraudReason : "Unusual activity detected";

        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#f59e0b;margin-bottom:4px;'>Transaction Paused</h2>"
                + "<p style='color:#64748b;margin-top:0;'>Action Required — JavaBank Security</p>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>A transaction on your account has been <strong style='color:#f59e0b;'>temporarily held</strong> "
                + "by our fraud protection system. Your money has <strong>NOT been deducted</strong> yet.</p>"
                + "<div style='background:#1e293b;border:2px solid #f59e0b;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<div style='text-align:center;margin-bottom:12px;'>"
                + "<span style='font-size:2rem;font-weight:800;color:#f59e0b;'>Rs." + amtStr + "</span><br>"
                + "<span style='font-size:0.8rem;color:#64748b;text-transform:uppercase;'>" + txType + " ON HOLD</span>"
                + "</div>"
                + "<p><strong>Reason:</strong> " + reasonStr + "</p>"
                + "<p><strong>Reference:</strong> Pending #" + pendingId + "</p>"
                + "<p><strong>Held At:</strong> " + new java.util.Date() + "</p>"
                + "</div>"
                + "<div style='background:#dc2626;border-radius:10px;padding:14px;text-align:center;margin:16px 0;'>"
                + "<strong style='color:white;'>AUTO-CANCELS IN 5 MINUTES if no action!</strong>"
                + "</div>"
                + "<p><strong>To confirm or reject this transaction, tap the button below:</strong></p>"
                + "<div style='text-align:center;margin:20px 0;'>"
                + "<a href='" + pendingUrl + "' style='display:inline-block;background:#f59e0b;"
                + "color:#0f172a;font-weight:700;padding:14px 32px;border-radius:10px;"
                + "text-decoration:none;font-size:1rem;'>Review Pending Transaction</a>"
                + "</div>"
                + "<p style='color:#475569;font-size:0.8rem;'>If you did not make this transaction, "
                + "click the button above and select Reject to keep your money safe.</p>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        new Thread(() -> sendEmail(toEmail, userName, subject, html)).start();
    }

    // -------------------------------------------------------
    // Transaction FAILED email
    // -------------------------------------------------------
    public static void sendTransactionFailedEmail(
            String toEmail, String userName,
            String txType, double amount, String reason) {

        String amtStr  = String.format("%.2f", amount);
        String subject = "JavaBank: " + txType + " of Rs." + amtStr + " Failed";

        String html = "<div style='font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "background:#0f172a;color:#f1f5f9;padding:32px;border-radius:16px;'>"
                + "<h2 style='color:#ef4444;margin-bottom:4px;'>Transaction Failed</h2>"
                + "<p style='color:#64748b;margin-top:0;'>JavaBank Notification</p>"
                + "<p>Dear <strong>" + userName + "</strong>,</p>"
                + "<p>Your recent transaction could not be completed.</p>"
                + "<div style='background:#1e293b;border:1px solid #ef4444;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<div style='text-align:center;margin-bottom:12px;'>"
                + "<span style='font-size:2rem;font-weight:800;color:#ef4444;'>Rs." + amtStr + "</span><br>"
                + "<span style='font-size:0.8rem;color:#64748b;text-transform:uppercase;'>" + txType + " FAILED</span>"
                + "</div>"
                + "<p><strong>Reason:</strong> " + (reason != null ? reason : "Transaction could not be processed") + "</p>"
                + "<p><strong>Date & Time:</strong> " + new java.util.Date() + "</p>"
                + "</div>"
                + "<p>No money has been deducted from your account.</p>"
                + "<p style='color:#475569;font-size:0.8rem;'>JavaBank Security Team</p>"
                + "</div>";

        new Thread(() -> sendEmail(toEmail, userName, subject, html)).start();
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