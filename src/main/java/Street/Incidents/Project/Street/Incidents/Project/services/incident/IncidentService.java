package Street.Incidents.Project.Street.Incidents.Project.services.incident;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Quartier;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuartierService quartierService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private EmailService emailService;

    /**
     * Save a new incident
     */
    @Transactional
    public void saveIncident(
            Incident incident,
            String userEmail,
            String gouvernorat,
            String municipalite,
            MultipartFile[] photos
    ) throws IOException {

        // 1️⃣ User
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        incident.setDeclarant(user);

        // 2️⃣ Quartier
        Quartier quartier = quartierService.findOrCreateQuartier(
                gouvernorat, municipalite
        );
        incident.setQuartier(quartier);

        // 3️⃣ Business fields
        incident.setDateCreation(LocalDateTime.now());
        incident.setStatut(StatutIncident.SIGNALE);

        // 4️⃣ Save incident first
        Incident savedIncident = incidentRepo.save(incident);

        // 5️⃣ Save photos
        if (photos != null && photos.length > 0) {
            photoService.savePhotos(photos, savedIncident);
        }
    }

    // ========================================
    // ✅ ADMIN WORKFLOW MANAGEMENT METHODS
    // ========================================

    /**
     * Get all incidents with pagination
     */
    public Page<Incident> getAllIncidents(Pageable pageable) {
        log.info("Fetching all incidents with pagination: {}", pageable);
        return incidentRepo.findAll(pageable);
    }

    /**
     * Get incidents filtered by status
     */
    public Page<Incident> getIncidentsByStatus(StatutIncident status, Pageable pageable) {
        log.info("Fetching incidents with status: {} and pagination: {}", status, pageable);
        return incidentRepo.findByStatut(status, pageable);
    }

    /**
     * Get incidents by agent
     */
    public Page<Incident> getIncidentsByAgent(Long agentId, Pageable pageable) {
        log.info("Fetching incidents assigned to agent: {} with pagination: {}", agentId, pageable);
        return incidentRepo.findByAgentId(agentId, pageable);
    }

    /**
     * Get unassigned incidents
     */
    public Page<Incident> getUnassignedIncidents(Pageable pageable) {
        log.info("Fetching unassigned incidents with pagination: {}", pageable);
        return incidentRepo.findByAgentIsNull(pageable);
    }

    /**
     * Get incident by ID
     */
    public Optional<Incident> getIncidentById(Long id) {
        log.info("Fetching incident with ID: {}", id);
        return incidentRepo.findById(id);
    }

    /**
     * Assign an incident to an agent with email notifications
     */
    @Transactional
    public Incident assignIncidentToAgent(Long incidentId, Long agentId, boolean sendNotification) {
        log.info("Assigning incident {} to agent {}", incidentId, agentId);

        Incident incident = incidentRepo.findById(incidentId)
                .orElseThrow(() -> {
                    log.error("Incident not found with ID: {}", incidentId);
                    return new IllegalArgumentException("Incident not found");
                });

        User agent = userRepo.findById(agentId)
                .orElseThrow(() -> {
                    log.error("Agent not found with ID: {}", agentId);
                    return new IllegalArgumentException("Agent not found");
                });

        // Verify that user is actually an agent or admin
        if (agent.getRole() != Role.AGENT_MUNICIPAL && agent.getRole() != Role.ADMINISTRATEUR) {
            log.error("User {} is not an agent or administrator", agentId);
            throw new IllegalArgumentException("User is not an agent or administrator");
        }

        // Store old status for citizen notification
        StatutIncident oldStatus = incident.getStatut();

        incident.setAgent(agent);

        // Update status to "Pris en charge" if it was "Signalé"
        if (incident.getStatut() == StatutIncident.SIGNALE) {
            incident.setStatut(StatutIncident.PRIS_EN_CHARGE);
            log.info("Updated incident status to PRIS_EN_CHARGE");
        }

        Incident savedIncident = incidentRepo.save(incident);
        log.info("Incident {} assigned to agent {}", incidentId, agentId);

        // Send notification emails
        if (sendNotification) {
            String agentFullName = buildFullName(agent);

            // 1️⃣ Send notification email to AGENT
            try {
                emailService.sendIncidentAssignmentNotification(
                        agent.getEmail(),
                        agentFullName,
                        incident.getId(),
                        incident.getTitre(),
                        incident.getDescription(),
                        incident.getCategorie() != null ? incident.getCategorie().toString() : "N/A",
                        incident.getPriorite() != null ? incident.getPriorite().toString() : "MOYENNE"
                );

                log.info("Assignment notification sent to agent: {}", agent.getEmail());
            } catch (Exception e) {
                log.error("Failed to send assignment notification to agent {}: {}", agent.getEmail(), e.getMessage());
                // Don't throw - incident is still assigned, just log the error
            }

            // 2️⃣ Send status update notification to CITIZEN (declarant)
            if (incident.getDeclarant() != null && incident.getDeclarant().getEmail() != null) {
                try {
                    User citizen = incident.getDeclarant();
                    String citizenName = buildFullName(citizen);

                    emailService.sendIncidentStatusUpdateToCitizen(
                            citizen.getEmail(),
                            citizenName,
                            incident.getId(),
                            incident.getTitre(),
                            oldStatus != null ? oldStatus.name() : "SIGNALE",
                            "PRIS_EN_CHARGE",
                            agentFullName
                    );

                    log.info("Status update notification sent to citizen: {}", citizen.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send status update to citizen {}: {}",
                            incident.getDeclarant().getEmail(), e.getMessage());
                    // Don't throw - incident is still assigned, just log the error
                }
            }
        }

        return savedIncident;
    }

    /**
     * Update incident status with citizen notification
     */
    @Transactional
    public Incident updateIncidentStatus(Long incidentId, StatutIncident newStatus) {
        log.info("Updating incident {} status to {}", incidentId, newStatus);

        Incident incident = incidentRepo.findById(incidentId)
                .orElseThrow(() -> {
                    log.error("Incident not found with ID: {}", incidentId);
                    return new IllegalArgumentException("Incident not found");
                });

        StatutIncident oldStatus = incident.getStatut();
        incident.setStatut(newStatus);

        // Set resolution date if incident is resolved or closed
        if (newStatus == StatutIncident.RESOLU || newStatus == StatutIncident.CLOTURE) {
            if (incident.getDateResolution() == null) {
                incident.setDateResolution(LocalDateTime.now());
                log.info("Set resolution date for incident {}", incidentId);
            }
        }

        Incident savedIncident = incidentRepo.save(incident);
        log.info("Incident {} status updated to {}", incidentId, newStatus);

        // ✅ Send status update notification to CITIZEN
        if (incident.getDeclarant() != null && incident.getDeclarant().getEmail() != null) {
            try {
                User citizen = incident.getDeclarant();
                String citizenName = buildFullName(citizen);

                String agentName = incident.getAgent() != null ?
                        buildFullName(incident.getAgent()) : null;

                emailService.sendIncidentStatusUpdateToCitizen(
                        citizen.getEmail(),
                        citizenName,
                        incident.getId(),
                        incident.getTitre(),
                        oldStatus != null ? oldStatus.name() : "SIGNALE",
                        newStatus.name(),
                        agentName
                );

                log.info("Status update notification sent to citizen: {}", citizen.getEmail());
            } catch (Exception e) {
                log.error("Failed to send status update to citizen {}: {}",
                        incident.getDeclarant().getEmail(), e.getMessage());
                // Don't throw - status is still updated, just log the error
            }
        }

        return savedIncident;
    }

    // ========================================
    // ✅ ADVANCED FILTERING METHODS
    // ========================================

    /**
     * Advanced filtering with multiple criteria
     * All parameters are optional - only non-null values are used as filters
     */
    public Page<Incident> filterIncidents(
            StatutIncident statut,
            Departement departement,
            String gouvernorat,
            String municipalite,
            Long agentId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Filtering incidents with criteria - Status: {}, Department: {}, Gouvernorat: {}, " +
                        "Municipalite: {}, AgentId: {}, DateRange: {} to {}",
                statut, departement, gouvernorat, municipalite, agentId, startDate, endDate);

        return incidentRepo.findByMultipleFilters(
                statut, departement, gouvernorat, municipalite, agentId, startDate, endDate, pageable
        );
    }

    /**
     * Get incidents filtered by department (categorie)
     */
    public Page<Incident> getIncidentsByDepartement(Departement departement, Pageable pageable) {
        log.info("Fetching incidents with department: {} and pagination: {}", departement, pageable);
        return incidentRepo.findByCategorie(departement, pageable);
    }

    /**
     * Get incidents filtered by gouvernorat
     */
    public Page<Incident> getIncidentsByGouvernorat(String gouvernorat, Pageable pageable) {
        log.info("Fetching incidents in gouvernorat: {} with pagination: {}", gouvernorat, pageable);
        return incidentRepo.findByGouvernorat(gouvernorat, pageable);
    }

    /**
     * Get incidents filtered by municipalite
     */
    public Page<Incident> getIncidentsByMunicipalite(String municipalite, Pageable pageable) {
        log.info("Fetching incidents in municipalite: {} with pagination: {}", municipalite, pageable);
        return incidentRepo.findByMunicipalite(municipalite, pageable);
    }

    /**
     * Get incidents within a date range
     */
    public Page<Incident> getIncidentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.info("Fetching incidents between {} and {} with pagination: {}", startDate, endDate, pageable);
        return incidentRepo.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Get all distinct gouvernorats for filter dropdown
     */
    public List<String> getAllGouvernorats() {
        log.info("Fetching all distinct gouvernorats");
        return incidentRepo.findAllDistinctGouvernorats();
    }

    /**
     * Get all distinct municipalites for filter dropdown
     */
    public List<String> getAllMunicipalites() {
        log.info("Fetching all distinct municipalites");
        return incidentRepo.findAllDistinctMunicipalites();
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Get all agents (for assignment dropdown)
     */
    public List<User> getAllAgents() {
        log.info("Fetching all agents");
        return userRepo.findByRoleIn(List.of(Role.AGENT_MUNICIPAL, Role.ADMINISTRATEUR));
    }

    /**
     * Count incidents by status
     */
    public long countIncidentsByStatus(StatutIncident status) {
        long count = incidentRepo.countByStatut(status);
        log.debug("Counted {} incidents with status {}", count, status);
        return count;
    }

    /**
     * Count total incidents
     */
    public long countTotalIncidents() {
        long count = incidentRepo.count();
        log.debug("Total incidents count: {}", count);
        return count;
    }

    /**
     * Count unassigned incidents
     */
    public long countUnassignedIncidents() {
        long count = incidentRepo.countByAgentIsNull();
        log.debug("Unassigned incidents count: {}", count);
        return count;
    }

    /**
     * Helper method to build full name from user
     */
    private String buildFullName(User user) {
        if (user == null) {
            return "Utilisateur";
        }

        StringBuilder fullName = new StringBuilder();

        if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
            fullName.append(user.getPrenom());
        }

        if (user.getNom() != null && !user.getNom().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(user.getNom());
        }

        // If both are null/empty, use default based on role or email
        if (fullName.length() == 0) {
            if (user.getEmail() != null) {
                return user.getEmail().split("@")[0];
            }
            return "Utilisateur";
        }

        return fullName.toString();
    }
    /**
     * Count incidents by agent and status
     */
    public long countIncidentsByAgentAndStatus(Long agentId, StatutIncident status) {
        long count = incidentRepo.countByAgentIdAndStatut(agentId, status);
        log.debug("Counted {} incidents for agent {} with status {}", count, agentId, status);
        return count;
    }

}
