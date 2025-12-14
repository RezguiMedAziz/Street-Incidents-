package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.LoginRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.RegisterRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.ResendVerificationRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.VerificationRequest;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final UserService userService;

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
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Session exists: {}", session != null);

        if (session != null) {
            log.info("Session ID: {}", session.getId());
            log.info("Session creation time: {}", new Date(session.getCreationTime()));

            log.info("=== SESSION ATTRIBUTES ===");
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String name = attrNames.nextElement();
                log.info("  {} = {}", name, session.getAttribute(name));
            }

            // Check Spring Security context
            SecurityContext securityContext = SecurityContextHolder.getContext();
            log.info("SecurityContext Authentication: {}",
                    securityContext.getAuthentication() != null ?
                            securityContext.getAuthentication().getName() : "null");
        }

        if (session == null || session.getAttribute("token") == null) {
            log.warn("No session or token found, redirecting to login");
            return "redirect:/login-page";
        }

        // Add user info to model from session
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", session.getAttribute("userRole"));

        log.info("Dashboard access granted for: {}", session.getAttribute("userEmail"));
        return "dashboard";

    }

    @GetMapping("/login-page")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        log.info("Login page accessed. Error: {}, Logout: {}", error, logout);

        if (error != null) {
            model.addAttribute("error", "Invalid email or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }


    @GetMapping("/register-page")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }


    @PostMapping("/api/auth/login-form")
    public String loginForm(@ModelAttribute LoginRequest loginRequest,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes redirectAttributes) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            var authResponse = userService.login(loginRequest);
            log.info("Login successful for: {}", loginRequest.getEmail());

            // Get or create session
            HttpSession session = request.getSession();
            log.info("Session created with ID: {}", session.getId());

            // Store user data in session
            session.setAttribute("token", authResponse.getToken());
            session.setAttribute("userEmail", authResponse.getEmail());
            session.setAttribute("userName", authResponse.getNom() + " " + authResponse.getPrenom());
            session.setAttribute("userRole", authResponse.getRole());

            // ✅ CRITICAL: Also store authentication in Spring Security context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authResponse.getEmail(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + authResponse.getRole()))
                    );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Store Spring Security context in session
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            // Set session timeout
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            log.info("Session attributes saved for user: {}", authResponse.getEmail());
            log.info("All session attributes:");
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String name = attrNames.nextElement();
                log.info("  - {}: {}", name, session.getAttribute(name));
            }

            return "redirect:/dashboard";

        } catch (Exception e) {
            log.error("Login failed for {}: {}", loginRequest.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            return "redirect:/login-page?error=true";
        }
    }

    @PostMapping("/api/auth/register-form")
    public String registerForm(@ModelAttribute RegisterRequest registerRequest,
                               RedirectAttributes redirectAttributes) {
        log.info("Registration attempt for: {}", registerRequest.getEmail());

        try {
            var authResponse = userService.register(registerRequest);
            log.info("Registration successful for: {}", registerRequest.getEmail());

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please login with your credentials.");
            return "redirect:/login-page";

        } catch (Exception e) {
            log.error("Registration failed for {}: {}", registerRequest.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Registration failed: " + e.getMessage());
            return "redirect:/register-page";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {

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

            // Invalidate the session
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

        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully.");
        return "redirect:/login-page?logout=true";
    }

    @GetMapping("/verify-email-page")
    public String verifyEmailPage(@RequestParam(value = "code", required = false) String code,
                                  @RequestParam(value = "error", required = false) String error,
                                  @RequestParam(value = "success", required = false) String success,
                                  Model model) {
        log.info("Accessing verify-email-page. Code: {}, Error: {}, Success: {}",
                code, error, success);

        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (code != null) {
            model.addAttribute("code", code);
        }

        // Create empty LoginRequest for the form
        model.addAttribute("verificationRequest", new VerificationRequest());

        return "verify-email";
    }

    // ✅ Verification form submission (POST)
    @PostMapping("/verify-email-form")
    public String verifyEmailForm(@ModelAttribute
                                      VerificationRequest verificationRequest,
                                  RedirectAttributes redirectAttributes) {
        log.info("Verification form submitted with code: {}",
                verificationRequest.getCode());

        try {
            boolean verified = userService.verifyEmail(verificationRequest.getCode());
            if (verified) {
                redirectAttributes.addFlashAttribute("success",
                        "🎉 Email verified successfully! You can now log in.");
                return "redirect:/login-page";
            }
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/verify-email-page?error=true";
    }

    // ✅ Email link verification (GET - from email link)
    @GetMapping("/verify-email")
    public String verifyEmailFromLink(@RequestParam("code") String code,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
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
            model.addAttribute("code", code); // Preserve code for retry
            return "verify-email";
        }
    }

    // ✅ Resend verification page
    @GetMapping("/resend-verification-page")
    public String resendVerificationPage(Model model) {
        log.info("Accessing resend-verification-page");
        model.addAttribute("resendRequest", new ResendVerificationRequest());
        return "resend-verification";
    }

    // ✅ Resend verification form submission
    @PostMapping("/resend-verification-form")
    public String resendVerificationForm(@ModelAttribute ResendVerificationRequest resendRequest,
                                         RedirectAttributes redirectAttributes) {
        log.info("Resend verification request for email: {}", resendRequest.getEmail());

        try {
            boolean resent = userService.resendVerificationEmail(resendRequest.getEmail());
            if (resent) {
                redirectAttributes.addFlashAttribute("success",
                        "📧 Verification email resent! Please check your inbox.");
                return "redirect:/login-page";
            }
        } catch (Exception e) {
            log.error("Resend verification failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/resend-verification-page?error=true";
    }

    // ✅ Verification success page
    @GetMapping("/verification-success")
    public String verificationSuccessPage(Model model) {
        return "verification-success";
    }
}