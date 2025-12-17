package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Trouver tous les incidents d'un déclarant par ID
    @Query("SELECT i FROM Incident i WHERE i.declarant.id = :declarantId")
    List<Incident> findByDeclarantId(@Param("declarantId") Long declarantId);

    // Trouver les incidents d'un déclarant par statut (par ID)
    @Query("SELECT i FROM Incident i WHERE i.declarant.id = :declarantId AND i.statut = :statut")
    List<Incident> findByDeclarantIdAndStatut(@Param("declarantId") Long declarantId, @Param("statut") StatutIncident statut);

    // Compter les incidents d'un déclarant par statut (par ID)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.declarant.id = :declarantId AND i.statut = :statut")
    long countByDeclarantIdAndStatut(@Param("declarantId") Long declarantId, @Param("statut") StatutIncident statut);

    // Trouver les incidents récents d'un déclarant (triés par date, par ID)
    @Query("SELECT i FROM Incident i WHERE i.declarant.id = :declarantId ORDER BY i.dateCreation DESC")
    List<Incident> findRecentByDeclarantId(@Param("declarantId") Long declarantId);

    // Trouver les N derniers incidents d'un déclarant (par ID)
    @Query("SELECT i FROM Incident i WHERE i.declarant.id = :declarantId ORDER BY i.dateCreation DESC")
    List<Incident> findTop5ByDeclarantIdOrderByDateCreationDesc(@Param("declarantId") Long declarantId);

    // Compter tous les incidents d'un déclarant (par ID)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.declarant.id = :declarantId")
    long countByDeclarantId(@Param("declarantId") Long declarantId);

    // Keep original methods for backward compatibility if needed
    List<Incident> findByDeclarant(User declarant);
    long countByDeclarantAndStatut(User declarant, StatutIncident statut);
    long countByDeclarant(User declarant);
}