package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.*;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final UserService userService;

    // ========================================
    // HOME & DASHBOARD ROUTES
    // ========================================

    @GetMapping("/")
    public String home() {
        return "redirect:/login-page";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        log.info("=== DASHBOARD ACCESS ATTEMPT ===");

        if (session == null || session.getAttribute("token") == null) {
            log.warn("No session or token found, redirecting to login");
            return "redirect:/login-page?error";
        }

        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", session.getAttribute("userRole"));

        // Add active page for sidebar highlighting
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard - Street Incidents");

        // Return the layout with dashboard content
        return "dashboard";
    }

    // ========================================
    // AUTHENTICATION PAGES (GET)
    // ========================================

    @GetMapping("/login-page")
    public String loginPage(Model model) {
        log.info("Login page accessed");
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/register-page")
    public String registerPage(Model model) {
        log.info("Register page accessed");
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    // ========================================
    // AUTHENTICATION FORMS (POST)
    // ========================================

    @PostMapping("/api/auth/login-form")
    public String loginForm(@ModelAttribute LoginRequest loginRequest,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            var authResponse = userService.login(loginRequest);
            log.info("Login successful for: {}", loginRequest.getEmail());

            HttpSession session = request.getSession();
            log.info("Session created with ID: {}", session.getId());

            // Store user data in session
            session.setAttribute("token", authResponse.getToken());
            session.setAttribute("userEmail", authResponse.getEmail());
            session.setAttribute("userName", authResponse.getNom() + " " + authResponse.getPrenom());
            session.setAttribute("userRole", authResponse.getRole());

            // Store authentication in Spring Security context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authResponse.getEmail(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + authResponse.getRole()))
                    );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            // Set session timeout (30 minutes)
            session.setMaxInactiveInterval(30 * 60);

            log.info("Session attributes saved for user: {}", authResponse.getEmail());
            log.info("All session attributes:");
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String name = attrNames.nextElement();
                log.info("  - {}: {}", name, session.getAttribute(name));
            }

            return "redirect:/dashboard?loginsuccess";

        } catch (Exception e) {
            log.error("Login failed for {}: {}", loginRequest.getEmail(), e.getMessage());
            return "redirect:/login-page?error";
        }
    }

    @PostMapping("/api/auth/register-form")
    public String registerForm(@ModelAttribute RegisterRequest registerRequest) {
        log.info("Registration attempt for: {}", registerRequest.getEmail());

        try {
            var authResponse = userService.register(registerRequest);
            log.info("Registration successful for: {}", registerRequest.getEmail());
            return "redirect:/login-page?registered";

        } catch (Exception e) {
            log.error("Registration failed for {}: {}", registerRequest.getEmail(), e.getMessage());
            return "redirect:/register-page?error";
        }
    }

    // ========================================
    // LOGOUT
    // ========================================

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            String userEmail = (String) session.getAttribute("userEmail");
            log.info("Logging out user: {}", userEmail);

            // Clear all session attributes
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                session.removeAttribute(attrName);
            }

            session.invalidate();
            log.info("Session invalidated for user: {}", userEmail);
        }

        // Clear Spring Security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Clearing Spring Security authentication for: {}", auth.getName());
            SecurityContextHolder.clearContext();
        }

        // Clear JSESSIONID cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setValue(null);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    log.info("JSESSIONID cookie cleared");
                    break;
                }
            }
        }

        return "redirect:/login-page?logout";
    }

    // ========================================
    // EMAIL VERIFICATION PAGES (GET)
    // ========================================

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

    // ========================================
    // EMAIL VERIFICATION FORMS (POST)
    // ========================================

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

    // ========================================
    // EMAIL LINK VERIFICATION (GET)
    // ========================================

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
    @GetMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        log.info("=== PROFILE PAGE ACCESS ATTEMPT ===");

        if (session == null || session.getAttribute("token") == null) {
            log.warn("No session or token found, redirecting to login");
            return "redirect:/login-page?error";
        }

        String email = (String) session.getAttribute("userEmail");

        try {
            // Get user details
            var user = userService.getUserByEmail(email);
            model.addAttribute("userDetails", user);

            // Add profile update request for form
            model.addAttribute("profileUpdateRequest", new ProfileUpdateRequest());
        } catch (Exception e) {
            log.error("Error getting user details: {}", e.getMessage());
            model.addAttribute("error", "Unable to load profile details");
        }

        // Set active page for sidebar highlighting
        model.addAttribute("activePage", "profile");
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("pageTitle", "Profile - Street Incidents");

        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest profileUpdateRequest,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("token") == null) {
            return "redirect:/login-page?error";
        }

        String currentEmail = (String) session.getAttribute("userEmail");

        try {
            // Update user profile
            userService.updateUserProfile(currentEmail, profileUpdateRequest);

            // Update session attributes if email changed
            if (profileUpdateRequest.getEmail() != null &&
                    !profileUpdateRequest.getEmail().isEmpty() &&
                    !profileUpdateRequest.getEmail().equals(currentEmail)) {

                session.setAttribute("userEmail", profileUpdateRequest.getEmail());
            }

            // Update name in session
            String newFullName = profileUpdateRequest.getNom() + " " + profileUpdateRequest.getPrenom();
            session.setAttribute("userName", newFullName);

            log.info("Profile updated successfully for user: {}", currentEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");

        } catch (Exception e) {
            log.error("Profile update failed for {}: {}", currentEmail, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}