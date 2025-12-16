package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data  // Lombok annotation to automatically generate getters and setters
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasse;  // Password field (motDePasse in French)

    @Enumerated(EnumType.STRING)
    private Role role;  // User role (admin, agent, etc.)

    @Builder.Default
    private boolean actif = false; // ✅ Change to false - user must verify email first

    @Column(name = "verification_code", length = 64)
    private String verificationCode;

    @Column(name = "verification_code_expiry")
    private LocalDateTime verificationCodeExpiry;

    @Builder.Default
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "declarant")
    private List<Incident> incidentsDeclarés;

    @OneToMany(mappedBy = "agent")
    private List<Incident> incidentsAssignés;

    @OneToMany(mappedBy = "utilisateur")
    private List<Notification> notifications;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return actif;
    }

    @Override
    public boolean isAccountNonLocked() {
        return actif;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return actif;
    }

    @Override
    public boolean isEnabled() {
        return actif && emailVerified; // ✅ User must be both active AND email verified
    }
    public void setPassword(String password) {
    this.motDePasse = password;
}

}
