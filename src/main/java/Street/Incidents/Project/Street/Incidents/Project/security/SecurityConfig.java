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
                        // Public web pages and routes
                        .requestMatchers("/", "/home", "/login-page", "/register-page").permitAll()
                        .requestMatchers(
                                "/verify-email-page",           // GET: Verification form page
                                "/verify-email",                // GET: Email link verification
                                "/resend-verification-page",    // GET: Resend verification page
                                "/verification-success",        // GET: Success page
                                "/verify-email-form",           // POST: Form submission
                                "/resend-verification-form"     // POST: Resend form submission
                        ).permitAll()

                        // Public API Endpoints (authentication and registration)
                        .requestMatchers(
                                "/api/auth/verify-email",       // GET/POST: Verify email API
                                "/api/auth/resend-verification" // POST: Resend verification API
                        ).permitAll()

                        // ========================
                        // AUTHENTICATION ENDPOINTS
                        // ========================
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
                        .requestMatchers("/admin/**").hasRole("ADMINISTRATEUR")  // Admin routes secured
                        .anyRequest().authenticated()  // Any other request needs authentication
                )
                .formLogin(form -> form
                        .loginPage("/login-page")  // Custom login page
                        .defaultSuccessUrl("/admin/dashboard", true)  // Redirect to the admin dashboard after login
                        .failureUrl("/login-page?error=true")  // Optional: Redirect on failure
                )
                .logout(logout -> logout
                        .logoutUrl("/perform-logout") // URL for Spring Security logout
                        .logoutSuccessUrl("/login-page?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionFixation().migrateSession() // Preserve session on login
                        .maximumSessions(1) // Only one session per user
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
