package com.banking.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

// =============================================
// SYLLABUS: Unit III - Packages & Utility Classes
//           Unit IV  - Networking (SMTP is a network protocol)
// =============================================
public class EmailService {

    public static void sendOTPEmail(String toEmail, String userName, String otp) {
        System.out.println("========================================");
        System.out.println("[EmailService] Sending OTP Email");
        System.out.println("  To      : " + toEmail);
        System.out.println("  Name    : " + userName);
        System.out.println("  OTP     : " + otp);
        System.out.println("  Valid   : 10 minutes");
        System.out.println("========================================");

        // Prepare email content
        String subject = "JavaBank - Email Verification OTP";
        String body = "Dear " + userName + ",\n\n" +
                "Your OTP for JavaBank email verification is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Regards,\nJavaBank Security Team";
        
        // Send the email
        sendEmail(toEmail, subject, body);
    }

    public static void sendFraudAlertEmail(String toEmail, String userName,
                                            String txType, double amount,
                                            String pendingTxId) {
        System.out.println("========================================");
        System.out.println("[EmailService] FRAUD ALERT EMAIL");
        System.out.println("  To          : " + toEmail);
        System.out.println("  User        : " + userName);
        System.out.println("  Transaction : " + txType + " of Rs." + String.format("%.2f", amount));
        System.out.println("  Pending ID  : " + pendingTxId);
        System.out.println("  ACTION NEEDED: Confirm or reject this transaction!");
        System.out.println("========================================");

        // Prepare email content
        String subject = "⚠️ JAVABANK FRAUD ALERT - Action Required";
        String body = "Dear " + userName + ",\n\n" +
                "⚠️ SECURITY ALERT ⚠️\n\n" +
                "A suspicious transaction has been flagged:\n" +
                "- Type: " + txType + "\n" +
                "- Amount: Rs." + String.format("%.2f", amount) + "\n" +
                "- Pending ID: " + pendingTxId + "\n\n" +
                "ACTION REQUIRED:\n" +
                "Please log in to your JavaBank account and confirm or reject this transaction.\n\n" +
                "⏱️ This transaction will be AUTO-CANCELLED in 5 minutes if you don't respond.\n\n" +
                "If you did NOT make this transaction, please reject it immediately and contact our support.\n\n" +
                "Regards,\nJavaBank Security Team";
        
        // Send the email
        sendEmail(toEmail, subject, body);
    }

    private static void sendEmail(String toEmail, String subject, String body) {
        // Load email configuration from config.properties
        String host = ConfigLoader.get("email.smtp.host", "smtp.gmail.com");
        String port = ConfigLoader.get("email.smtp.port", "587");
        String username = ConfigLoader.get("email.username");
        String password = ConfigLoader.get("email.password");
        String from = ConfigLoader.get("email.from", "noreply@javabank.com");

        // Check if email is configured
        if (username == null || password == null || 
            username.equals("YOUR_GMAIL_ADDRESS@gmail.com")) {
            System.err.println("[EmailService] Email not configured in config.properties!");
            System.err.println("[EmailService] Please set email.username and email.password");
            return;
        }

        // Setup SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ssl.trust", host);

        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            // Send message
            Transport.send(message);
            
            System.out.println("[EmailService] ✅ Email sent successfully to " + toEmail);
            
        } catch (MessagingException e) {
            System.err.println("[EmailService] ❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
