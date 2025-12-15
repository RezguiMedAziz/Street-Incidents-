package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.*;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Registration failed - email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already exists");
        }

        // Generate verification code
        String verificationCode = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(request.getRole())
                .actif(true) // User is active but email not verified
                .verificationCode(verificationCode)
                .verificationCodeExpiry(expiryDate)
                .emailVerified(false)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", request.getEmail());

        // Send verification email asynchronously
        try {
            String fullName = request.getPrenom() + " " + request.getNom();
            emailService.sendVerificationEmail(request.getEmail(), fullName, verificationCode);
            log.info("Verification email sent to: {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email, but user created: {}", e.getMessage());
            // Don't throw - user is created, they can request verification email later
        }

        // Don't generate JWT token yet - user needs to verify email first
        return AuthResponse.builder()
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole())
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    @Transactional
    public boolean verifyEmail(String verificationCode) {
        log.info("Attempting to verify email with code: {}", verificationCode);
        User user = userRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> {
                    log.error("Invalid verification code: {}", verificationCode);
                    return new RuntimeException("Invalid verification code");
                });


        // Check if code is expired
        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            log.error("Verification code expired for user: {}", user.getEmail());
            throw new RuntimeException("Verification code has expired");
        }

        // Verify email
        user.setEmailVerified(true);
        user.setVerificationCode(null); // Clear verification code
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());

        // Send welcome email
        try {
            String fullName = user.getPrenom() + " " + user.getNom();
            emailService.sendWelcomeEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }

        return true;
    }

    @Transactional
    public boolean resendVerificationEmail(String email) {
        log.info("Resending verification email to: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        if (user.isEmailVerified()) {
            log.error("Email already verified for: {}", email);
            throw new RuntimeException("Email already verified");
        }

        // Generate new verification code
        String newVerificationCode = UUID.randomUUID().toString();
        LocalDateTime newExpiryDate = LocalDateTime.now().plusHours(24);

        user.setVerificationCode(newVerificationCode);
        user.setVerificationCodeExpiry(newExpiryDate);
        userRepository.save(user);

        // Send verification email
        String fullName = user.getPrenom() + " " + user.getNom();
        emailService.sendVerificationEmail(email, fullName, newVerificationCode);

        log.info("Verification email resent to: {}", email);
        return true;
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            // First, check if user exists
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", request.getEmail());
                        return new RuntimeException("User not found");
                    });

            log.info("User found: {}, active: {}, email verified: {}",
                    request.getEmail(), user.isActif(), user.isEmailVerified());

            // Check if email is verified
            if (!user.isEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", request.getEmail());
                throw new RuntimeException("Please verify your email first. Check your inbox or request a new verification email.");
            }

            // Check if account is active
            if (!user.isActif()) {
                log.warn("Login attempt with inactive account: {}", request.getEmail());
                throw new RuntimeException("Account is not active. Please contact support.");
            }

            // Try to authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()
                    )
            );

            log.info("Authentication successful for: {}", request.getEmail());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User authenticatedUser = (User) authentication.getPrincipal();

            String token = jwtUtil.generateToken(authenticatedUser);
            log.info("JWT token generated for: {}", request.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .email(authenticatedUser.getEmail())
                    .nom(authenticatedUser.getNom())
                    .prenom(authenticatedUser.getPrenom())
                    .role(authenticatedUser.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        } catch (AuthenticationException e) {
            log.error("Authentication failed for {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during login for {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    //  User update and consult

    public User getUserByEmail(String email) {
        // Implementation to get user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return (user);
    }

    public void updateUserProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        if (request.getNom() != null && !request.getNom().isEmpty()) {
            user.setNom(request.getNom());
        }
        if (request.getPrenom() != null && !request.getPrenom().isEmpty()) {
            user.setPrenom(request.getPrenom());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(email)) {
            // Check if new email is not taken
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);
    }

    // ========================================
    // FORGET PASSWORD METHODS
    // ========================================

    @Transactional
    public boolean initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found for password reset: {}", email);
                    return new RuntimeException("If this email exists, a reset link will be sent");
                });

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(2); // Token valid for 2 hours

        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(tokenExpiry);
        userRepository.save(user);

        // Send reset email
        try {
            String fullName = user.getPrenom() + " " + user.getNom();
            emailService.sendPasswordResetEmail(email, fullName, resetToken);
            log.info("Password reset email sent to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            throw new RuntimeException("Failed to send reset email");
        }
    }

    @Transactional
    public boolean validateResetToken(String token) {
        log.info("Validating reset token: {}", token);

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid reset token: {}", token);
                    return new RuntimeException("Invalid or expired reset token");
                });

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("Reset token expired for user: {}", user.getEmail());
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Reset token has expired");
        }

        return true;
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password with token: {}", request.getToken());

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> {
                    log.error("Invalid reset token: {}", request.getToken());
                    return new RuntimeException("Invalid or expired reset token");
                });

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("Reset token expired for user: {}", user.getEmail());
            throw new RuntimeException("Reset token has expired");
        }

        // Update password
        user.setMotDePasse(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successful for user: {}", user.getEmail());

        // Send confirmation email
        try {
            String fullName = user.getPrenom() + " " + user.getNom();
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email: {}", e.getMessage());
        }

        return true;
    }

    // ========================================
    // CHANGE PASSWORD METHODS
    // ========================================

    @Transactional
    public boolean initiatePasswordChange(String email, ChangePasswordRequest request) {
        log.info("Initiating password change for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getMotDePasse())) {
            log.error("Current password incorrect for user: {}", email);
            throw new RuntimeException("Current password is incorrect");
        }

        // Generate change token
        String changeToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(2);

        user.setPasswordChangeToken(changeToken);
        user.setPasswordChangeTokenExpiry(tokenExpiry);
        user.setTemporaryNewPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Send confirmation email
        try {
            String fullName = user.getPrenom() + " " + user.getNom();
            emailService.sendPasswordChangeVerificationEmail(email, fullName, changeToken);
            log.info("Password change verification email sent to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password change verification email: {}", e.getMessage());
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Transactional
    public boolean confirmPasswordChange(String token) {
        log.info("Confirming password change with token: {}", token);

        User user = userRepository.findByPasswordChangeToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid password change token: {}", token);
                    return new RuntimeException("Invalid or expired token");
                });

        // Check if token is expired
        if (user.getPasswordChangeTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("Password change token expired for user: {}", user.getEmail());
            user.setPasswordChangeToken(null);
            user.setPasswordChangeTokenExpiry(null);
            user.setTemporaryNewPassword(null);
            userRepository.save(user);
            throw new RuntimeException("Verification token has expired");
        }

        // Update password with the temporary one
        user.setMotDePasse(user.getTemporaryNewPassword());
        user.setPasswordChangeToken(null);
        user.setPasswordChangeTokenExpiry(null);
        user.setTemporaryNewPassword(null);
        userRepository.save(user);

        log.info("Password change confirmed successfully for user: {}", user.getEmail());

        // Send confirmation email
        try {
            String fullName = user.getPrenom() + " " + user.getNom();
            emailService.sendPasswordChangeCompletedEmail(user.getEmail(), fullName);
        } catch (Exception e) {
            log.error("Failed to send password change completion email: {}", e.getMessage());
        }

        return true;
    }
}