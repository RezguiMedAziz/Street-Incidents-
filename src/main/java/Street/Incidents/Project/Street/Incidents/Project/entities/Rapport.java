package Street.Incidents.Project.Street.Incidents.Project.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "rapports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateGeneration;
    private String type;
    @Column(length = 10000)
    private String contenu;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User generePar;
}
