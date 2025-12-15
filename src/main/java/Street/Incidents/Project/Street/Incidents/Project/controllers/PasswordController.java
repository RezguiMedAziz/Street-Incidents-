package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.*;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final UserService userService;

    // ========================================
    // FORGET PASSWORD PAGES
    // ========================================

    @GetMapping("/forgot-password-page")
    public String forgotPasswordPage(Model model) {
        log.info("Accessing forgot password page");
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    @GetMapping("/reset-password-page")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        log.info("Accessing reset password page. Token: {}", token);

        if (token != null) {
            try {
                // Validate token
                boolean isValid = userService.validateResetToken(token);
                if (isValid) {
                    ResetPasswordRequest resetRequest = new ResetPasswordRequest();
                    resetRequest.setToken(token);
                    model.addAttribute("resetPasswordRequest", resetRequest);
                    model.addAttribute("tokenValid", true);
                }
            } catch (Exception e) {
                log.error("Invalid token: {}", e.getMessage());
                model.addAttribute("error", e.getMessage());
                model.addAttribute("tokenValid", false);
            }
        } else {
            model.addAttribute("error", "No reset token provided");
            model.addAttribute("tokenValid", false);
        }

        return "reset-password";
    }

    @GetMapping("/verify-reset-token-page")
    public String verifyResetTokenPage(Model model) {
        log.info("Accessing verify reset token page");
        model.addAttribute("verifyTokenRequest", new VerifyResetTokenRequest());
        return "verify-reset-token";
    }

    // ========================================
    // FORGET PASSWORD FORMS
    // ========================================

    @PostMapping("/forgot-password-form")
    public String forgotPasswordForm(@ModelAttribute ForgotPasswordRequest forgotPasswordRequest,
                                     RedirectAttributes redirectAttributes) {
        log.info("Forgot password request for email: {}", forgotPasswordRequest.getEmail());

        try {
            boolean initiated = userService.initiatePasswordReset(forgotPasswordRequest.getEmail());
            if (initiated) {
                redirectAttributes.addFlashAttribute("success", "Password reset instructions have been sent to your email.");
                return "redirect:/login-page";
            }
        } catch (Exception e) {
            log.error("Forgot password failed: {}", e.getMessage());
            // For security, don't reveal if email exists or not
            redirectAttributes.addFlashAttribute("success", "If your email exists in our system, you will receive reset instructions.");
        }

        return "redirect:/login-page";
    }

    @PostMapping("/verify-reset-token-form")
    public String verifyResetTokenForm(@ModelAttribute VerifyResetTokenRequest verifyTokenRequest,
                                       RedirectAttributes redirectAttributes) {
        log.info("Verifying reset token: {}", verifyTokenRequest.getToken());

        try {
            boolean isValid = userService.validateResetToken(verifyTokenRequest.getToken());
            if (isValid) {
                return "redirect:/reset-password-page?token=" + verifyTokenRequest.getToken();
            }
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/verify-reset-token-page?error";
    }

    @PostMapping("/reset-password-form")
    public String resetPasswordForm(@ModelAttribute ResetPasswordRequest resetPasswordRequest,
                                    RedirectAttributes redirectAttributes) {
        log.info("Resetting password with token: {}", resetPasswordRequest.getToken());

        // Validate passwords match
        if (!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/reset-password-page?token=" + resetPasswordRequest.getToken() + "&error";
        }

        try {
            boolean reset = userService.resetPassword(resetPasswordRequest);
            if (reset) {
                redirectAttributes.addFlashAttribute("success", "Password has been reset successfully. Please login with your new password.");
                return "redirect:/login-page";
            }
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/reset-password-page?token=" + resetPasswordRequest.getToken() + "&error";
    }

    // ========================================
    // CHANGE PASSWORD (From Profile)
    // ========================================

    @PostMapping("/change-password-request")
    public String changePasswordRequest(@ModelAttribute ChangePasswordRequest changePasswordRequest,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("token") == null) {
            return "redirect:/login-page?error";
        }

        String email = (String) session.getAttribute("userEmail");

        // Validate passwords match
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match");
            return "redirect:/profile";
        }

        try {
            boolean initiated = userService.initiatePasswordChange(email, changePasswordRequest);
            if (initiated) {
                redirectAttributes.addFlashAttribute("successMessage", "Password change verification email sent. Please check your email to confirm the change.");
            }
        } catch (Exception e) {
            log.error("Password change request failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }

    // ========================================
    // CHANGE PASSWORD CONFIRMATION (Email Link)
    // ========================================

    @GetMapping("/confirm-password-change")
    public String confirmPasswordChange(@RequestParam("token") String token,
                                        Model model) {
        log.info("Confirming password change with token: {}", token);

        try {
            boolean confirmed = userService.confirmPasswordChange(token);
            if (confirmed) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Your password has been changed successfully. You can now login with your new password.");
                return "password-change-success";
            }
        } catch (Exception e) {
            log.error("Password change confirmation failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "password-change-error";
    }
}