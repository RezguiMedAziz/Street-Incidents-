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

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

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
            context.setVariable("loginUrl", frontendUrl + "/login-page");

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
            context.setVariable("loginUrl", frontendUrl + "/login-page");

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
            context.setVariable("loginUrl", frontendUrl + "/login-page");

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

    @Async
    public void sendAccountCredentialsEmail(String toEmail, String name, String email,
                                            String password, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Account Has Been Created - Street Incidents");

            // Prepare Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("email", email);
            context.setVariable("password", password);
            context.setVariable("role", role);
            context.setVariable("loginUrl", frontendUrl + "/login-page");

            // Process the template
            String htmlContent = templateEngine.process("email/account-credentials", context);
            helper.setText(htmlContent, true);

            // Set from email
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(message);
            log.info("Account credentials email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send account credentials email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send account credentials email", e);
        }
    }

    /**
     * Send account credentials to user
     */
    @Async
    public void sendAccountCredentials(String email, String name, String password, String role) {
        sendAccountCredentialsEmail(email, name, email, password, role);
    }

    /**
     * Send account credentials for admin-created users
     */
    @Async
    public void sendAdminCreatedAccount(String email, String fullName, String password, String role) {
        sendAccountCredentialsEmail(email, fullName, email, password, role);
    }

    /**
     * Send account update notification email
     */
    @Async
    public void sendAccountUpdateNotification(String toEmail, String name, String email,
                                              String newPassword, String role,
                                              boolean emailChanged, boolean passwordChanged,
                                              boolean roleChanged) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Account Information Has Been Updated - Street Incidents");

            // Prepare Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("name", name);
            context.setVariable("email", email);
            context.setVariable("newPassword", newPassword);
            context.setVariable("role", role);
            context.setVariable("emailChanged", emailChanged);
            context.setVariable("passwordChanged", passwordChanged);
            context.setVariable("roleChanged", roleChanged);
            context.setVariable("loginUrl", frontendUrl + "/login-page");

            // Process the template
            String htmlContent = templateEngine.process("email/account-update-notification", context);
            helper.setText(htmlContent, true);

            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(message);
            log.info("Account update notification email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send account update notification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send account update notification email", e);
        }
    }

    // ========================================
    // ✅ INCIDENT ASSIGNMENT NOTIFICATION
    // ========================================

    /**
     * Send incident assignment notification to agent
     */
    @Async
    public void sendIncidentAssignmentNotification(String toEmail, String agentName, Long incidentId,
                                                   String incidentTitle, String incidentDescription,
                                                   String category, String priority) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("New Incident Assigned - #" + incidentId + " - Street Incidents");

            // Inline HTML (no template needed for now - you can create a template later)
            String htmlContent = buildIncidentAssignmentHtml(
                    agentName, incidentId, incidentTitle, incidentDescription, category, priority
            );

            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(message);
            log.info("Incident assignment notification sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send incident assignment notification to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send incident assignment email", e);
        }
    }

    /**
     * Build HTML content for incident assignment email
     */
    private String buildIncidentAssignmentHtml(String agentName, Long incidentId,
                                               String title, String description,
                                               String category, String priority) {
        // Determine priority color
        String priorityColor = switch (priority.toLowerCase()) {
            case "haute" -> "#dc2626";
            case "moyenne" -> "#f59e0b";
            case "faible" -> "#10b981";
            default -> "#6b7280";
        };

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            background-color: #f3f4f6;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 30px auto;
                            background: white;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #3b82f6 0%%, #2563eb 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 30px;
                        }
                        .greeting {
                            font-size: 16px;
                            margin-bottom: 20px;
                        }
                        .incident-card {
                            background: #f9fafb;
                            border: 1px solid #e5e7eb;
                            border-radius: 8px;
                            padding: 20px;
                            margin: 20px 0;
                        }
                        .incident-card h2 {
                            color: #1f2937;
                            font-size: 18px;
                            margin: 0 0 15px 0;
                        }
                        .detail-row {
                            padding: 10px 0;
                            border-bottom: 1px solid #e5e7eb;
                        }
                        .detail-row:last-child {
                            border-bottom: none;
                        }
                        .detail-label {
                            font-weight: 600;
                            color: #6b7280;
                            font-size: 13px;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                        }
                        .detail-value {
                            color: #1f2937;
                            margin-top: 5px;
                            font-size: 15px;
                        }
                        .priority-badge {
                            display: inline-block;
                            padding: 4px 12px;
                            border-radius: 12px;
                            font-size: 13px;
                            font-weight: 600;
                            color: white;
                            background-color: %s;
                        }
                        .btn {
                            display: inline-block;
                            padding: 12px 30px;
                            background: #3b82f6;
                            color: white;
                            text-decoration: none;
                            border-radius: 6px;
                            margin: 20px 0;
                            font-weight: 600;
                        }
                        .btn:hover {
                            background: #2563eb;
                        }
                        .footer {
                            background: #f9fafb;
                            padding: 20px;
                            text-align: center;
                            font-size: 13px;
                            color: #6b7280;
                            border-top: 1px solid #e5e7eb;
                        }
                        .icon {
                            font-size: 48px;
                            margin-bottom: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="icon">🔔</div>
                            <h1>New Incident Assigned</h1>
                        </div>
                        <div class="content">
                            <p class="greeting">Hello <strong>%s</strong>,</p>
                            <p>A new incident has been assigned to you. Please review the details below and take the necessary actions.</p>
                            
                            <div class="incident-card">
                                <h2>Incident Details</h2>
                                
                                <div class="detail-row">
                                    <div class="detail-label">Incident ID</div>
                                    <div class="detail-value">#%d</div>
                                </div>
                                
                                <div class="detail-row">
                                    <div class="detail-label">Title</div>
                                    <div class="detail-value">%s</div>
                                </div>
                                
                                <div class="detail-row">
                                    <div class="detail-label">Description</div>
                                    <div class="detail-value">%s</div>
                                </div>
                                
                                <div class="detail-row">
                                    <div class="detail-label">Category</div>
                                    <div class="detail-value">%s</div>
                                </div>
                                
                                <div class="detail-row">
                                    <div class="detail-label">Priority</div>
                                    <div class="detail-value">
                                        <span class="priority-badge">%s</span>
                                    </div>
                                </div>
                            </div>
                            
                            <p>Please log in to the system to view the full details, including location, photos, and reporter information.</p>
                            
                            <center>
                                <a href="%s/login-page" class="btn">View in Dashboard</a>
                            </center>
                            
                            <p style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                                If you have any questions or need assistance, please contact the administration team.
                            </p>
                            
                            <p style="margin-top: 20px;">
                                Best regards,<br>
                                <strong>Street Incidents Team</strong>
                            </p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p style="margin-top: 10px;">© 2025 Street Incidents. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                priorityColor,
                agentName,
                incidentId,
                title != null ? title : "No title",
                description != null ? description : "No description provided",
                category != null ? category : "Uncategorized",
                priority != null ? priority.toUpperCase() : "MOYENNE",
                frontendUrl
        );
    }

    // ========================================
    // ✅ INCIDENT STATUS UPDATE NOTIFICATION
    // ========================================

    /**
     * Send incident status update notification to citizen
     */
    @Async
    public void sendIncidentStatusUpdateToCitizen(String toEmail, String citizenName, Long incidentId,
                                                  String incidentTitle, String oldStatus,
                                                  String newStatus, String agentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Mise à jour de votre incident #" + incidentId + " - Street Incidents");

            String htmlContent = buildIncidentStatusUpdateHtml(
                    citizenName, incidentId, incidentTitle, oldStatus, newStatus, agentName
            );

            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(message);
            log.info("Incident status update notification sent successfully to citizen: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send status update notification to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send status update email", e);
        }
    }

    /**
     * Build HTML content for citizen status update email
     */
    private String buildIncidentStatusUpdateHtml(String citizenName, Long incidentId,
                                                 String title, String oldStatus,
                                                 String newStatus, String agentName) {
        // Determine status color and message
        String statusColor = switch (newStatus.toLowerCase()) {
            case "pris_en_charge" -> "#f59e0b";
            case "en_resolution" -> "#3b82f6";
            case "resolu" -> "#10b981";
            case "cloture" -> "#6b7280";
            default -> "#3b82f6";
        };

        String statusMessage = switch (newStatus) {
            case "PRIS_EN_CHARGE" -> "pris en charge";
            case "EN_RESOLUTION" -> "en cours de résolution";
            case "RESOLU" -> "résolu";
            case "CLOTURE" -> "clôturé";
            default -> "mis à jour";
        };

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f3f4f6;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 30px auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .content {
                        padding: 30px;
                    }
                    .greeting {
                        font-size: 16px;
                        margin-bottom: 20px;
                    }
                    .status-badge {
                        display: inline-block;
                        padding: 8px 16px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 600;
                        color: white;
                        background-color: %s;
                        margin: 10px 0;
                    }
                    .incident-info {
                        background: #f9fafb;
                        border-left: 4px solid #10b981;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .incident-info strong {
                        color: #1f2937;
                    }
                    .agent-info {
                        background: #eff6ff;
                        border-left: 4px solid #3b82f6;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 20px;
                        text-align: center;
                        font-size: 13px;
                        color: #6b7280;
                        border-top: 1px solid #e5e7eb;
                    }
                    .icon {
                        font-size: 48px;
                        margin-bottom: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">✅</div>
                        <h1>Mise à jour de votre incident</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">Bonjour <strong>%s</strong>,</p>
                        <p>Nous avons une bonne nouvelle ! Votre incident a été %s.</p>
                        
                        <div class="incident-info">
                            <p style="margin: 5px 0;"><strong>Incident #%d:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Nouveau statut:</strong> <span class="status-badge">%s</span></p>
                        </div>
                        
                        %s
                        
                        <p style="margin-top: 20px;">
                            Nous vous tiendrons informé de l'évolution de votre incident. 
                            Merci de votre patience et de votre confiance.
                        </p>
                        
                        <p style="margin-top: 30px;">
                            Cordialement,<br>
                            <strong>L'équipe Street Incidents</strong>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Ceci est un message automatique. Merci de ne pas répondre à cet email.</p>
                        <p style="margin-top: 10px;">© 2025 Street Incidents. Tous droits réservés.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                statusColor,
                citizenName,
                statusMessage,
                incidentId,
                title != null ? title : "Sans titre",
                newStatus.replace("_", " "),
                agentName != null ?
                        String.format("<div class=\"agent-info\"><p style=\"margin: 5px 0;\">👤 <strong>Agent assigné:</strong> %s</p></div>", agentName)
                        : ""
        );
    }

    // ========================================
    // ✅ NEW: INCIDENT CLOSURE NOTIFICATION
    // ========================================

    /**
     * Send incident closure notification to citizen
     */
    @Async
    public void sendIncidentClosureNotification(String citizenEmail, String citizenName,
                                                Long incidentId, String incidentTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(citizenEmail);
            helper.setSubject("Incident #" + incidentId + " - CLÔTURÉ - Street Incidents");

            String htmlContent = buildIncidentClosureHtml(citizenName, incidentId, incidentTitle);

            helper.setText(htmlContent, true);
            helper.setFrom("noreply@streetincidents.com");

            mailSender.send(message);
            log.info("Incident closure notification sent successfully to citizen: {}", citizenEmail);

        } catch (MessagingException e) {
            log.error("Failed to send incident closure notification to {}: {}", citizenEmail, e.getMessage());
            throw new RuntimeException("Failed to send incident closure email", e);
        }
    }

    /**
     * Build HTML content for incident closure email
     */
    private String buildIncidentClosureHtml(String citizenName, Long incidentId, String incidentTitle) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f3f4f6;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 30px auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #6b7280 0%%, #4b5563 100%%);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .content {
                        padding: 30px;
                    }
                    .greeting {
                        font-size: 16px;
                        margin-bottom: 20px;
                    }
                    .incident-card {
                        background: #f9fafb;
                        border-left: 4px solid #6b7280;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 8px;
                    }
                    .incident-card h2 {
                        color: #1f2937;
                        font-size: 18px;
                        margin: 0 0 15px 0;
                    }
                    .incident-card p {
                        margin: 8px 0;
                    }
                    .status-badge {
                        display: inline-block;
                        padding: 8px 16px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 600;
                        color: white;
                        background-color: #6b7280;
                        margin: 10px 0;
                    }
                    .thank-you-box {
                        background: #dbeafe;
                        border-left: 4px solid #3b82f6;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                        font-size: 15px;
                    }
                    .btn {
                        display: inline-block;
                        padding: 12px 30px;
                        background: #6b7280;
                        color: white;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 20px 0;
                        font-weight: 600;
                    }
                    .btn:hover {
                        background: #4b5563;
                    }
                    .footer {
                        background: #f9fafb;
                        padding: 20px;
                        text-align: center;
                        font-size: 13px;
                        color: #6b7280;
                        border-top: 1px solid #e5e7eb;
                    }
                    .icon {
                        font-size: 48px;
                        margin-bottom: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">🔒</div>
                        <h1>Incident Clôturé</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">Bonjour <strong>%s</strong>,</p>
                        <p>Nous vous informons que votre incident a été officiellement <strong>CLÔTURÉ</strong> par notre équipe administrative.</p>
                        
                        <div class="incident-card">
                            <h2>Détails de l'incident</h2>
                            <p><strong>ID :</strong> #%d</p>
                            <p><strong>Titre :</strong> %s</p>
                            <p><strong>Statut :</strong> <span class="status-badge">CLÔTURÉ</span></p>
                        </div>
                        
                        <div class="thank-you-box">
                            <p style="margin: 0;">
                                <strong>🙏 Merci pour votre signalement !</strong><br>
                                Votre contribution aide à améliorer les services de notre municipalité.
                            </p>
                        </div>
                        
                        <p style="margin-top: 20px;">
                            Si vous rencontrez d'autres problèmes dans votre quartier, n'hésitez pas à signaler un nouvel incident.
                        </p>
                        
                        <center>
                            <a href="%s/citizen/incidents" class="btn">Voir mes incidents</a>
                        </center>
                        
                        <p style="margin-top: 30px;">
                            Cordialement,<br>
                            <strong>L'équipe Street Incidents</strong>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Ceci est un message automatique. Merci de ne pas répondre à cet email.</p>
                        <p style="margin-top: 10px;">© 2025 Street Incidents. Tous droits réservés.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                citizenName,
                incidentId,
                incidentTitle != null ? incidentTitle : "Sans titre",
                frontendUrl
        );
    }
}