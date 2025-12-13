package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    private boolean actif = true;

    @OneToMany(mappedBy = "declarant")
    private List<Incident> incidentsDeclarés;

    @OneToMany(mappedBy = "agent")
    private List<Incident> incidentsAssignés;

    @OneToMany(mappedBy = "utilisateur")
    private List<Notification> notifications;
}
