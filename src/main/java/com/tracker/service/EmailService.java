package com.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

/**
 * Email notification service.
 * JavaMailSender is optional — if mail is not configured the app still works
 * and warnings are logged instead of throwing exceptions.
 */
@Slf4j
@Service
public class EmailService {

    /** Injected lazily — null when spring.mail is not configured or has invalid credentials. */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@expensetracker.com}")
    private String fromEmail;

    // ─────────────────────────────────────────────────────────────────────────
    // Public methods
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void sendBudgetExceededAlert(String toEmail, String username, String categoryName,
                                        BigDecimal budgetAmount, BigDecimal spent) {
        if (!isMailConfigured()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Budget Exceeded - " + categoryName);
            helper.setText(buildBudgetAlertHtml(username, categoryName, budgetAmount, spent), true);
            mailSender.send(message);
            log.info("Budget exceeded alert sent to {}", toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Failed to send budget alert email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendSavingsGoalAchievedAlert(String toEmail, String username, String goalName,
                                             BigDecimal targetAmount) {
        if (!isMailConfigured()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎉 Savings Goal Achieved - " + goalName);
            helper.setText(buildSavingsGoalHtml(username, goalName, targetAmount), true);
            mailSender.send(message);
            log.info("Savings goal achieved alert sent to {}", toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Failed to send savings goal email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        if (!isMailConfigured()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Password Reset Request");
            helper.setText(buildPasswordResetHtml(username, resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isMailConfigured() {
        if (mailSender == null) {
            log.debug("Mail sender not configured — skipping email notification");
            return false;
        }
        // Also skip if the username is still the placeholder value
        if ("your-email@gmail.com".equals(fromEmail) || fromEmail == null || fromEmail.isBlank()) {
            log.debug("Mail credentials not configured — skipping email notification");
            return false;
        }
        return true;
    }

    private String buildBudgetAlertHtml(String username, String categoryName,
                                        BigDecimal budgetAmount, BigDecimal spent) {
        return """
            <html><body style="font-family:Arial,sans-serif;background:#1a1a2e;color:#e0e0e0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#16213e;border-radius:12px;padding:30px;">
                <h2 style="color:#f5a623;">⚠️ Budget Exceeded!</h2>
                <p>Hi <strong>%s</strong>,</p>
                <p>You have exceeded your <strong>%s</strong> budget this month.</p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0;">
                  <tr style="background:#0f3460;"><td style="padding:10px;">Budget Limit</td><td style="padding:10px;color:#4ecca3;">₹%s</td></tr>
                  <tr><td style="padding:10px;">Amount Spent</td><td style="padding:10px;color:#e94560;">₹%s</td></tr>
                  <tr style="background:#0f3460;"><td style="padding:10px;">Over Budget</td><td style="padding:10px;color:#e94560;">₹%s</td></tr>
                </table>
                <p>Please review your spending on the <a href="http://localhost:8080" style="color:#4ecca3;">Smart Expense Tracker</a>.</p>
                <p style="color:#888;font-size:12px;">This is an automated notification from Smart Expense Tracker.</p>
              </div>
            </body></html>
            """.formatted(username, categoryName, budgetAmount, spent, spent.subtract(budgetAmount));
    }

    private String buildSavingsGoalHtml(String username, String goalName, BigDecimal targetAmount) {
        return """
            <html><body style="font-family:Arial,sans-serif;background:#1a1a2e;color:#e0e0e0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#16213e;border-radius:12px;padding:30px;">
                <h2 style="color:#4ecca3;">🎉 Congratulations!</h2>
                <p>Hi <strong>%s</strong>,</p>
                <p>You have achieved your savings goal: <strong>%s</strong>!</p>
                <p>You successfully saved <strong style="color:#4ecca3;">₹%s</strong>. Keep up the great work!</p>
                <p>Set a new savings goal on the <a href="http://localhost:8080" style="color:#4ecca3;">Smart Expense Tracker</a>.</p>
                <p style="color:#888;font-size:12px;">This is an automated notification from Smart Expense Tracker.</p>
              </div>
            </body></html>
            """.formatted(username, goalName, targetAmount);
    }

    private String buildPasswordResetHtml(String username, String resetLink) {
        return """
            <html><body style="font-family:Arial,sans-serif;background:#1a1a2e;color:#e0e0e0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#16213e;border-radius:12px;padding:30px;">
                <h2 style="color:#6366f1;">🔐 Password Reset</h2>
                <p>Hi <strong>%s</strong>,</p>
                <p>Click the button below to reset your password. This link expires in 15 minutes.</p>
                <a href="%s" style="display:inline-block;margin:20px 0;padding:12px 28px;background:#6366f1;color:#fff;border-radius:8px;text-decoration:none;font-weight:bold;">Reset Password</a>
                <p>If you did not request this, ignore this email.</p>
                <p style="color:#888;font-size:12px;">This is an automated notification from Smart Expense Tracker.</p>
              </div>
            </body></html>
            """.formatted(username, resetLink);
    }
}
