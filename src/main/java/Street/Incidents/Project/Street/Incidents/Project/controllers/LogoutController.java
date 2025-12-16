package Street.Incidents.Project.Street.Incidents.Project.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LogoutController {

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
}