package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);
 Page<User> findByRole(Role role, Pageable pageable);
}
