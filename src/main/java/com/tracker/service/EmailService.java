package com.tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@expensetracker.com}")
    private String fromEmail;

    @Async
    public void sendBudgetExceededAlert(String toEmail, String username, String categoryName,
                                         BigDecimal budgetAmount, BigDecimal spent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Budget Exceeded - " + categoryName);
            helper.setText(buildBudgetAlertHtml(username, categoryName, budgetAmount, spent), true);
            mailSender.send(message);
            log.info("Budget exceeded alert sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send budget alert email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendSavingsGoalAchievedAlert(String toEmail, String username, String goalName, BigDecimal targetAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎉 Savings Goal Achieved - " + goalName);
            helper.setText(buildSavingsGoalHtml(username, goalName, targetAmount), true);
            mailSender.send(message);
            log.info("Savings goal achieved alert sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send savings goal email to {}: {}", toEmail, e.getMessage());
        }
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
}
