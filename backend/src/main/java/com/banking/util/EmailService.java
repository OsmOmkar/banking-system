package com.banking.util;

// =============================================
// SYLLABUS: Unit III - Packages & Utility Classes
//           Unit IV  - Networking (SMTP is a network protocol)
// =============================================
public class EmailService {

    // In demo/college mode: OTP is printed to console
    // In production: uncomment the SMTP section below and add JavaMail dependency

    public static void sendOTPEmail(String toEmail, String userName, String otp) {
        System.out.println("========================================");
        System.out.println("[EmailService] Sending OTP Email");
        System.out.println("  To      : " + toEmail);
        System.out.println("  Name    : " + userName);
        System.out.println("  OTP     : " + otp);
        System.out.println("  Valid   : 10 minutes");
        System.out.println("  Message : Your OTP for JavaBank registration is: " + otp);
        System.out.println("========================================");

        // -------------------------------------------------------
        // PRODUCTION SMTP CODE (uncomment when ready)
        // Requires: javax.mail JAR in your classpath
        // -------------------------------------------------------
        /*
        String from     = "noreply@javabank.com";
        String host     = "smtp.gmail.com";
        String smtpUser = "your_email@gmail.com";
        String smtpPass = "your_app_password";   // Gmail App Password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("JavaBank - Email Verification OTP");
            message.setText(
                "Dear " + userName + ",\n\n" +
                "Your OTP for JavaBank email verification is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Regards,\nJavaBank Security Team"
            );
            Transport.send(message);
            System.out.println("[EmailService] Email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send email: " + e.getMessage());
        }
        */
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
        System.out.println("  Confirm URL : /api/pending/" + pendingTxId + "/confirm");
        System.out.println("  Reject URL  : /api/pending/" + pendingTxId + "/reject");
        System.out.println("  Auto-cancel : in 5 minutes if no response");
        System.out.println("========================================");
    }
}