package Street.Incidents.Project.Street.Incidents.Project.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Async
    public void sendVerificationEmail(String to, String name, String verificationCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // Prepare the evaluation context
            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("verificationUrl", "http://localhost:8080/api/auth/verify-email?code=" + verificationCode);

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
            context.setVariable("loginUrl", "http://localhost:8080/login-page");

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
}