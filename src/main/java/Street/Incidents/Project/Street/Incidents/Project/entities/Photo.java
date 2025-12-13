package Street.Incidents.Project.Street.Incidents.Project.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomFichier;
    private String type;
    private Long taille;
    private String cheminStockage;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;
}
