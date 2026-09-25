package com.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                         @Value("${app.mail.from:no-reply@dusk.app}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends the "reset your password" email. Failures are logged (and, in dev,
     * the link is logged too so the flow can still be exercised before real
     * SMTP credentials are configured in .env) rather than thrown, because the
     * caller (forgot-password) must not reveal to the client whether sending
     * succeeded — that would leak whether an email address is registered.
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        String body = "Hi " + username + ",\n\n"
                + "We received a request to reset your Dusk account password.\n"
                + "Click the link below to choose a new password. This link expires in 10 minutes "
                + "and can only be used once.\n\n"
                + resetLink + "\n\n"
                + "If you didn't request this, you can safely ignore this email — your password won't be changed.\n\n"
                + "— Dusk";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your Dusk password");
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Could not send password reset email to {}: {}", toEmail, ex.getMessage());
            log.info("Password reset link for {} (mail delivery failed, use this to test manually): {}",
                    toEmail, resetLink);
        }
    }
}
