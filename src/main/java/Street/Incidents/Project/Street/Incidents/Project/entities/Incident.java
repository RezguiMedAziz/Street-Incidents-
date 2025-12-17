package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.CategorieIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String description;

    @Enumerated(EnumType.STRING)
    private CategorieIncident categorie;

    @Enumerated(EnumType.STRING)
    private StatutIncident statut;

    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;

    private Double latitude;
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "declarant_id")
    private User declarant;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @OneToMany(mappedBy = "incident")
    private List<Photo> photos;
}
