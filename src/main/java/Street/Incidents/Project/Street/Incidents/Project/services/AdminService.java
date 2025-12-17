package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Get paginated users from the database
     */
    public Page<User> getUsersPage(Pageable pageable) {
        log.info("Fetching users page: {}", pageable);
        return userRepository.findAll(pageable);
    }

    /**
     * Get users filtered by role with pagination
     */
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        log.info("Fetching users with role: {} and pagination: {}", role, pageable);
        return userRepository.findByRole(role, pageable);
    }

    /**
     * Get users filtered by role as List (not paginated)
     * ✅ NEW: For dropdown selections
     */
    public List<User> getUsersByRoleList(Role role) {
        log.info("Fetching all users with role: {}", role);
        return userRepository.findByRole(role);
    }

    /**
     * Get all users from the database (for non-paginated use)
     */
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Create a new user with specified role and send credentials email
     */
    public User createUser(String email, String password, Role role, String nom, String prenom) {
        log.info("Creating user with email: {} and role: {}", email, role);

        if (userRepository.existsByEmail(email)) {
            log.warn("User creation failed - email already exists: {}", email);
            throw new IllegalArgumentException("User with this email already exists.");
        }

        // Validate that password is provided
        if (password == null || password.isEmpty()) {
            log.warn("User creation failed - password is required");
            throw new IllegalArgumentException("Password is required.");
        }

        // Validate password strength
        if (password.length() < 6) {
            log.warn("User creation failed - password must be at least 6 characters");
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User user = User.builder()
                .email(email)
                .nom(nom)
                .prenom(prenom)
                .motDePasse(passwordEncoder.encode(password))
                .role(role)
                .actif(true)  // Admin-created users are active by default
                .emailVerified(true)  // Admin-created users don't need verification
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {} and role: {}", savedUser.getId(), role);

        // Send account credentials email
        try {
            String fullName = (prenom != null && !prenom.isEmpty() ? prenom + " " : "") +
                    (nom != null && !nom.isEmpty() ? nom : "User");

            emailService.sendAccountCredentials(
                    email,
                    fullName,
                    password,  // Send the actual password typed by admin
                    role.name()
            );

            log.info("Account credentials email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", email, e.getMessage());
            // Don't throw - user is created, just log the error
        }

        return savedUser;
    }

    /**
     * Create a new user with minimal info (backward compatibility)
     */
    public User createUser(String email, String password, Role role) {
        return createUser(email, password, role, null, null);
    }

    /**
     * Create a new admin user
     */
    public User createAdmin(String email, String password) {
        return createUser(email, password, Role.ADMINISTRATEUR);
    }

    /**
     * Create a new agent user
     */
    public User createAgent(String email, String password) {
        return createUser(email, password, Role.AGENT_MUNICIPAL);
    }

    /**
     * Create a new citoyen user
     */
    public User createCitoyen(String email, String password) {
        return createUser(email, password, Role.CITOYEN);
    }

    /**
     * Change the role of an existing user
     */
    public User changeUserRole(Long userId, Role newRole) {
        log.info("Changing role for user ID: {} to {}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found.");
                });

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        log.info("User role updated successfully for user ID: {}", userId);
        return updatedUser;
    }

    /**
     * Update user information
     */
    public User updateUser(Long userId, String nom, String prenom, String email, Role role) {
        log.info("Updating user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found.");
                });

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            log.warn("Email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists.");
        }

        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", userId);

        return updatedUser;
    }

    /**
     * Update user information with optional password change and email notification
     */
    public User updateUserWithNotification(Long userId, String nom, String prenom, String email,
                                           Role role, String newPassword, boolean sendNotification) {
        log.info("Updating user ID: {} with notification option", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found.");
                });

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            log.warn("Email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists.");
        }

        boolean emailChanged = !user.getEmail().equals(email);
        boolean passwordChanged = newPassword != null && !newPassword.isEmpty();
        boolean roleChanged = user.getRole() != role;

        // Update user fields
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setRole(role);

        // Update password if provided
        if (passwordChanged) {
            // Validate new password
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long.");
            }
            user.setMotDePasse(passwordEncoder.encode(newPassword));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", userId);

        // Send notification email if requested
        if (sendNotification && (emailChanged || passwordChanged || roleChanged)) {
            try {
                String fullName = (prenom != null && !prenom.isEmpty() ? prenom + " " : "") +
                        (nom != null && !nom.isEmpty() ? nom : "User");

                emailService.sendAccountUpdateNotification(
                        email,
                        fullName,
                        email,
                        passwordChanged ? newPassword : null,
                        role.name(),
                        emailChanged,
                        passwordChanged,
                        roleChanged
                );

                log.info("Account update notification email sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send update notification email to {}: {}", email, e.getMessage());
            }
        }

        return updatedUser;
    }

    /**
     * Get a user by ID
     */
    public Optional<User> getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);
        return userRepository.findById(userId);
    }

    /**
     * Check if a user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Delete a user by ID
     */
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Cannot delete - user not found with ID: {}", userId);
            throw new IllegalArgumentException("User not found.");
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully with ID: {}", userId);
    }
}
