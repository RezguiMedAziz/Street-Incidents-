package Street.Incidents.Project.Street.Incidents.Project.entities;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
//import Street.Incidents.Project.Street.Incidents.Project.entities.converter.CryptoDoubleConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Departement categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default // Ajouter @Builder.Default pour les initialisations
    private StatutIncident statut = StatutIncident.SIGNALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default // Ajouter @Builder.Default pour les initialisations
    private Priorite priorite = Priorite.MOYENNE;

    @Column(nullable = false, updatable = false)
    @Builder.Default // Ajouter @Builder.Default pour les initialisations
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column
    private LocalDateTime dateResolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declarant_id")
    private User declarant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @Column
//    @Convert(converter = CryptoDoubleConverter.class)
    private Double latitude;

    @Column
//    @Convert(converter = CryptoDoubleConverter.class)
    private Double longitude;


    @Column(name = "commentaire_citoyen", columnDefinition = "TEXT")
    private String commentaireCitoyen;

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutIncident.SIGNALE;
        }
        if (this.priorite == null) {
            this.priorite = Priorite.MOYENNE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.statut == StatutIncident.RESOLU && this.dateResolution == null) {
            this.dateResolution = LocalDateTime.now();
        }
    }
}