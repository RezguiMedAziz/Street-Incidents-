package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.LoginRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.RegisterRequest;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;
import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthPageController {

    private final UserService userService;

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
}