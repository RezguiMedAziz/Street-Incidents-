package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.converter.CryptoConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private Departement categorie;

    @Enumerated(EnumType.STRING)
    private StatutIncident statut;

    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;

    @Convert(converter = CryptoConverter.class)
    private String latitude;

    @Convert(converter = CryptoConverter.class)
    private String longitude;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite = Priorite.MOYENNE;


    @ManyToOne
    @JoinColumn(name = "declarant_id")
    private User declarant;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();

    // Méthode utilitaire pour ajouter une photo
    public void addPhoto(Photo photo) {
        photos.add(photo);
        photo.setIncident(this);
    }

}
