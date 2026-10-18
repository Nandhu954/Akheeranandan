package com.campusfind.app.utils;

import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Sends emails automatically via Gmail SMTP in a background thread.
 * No user interaction, no dialog, no intent required.
 *
 * ── SETUP (one-time) ──────────────────────────────────────────────────────
 *  1. Create a dedicated Gmail account for the app
 *     (e.g.  campusfind.noreply@gmail.com)
 *  2. Enable 2-Factor Authentication on that account.
 *  3. Go to: Google Account → Security → App Passwords
 *     → Select "Mail" + "Other (Custom name)" → Generate
 *  4. Copy the 16-character App Password (no spaces) and paste below.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class EmailSender {

    private static final String TAG = "EmailSender";

    // ── Fill in your dedicated app Gmail address and App Password ─────────
    private static final String SENDER_EMAIL    = "campusfind.noreply@gmail.com";
    private static final String SENDER_PASSWORD = "your_16char_app_password_here";
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sends an email in a background thread. Safe to call from the UI thread.
     * Failures are logged but never crash the app.
     *
     * @param toEmail  Recipient email address
     * @param subject  Email subject
     * @param body     Plain-text email body
     */
    public static void sendAsync(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            Log.w(TAG, "sendAsync: recipient email is empty, skipping.");
            return;
        }
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host",                   "smtp.gmail.com");
                props.put("mail.smtp.port",                   "465");
                props.put("mail.smtp.auth",                   "true");
                props.put("mail.smtp.socketFactory.port",     "465");
                props.put("mail.smtp.socketFactory.class",    "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "CampusFind App"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(toEmail.trim()));
                message.setSubject(subject);
                message.setText(body);
                Transport.send(message);
                Log.d(TAG, "Email sent successfully to " + toEmail);

            } catch (Exception e) {
                Log.e(TAG, "Failed to send email to " + toEmail + " : " + e.getMessage());
            }
        }).start();
    }
}
