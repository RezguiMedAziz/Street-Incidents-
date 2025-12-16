package Street.Incidents.Project.Street.Incidents.Project.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "quartiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quartier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "quartier")
    private List<Incident> incidents;

    private String gouvernorat;
    private String municipalite;

}
