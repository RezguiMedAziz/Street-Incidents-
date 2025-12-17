package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Incident entity
 * Provides CRUD operations and custom query methods with advanced filtering
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    // ========================================
    // BASIC QUERY METHODS
    // ========================================

    /**
     * Find incidents by status with pagination
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of incidents matching the status
     */
    Page<Incident> findByStatut(StatutIncident status, Pageable pageable);

    /**
     * Find incidents assigned to a specific agent with pagination
     * @param agentId The ID of the agent
     * @param pageable Pagination information
     * @return Page of incidents assigned to the agent
     */
    Page<Incident> findByAgentId(Long agentId, Pageable pageable);

    /**
     * Find incidents that have no assigned agent with pagination
     * @param pageable Pagination information
     * @return Page of unassigned incidents
     */
    Page<Incident> findByAgentIsNull(Pageable pageable);

    /**
     * Count incidents by status
     * @param status The status to count
     * @return Number of incidents with the given status
     */
    long countByStatut(StatutIncident status);

    /**
     * Count incidents that have no assigned agent
     * @return Number of unassigned incidents
     */
    long countByAgentIsNull();

    // ========================================
    // ✅ ADVANCED FILTERING METHODS
    // ========================================

    /**
     * Find incidents by department (categorie) with pagination
     * @param categorie The department to filter by
     * @param pageable Pagination information
     * @return Page of incidents matching the department
     */
    Page<Incident> findByCategorie(Departement categorie, Pageable pageable);

    /**
     * Find incidents by gouvernorat (via Quartier relationship)
     * @param gouvernorat The gouvernorat name
     * @param pageable Pagination information
     * @return Page of incidents in the specified gouvernorat
     */
    @Query("SELECT i FROM Incident i WHERE i.quartier.gouvernorat = :gouvernorat")
    Page<Incident> findByGouvernorat(@Param("gouvernorat") String gouvernorat, Pageable pageable);

    /**
     * Find incidents by municipalite (via Quartier relationship)
     * @param municipalite The municipalite name
     * @param pageable Pagination information
     * @return Page of incidents in the specified municipalite
     */
    @Query("SELECT i FROM Incident i WHERE i.quartier.municipalite = :municipalite")
    Page<Incident> findByMunicipalite(@Param("municipalite") String municipalite, Pageable pageable);

    /**
     * Find incidents within a date range
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param pageable Pagination information
     * @return Page of incidents created within the date range
     */
    @Query("SELECT i FROM Incident i WHERE i.dateCreation BETWEEN :startDate AND :endDate")
    Page<Incident> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);

    /**
     * ✅ FIXED: Advanced multi-criteria search for incidents
     * Uses COALESCE and CAST to avoid PostgreSQL parameter type issues
     * All parameters are optional (nullable) - only non-null values are used as filters
     *
     * @param statut Filter by incident status (optional)
     * @param categorie Filter by incident department (optional)
     * @param gouvernorat Filter by gouvernorat (optional)
     * @param municipalite Filter by municipalite (optional)
     * @param agentId Filter by assigned agent ID (optional)
     * @param startDate Filter by minimum creation date (optional)
     * @param endDate Filter by maximum creation date (optional)
     * @param pageable Pagination information
     * @return Page of incidents matching all specified criteria
     */
    @Query("""
        SELECT i FROM Incident i 
        LEFT JOIN i.quartier q 
        WHERE (:statut IS NULL OR i.statut = :statut) 
        AND (:categorie IS NULL OR i.categorie = :categorie) 
        AND (COALESCE(:gouvernorat, '') = '' OR q.gouvernorat = :gouvernorat) 
        AND (COALESCE(:municipalite, '') = '' OR q.municipalite = :municipalite) 
        AND (:agentId IS NULL OR i.agent.id = :agentId) 
        AND (CAST(:startDate AS timestamp) IS NULL OR i.dateCreation >= :startDate) 
        AND (CAST(:endDate AS timestamp) IS NULL OR i.dateCreation <= :endDate)
        """)
    Page<Incident> findByMultipleFilters(
            @Param("statut") StatutIncident statut,
            @Param("categorie") Departement categorie,
            @Param("gouvernorat") String gouvernorat,
            @Param("municipalite") String municipalite,
            @Param("agentId") Long agentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Get all distinct gouvernorats from incidents
     * Used to populate filter dropdown
     * @return List of unique gouvernorat names
     */
    @Query("SELECT DISTINCT i.quartier.gouvernorat FROM Incident i " +
            "WHERE i.quartier.gouvernorat IS NOT NULL " +
            "ORDER BY i.quartier.gouvernorat ASC")
    List<String> findAllDistinctGouvernorats();

    /**
     * Get all distinct municipalites from incidents
     * Used to populate filter dropdown
     * @return List of unique municipalite names
     */
    @Query("SELECT DISTINCT i.quartier.municipalite FROM Incident i " +
            "WHERE i.quartier.municipalite IS NOT NULL " +
            "ORDER BY i.quartier.municipalite ASC")
    List<String> findAllDistinctMunicipalites();
    /**
     * Count incidents by agent and status
     */
    long countByAgentIdAndStatut(Long agentId, StatutIncident status);

}
