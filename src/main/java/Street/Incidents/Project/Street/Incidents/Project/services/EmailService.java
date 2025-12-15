package Street.Incidents.Project.Street.Incidents.Project.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Async
    public void sendVerificationEmail(String to, String name, String verificationCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // Prepare the evaluation context
            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("verificationUrl", baseUrl + "/verify-email?code=" + verificationCode);

            // Process the template
            String htmlContent = templateEngine.process("email/verification-email", context);

            // Set email properties
            helper.setTo(to);
            helper.setSubject("Verify Your Email - Street Incidents");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            // Send email
            mailSender.send(mimeMessage);
            log.info("Verification email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("loginUrl", baseUrl + "/login-page");

            String htmlContent = templateEngine.process("email/welcome-email", context);

            helper.setTo(to);
            helper.setSubject("Welcome to Street Incidents!");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(mimeMessage);
            log.info("Welcome email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String resetToken) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetUrl", baseUrl + "/reset-password-page?token=" + resetToken);
            context.setVariable("expiryHours", 2);

            String htmlContent = templateEngine.process("email/password-reset-email", context);

            helper.setTo(to);
            helper.setSubject("Reset Your Password - Street Incidents");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(mimeMessage);
            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    @Async
    public void sendPasswordChangeVerificationEmail(String to, String name, String verificationToken) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("verificationToken", verificationToken);
            context.setVariable("verificationUrl", baseUrl + "/confirm-password-change?token=" + verificationToken);
            context.setVariable("expiryHours", 2);

            String htmlContent = templateEngine.process("email/password-change-verification-email", context);

            helper.setTo(to);
            helper.setSubject("Confirm Your Password Change - Street Incidents");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(mimeMessage);
            log.info("Password change verification email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password change verification email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send password change verification email");
        }
    }

    @Async
    public void sendPasswordChangeConfirmationEmail(String to, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("loginUrl", baseUrl + "/login-page");

            String htmlContent = templateEngine.process("email/password-reset-confirmation-email", context);

            helper.setTo(to);
            helper.setSubject("Password Reset Successful - Street Incidents");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(mimeMessage);
            log.info("Password reset confirmation email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset confirmation email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangeCompletedEmail(String to, String name) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("loginUrl", baseUrl + "/login-page");

            String htmlContent = templateEngine.process("email/password-change-completed-email", context);

            helper.setTo(to);
            helper.setSubject("Password Change Completed - Street Incidents");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(mimeMessage);
            log.info("Password change completion email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password change completion email to {}: {}", to, e.getMessage());
        }
    }
}