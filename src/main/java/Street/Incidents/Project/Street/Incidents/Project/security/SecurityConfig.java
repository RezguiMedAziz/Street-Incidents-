package Street.Incidents.Project.Street.Incidents.Project.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ========================
                        // PUBLIC PAGES - NO AUTH REQUIRED
                        // ========================

                        // Public web pages
                        .requestMatchers("/", "/home", "/login-page", "/register-page").permitAll()

                        // Email Verification Pages
                        .requestMatchers(
                                "/verify-email-page",           // GET: Verification form page
                                "/verify-email",                // GET: Email link verification
                                "/resend-verification-page",    // GET: Resend verification page
                                "/verification-success",        // GET: Success page
                                "/verify-email-form",           // POST: Form submission
                                "/resend-verification-form"     // POST: Resend form submission
                        ).permitAll()

                        // ✅ API Endpoints (REST)
                        .requestMatchers(
                                "/api/auth/verify-email",       // GET/POST: Verify email API
                                "/api/auth/resend-verification" // POST: Resend verification API
                        ).permitAll()

                        // ========================
                        // FORGET PASSWORD ENDPOINTS - NO AUTH REQUIRED
                        // ========================
                        .requestMatchers(
                                "/forgot-password-page",        // GET: Forgot password page
                                "/forgot-password-form",        // POST: Submit forgot password
                                "/verify-reset-token-page",     // GET: Verify reset token page
                                "/verify-reset-token-form",     // POST: Submit token verification
                                "/reset-password-page",         // GET: Reset password page
                                "/reset-password-form",         // POST: Submit new password
                                "/confirm-password-change"      // GET: Confirm password change from email
                        ).permitAll()

                        // ========================
                        // PASSWORD CHANGE SUCCESS PAGES - NO AUTH REQUIRED
                        // ========================
                        .requestMatchers(
                                "/password-change-success",     // GET: Password change success page
                                "/password-change-error"        // GET: Password change error page
                        ).permitAll()

                        // ========================
                        // AUTHENTICATION ENDPOINTS
                        // ========================

                        // ✅ Web Pages
                        .requestMatchers(
                                "/",                            // GET: Home redirect
                                "/home",                        // GET: Home page
                                "/login-page",                  // GET: Login page
                                "/register-page",               // GET: Register page
                                "/logout",                      // GET/POST: Logout
                                "/perform-logout"               // Spring Security logout
                        ).permitAll()

                        // ✅ API Endpoints
                        .requestMatchers(
                                "/api/auth/login",              // POST: API login
                                "/api/auth/register",           // POST: API register
                                "/api/auth/login-form",         // POST: Web form login
                                "/api/auth/register-form",      // POST: Web form register
                                "/api/auth/test-token",         // GET: Test token
                                "/api/auth/**"                  // All other auth endpoints
                        ).permitAll()

                        // ========================
                        // STATIC RESOURCES
                        // ========================
                        .requestMatchers(
                                "/css/**",                      // CSS files
                                "/js/**",                       // JavaScript files
                                "/images/**",                   // Images
                                "/logo.png",                    // Logo file
                                "/**.png", "/**.jpg", "/**.jpeg", "/**.gif", "/**.svg", "/**.ico",
                                "/**.css", "/**.js",            // All static files
                                "/webjars/**",                  // WebJars
                                "/favicon.ico"                  // Favicon
                        ).permitAll()

                        // ========================
                        // DEVELOPMENT & DOCS
                        // ========================
                        .requestMatchers(
                                "/swagger-ui/**",               // Swagger UI
                                "/v3/api-docs/**",              // OpenAPI docs
                                "/swagger-resources/**",        // Swagger resources
                                "/swagger-ui.html",             // Swagger HTML
                                "/webjars/**"                   // WebJars for Swagger
                        ).permitAll()

                        // ========================
                        // DEBUG & TEST ENDPOINTS
                        // ========================
                        .requestMatchers(
                                "/debug-session",               // Debug session
                                "/debug-logout",                // Debug logout
                                "/debug-verification",          // Debug verification
                                "/test.html",                   // Test page
                                "/test-simple.html",            // Simple test
                                "/test-logout.html",            // Logout test
                                "/test-logout-simple.html",     // Simple logout test
                                "/test-session.html",           // Session test
                                "/test-session2.html",          // Session test 2
                                "/test-static.html",            // Static files test
                                "/test-auth"                    // Auth test
                        ).permitAll()

                        // ========================
                        // ERROR PAGES
                        // ========================
                        .requestMatchers(
                                "/error",                       // Error page
                                "/error/**"                     // All error pages
                        ).permitAll()

                        // ========================
                        // PROTECTED ENDPOINTS
                        // ========================
                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/perform-logout") // Use this URL for Spring Security logout
                        .logoutSuccessUrl("/login-page?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionFixation().migrateSession() // ✅ Important: Preserve session
                        .maximumSessions(1) // ✅ Allow only one session per user
                        .maxSessionsPreventsLogin(false)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}