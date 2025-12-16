package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Quartier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuartierRepository extends JpaRepository<Quartier, Long> {

    /**
     * Trouve un quartier par gouvernorat et municipalite
     * Permet d'éviter les doublons lors de la création d'incidents
     */
    Optional<Quartier> findByGouvernoratAndMunicipalite(
            String gouvernorat,
            String municipalite
    );
}
