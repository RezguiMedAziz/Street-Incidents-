package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 * Provides CRUD operations and custom query methods
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find user by verification code
     * @param verificationCode The verification code
     * @return Optional containing the user if found
     */
    Optional<User> findByVerificationCode(String verificationCode);

    /**
     * Find user by password reset token
     * @param token The password reset token
     * @return Optional containing the user if found
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Find user by password change token
     * @param token The password change token
     * @return Optional containing the user if found
     */
    Optional<User> findByPasswordChangeToken(String token);

    /**
     * Find users by role with pagination
     * @param role The role to filter by
     * @param pageable Pagination information
     * @return Page of users with the specified role
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Find users by role as List (not paginated)
     * ✅ NEW: Used for dropdown selections in admin panel
     * @param role The role to filter by
     * @return List of users with the specified role
     */
    List<User> findByRole(Role role);

    /**
     * Find all users with roles in the given list
     * Used to fetch all agents (AGENT_MUNICIPAL and ADMINISTRATEUR)
     * @param roles List of roles to search for
     * @return List of users with any of the specified roles
     */
    List<User> findByRoleIn(List<Role> roles);
}
