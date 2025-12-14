package Street.Incidents.Project.Street.Incidents.Project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class SessionContextFilter extends OncePerRequestFilter {

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String requestURI = request.getRequestURI();

        log.debug("SessionContextFilter - URI: {}, Session: {}",
                requestURI, session != null ? session.getId() : "null");

        // Skip for static resources and login/register pages
        if (shouldSkip(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // For dashboard and protected pages, check if we have session context
        if (session != null && requestURI.startsWith("/dashboard")) {
            SecurityContext context = (SecurityContext) session.getAttribute(
                    "SPRING_SECURITY_CONTEXT");

            if (context != null) {
                SecurityContextHolder.setContext(context);
                log.debug("Restored SecurityContext from session: {}",
                        context.getAuthentication().getName());
            } else {
                log.warn("No SecurityContext found in session for dashboard access");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String requestURI) {
        return requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/images/") ||
                requestURI.startsWith("/webjars/") ||
                requestURI.startsWith("/login-page") ||
                requestURI.startsWith("/register-page") ||
                requestURI.startsWith("/api/auth/");
    }
}