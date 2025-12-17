package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.ResendVerificationRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.VerificationRequest;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final UserService userService;

    @GetMapping("/verify-email-page")
    public String verifyEmailPage(@RequestParam(value = "code", required = false) String code,
                                  Model model) {
        log.info("Accessing verify-email-page. Code: {}", code);

        if (code != null) {
            model.addAttribute("code", code);
        }

        model.addAttribute("verificationRequest", new VerificationRequest());
        return "verify-email";
    }

    @GetMapping("/resend-verification-page")
    public String resendVerificationPage(Model model) {
        log.info("Accessing resend-verification-page");
        model.addAttribute("resendRequest", new ResendVerificationRequest());
        return "resend-verification";
    }

    @GetMapping("/verification-success")
    public String verificationSuccessPage() {
        return "verification-success";
    }

    @PostMapping("/verify-email-form")
    public String verifyEmailForm(@ModelAttribute VerificationRequest verificationRequest) {
        log.info("Verification form submitted with code: {}", verificationRequest.getCode());

        try {
            boolean verified = userService.verifyEmail(verificationRequest.getCode());
            if (verified) {
                return "redirect:/login-page?verified";
            }
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
        }
        return "redirect:/verify-email-page?error";
    }

    @PostMapping("/resend-verification-form")
    public String resendVerificationForm(@ModelAttribute ResendVerificationRequest resendRequest) {
        log.info("Resend verification request for email: {}", resendRequest.getEmail());

        try {
            boolean resent = userService.resendVerificationEmail(resendRequest.getEmail());
            if (resent) {
                return "redirect:/login-page?resent";
            }
        } catch (Exception e) {
            log.error("Resend verification failed: {}", e.getMessage());
        }
        return "redirect:/resend-verification-page?error";
    }

    @GetMapping("/verify-email")
    public String verifyEmailFromLink(@RequestParam("code") String code, Model model) {
        log.info("Email link verification attempt with code: {}", code);

        try {
            boolean verified = userService.verifyEmail(code);
            if (verified) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Email verified successfully!");
                return "verification-success";
            } else {
                model.addAttribute("error", "Verification failed.");
                return "verify-email";
            }
        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("code", code);
            return "verify-email";
        }
    }
}