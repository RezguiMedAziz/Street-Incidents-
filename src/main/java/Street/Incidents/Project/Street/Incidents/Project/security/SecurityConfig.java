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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.ui.Model;

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
                // Protection CSRF activée
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Désactiver CSRF pour les routes d'export (optionnel, mais recommandé pour les téléchargements)
                        .ignoringRequestMatchers("/citizen/export/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // Public web pages and routes
                        // ========================
                        // PUBLIC PAGES - NO AUTH REQUIRED
                        // ========================

                        // Public web pages
                        .requestMatchers("/", "/home", "/login-page", "/register-page").permitAll()

                        // Email Verification Pages
                        .requestMatchers(
                                "/verify-email-page",
                                "/verify-email",
                                "/resend-verification-page",
                                "/verification-success",
                                "/verify-email-form",
                                "/resend-verification-form"
                        ).permitAll()

                        // Public API Endpoints (authentication and registration)
                        .requestMatchers(
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification"
                        ).permitAll()

                        // ========================
                        // FORGET PASSWORD ENDPOINTS - NO AUTH REQUIRED
                        // ========================
                        .requestMatchers(
                                "/forgot-password-page",
                                "/forgot-password-form",
                                "/verify-reset-token-page",
                                "/verify-reset-token-form",
                                "/reset-password-page",
                                "/reset-password-form",
                                "/confirm-password-change"
                        ).permitAll()

                        // ========================
                        // PASSWORD CHANGE SUCCESS PAGES - NO AUTH REQUIRED
                        // ========================
                        .requestMatchers(
                                "/password-change-success",
                                "/password-change-error"
                        ).permitAll()

                        // ========================
                        // WEB PAGES
                        // ========================
                        .requestMatchers(
                                "/",
                                "/layouts/main",
                                "/login-page",
                                "/register-page",
                                "/logout",
                                "/perform-logout"
                        ).permitAll()

                        // ========================
                        // API ENDPOINTS
                        // ========================
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/login-form",
                                "/api/auth/register-form",
                                "/api/auth/test-token",
                                "/api/auth/**"
                        ).permitAll()

                        // ========================
                        // STATIC RESOURCES
                        // ========================
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/logo.png",
                                "/**.png", "/**.jpg", "/**.jpeg", "/**.gif", "/**.svg", "/**.ico",
                                "/**.css", "/**.js",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ========================
                        // DEVELOPMENT & DOCS
                        // ========================
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()

                        // ========================
                        // ERROR PAGES
                        // ========================
                        .requestMatchers(
                                "/error",
                                "/error/**"
                        ).permitAll()

                        // ========================
                        // PROTECTED ENDPOINTS
                        // ========================

                        // Admin routes
                        .requestMatchers("/admin/**").hasRole("ADMINISTRATEUR")

                        // Export routes for citizens (plus spécifique en premier)
                        .requestMatchers("/citizen/export/**").hasRole("CITOYEN")

                        // Citizen routes
                        .requestMatchers("/citizen/**").hasRole("CITOYEN")

                        // Agent routes
                        .requestMatchers("/agent/**").hasRole("AGENT_MUNICIPAL")

                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login-page")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/login-page?error=true")
                )
                .logout(logout -> logout
                        .logoutUrl("/perform-logout")
                        .logoutSuccessUrl("/login-page?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
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