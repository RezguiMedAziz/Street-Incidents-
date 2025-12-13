package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    private TypeNotification type;

    @Builder.Default
    private boolean lu = false;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;
}
