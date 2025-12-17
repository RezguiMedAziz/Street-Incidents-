package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // ========== DECLARANT QUERIES ==========

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

    // ========== AGENT QUERIES (NEW) ==========

    // Trouver tous les incidents assignés à un agent
    @Query("SELECT i FROM Incident i WHERE i.agent.id = :agentId ORDER BY i.dateCreation DESC")
    List<Incident> findByAgentId(@Param("agentId") Long agentId);

    // Trouver les incidents d'un agent par statut
    @Query("SELECT i FROM Incident i WHERE i.agent.id = :agentId AND i.statut = :statut ORDER BY i.dateCreation DESC")
    List<Incident> findByAgentIdAndStatut(@Param("agentId") Long agentId, @Param("statut") StatutIncident statut);

    // Compter les incidents d'un agent par statut
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.agent.id = :agentId AND i.statut = :statut")
    long countByAgentIdAndStatut(@Param("agentId") Long agentId, @Param("statut") StatutIncident statut);

    // Compter tous les incidents assignés à un agent
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.agent.id = :agentId")
    long countByAgentId(@Param("agentId") Long agentId);

    // Trouver les incidents critiques d'un agent
    @Query("SELECT i FROM Incident i WHERE i.agent.id = :agentId AND i.priorite = 'CRITIQUE' ORDER BY i.dateCreation DESC")
    List<Incident> findCriticalByAgentId(@Param("agentId") Long agentId);

    // Trouver les incidents actifs d'un agent (non résolus/clôturés)
    @Query("SELECT i FROM Incident i WHERE i.agent.id = :agentId AND i.statut NOT IN ('RESOLU', 'CLOTURE') ORDER BY i.dateCreation DESC")
    List<Incident> findActiveByAgentId(@Param("agentId") Long agentId);

    // ========== BACKWARD COMPATIBILITY ==========

    List<Incident> findByDeclarant(User declarant);
    long countByDeclarantAndStatut(User declarant, StatutIncident statut);
    long countByDeclarant(User declarant);
}