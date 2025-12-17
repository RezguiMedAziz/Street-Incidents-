package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentDashboardService {

    private final IncidentRepository incidentRepository;

    /**
     * Get all incidents assigned to a specific agent
     * @param agentId The agent's user ID
     * @return List of incidents assigned to the agent
     */
    public List<Incident> getIncidentsAssignedToAgent(Long agentId) {
        log.info("Fetching incidents assigned to agent ID: {}", agentId);
        List<Incident> incidents = incidentRepository.findByAgentId(agentId);
        log.info("Found {} incidents for agent {}", incidents.size(), agentId);
        return incidents;
    }

    /**
     * Get incident by ID
     * @param incidentId Incident ID
     * @return Incident entity
     */
    public Incident getIncidentById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));
    }

    /**
     * Get comprehensive statistics for an agent
     * @param agentId The agent's user ID
     * @return Map containing various statistics
     */
    public Map<String, Long> getAgentStatistics(Long agentId) {
        log.info("Calculating statistics for agent ID: {}", agentId);

        Map<String, Long> stats = new HashMap<>();

        // Get all assigned incidents
        List<Incident> allIncidents = incidentRepository.findByAgentId(agentId);

        // Total incidents
        stats.put("total", (long) allIncidents.size());

        // Count by priority
        long critical = allIncidents.stream()
                .filter(i -> i.getPriorite() == Priorite.CRITIQUE)
                .count();
        long high = allIncidents.stream()
                .filter(i -> i.getPriorite() == Priorite.ELEVEE)
                .count();
        long medium = allIncidents.stream()
                .filter(i -> i.getPriorite() == Priorite.MOYENNE)
                .count();
        long low = allIncidents.stream()
                .filter(i -> i.getPriorite() == Priorite.FAIBLE)
                .count();

        stats.put("critical", critical);
        stats.put("high", high);
        stats.put("medium", medium);
        stats.put("low", low);

        // Count by status
        long signale = allIncidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.SIGNALE)
                .count();
        long prisEnCharge = allIncidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE)
                .count();
        long enResolution = allIncidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.EN_RESOLUTION)
                .count();
        long resolu = allIncidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU)
                .count();
        long cloture = allIncidents.stream()
                .filter(i -> i.getStatut() == StatutIncident.CLOTURE)
                .count();

        stats.put("signale", signale);
        stats.put("prisEnCharge", prisEnCharge);
        stats.put("enResolution", enResolution);
        stats.put("resolu", resolu);
        stats.put("cloture", cloture);

        // In progress = Pris en charge + En résolution
        stats.put("inProgress", prisEnCharge + enResolution);

        log.info("Statistics calculated: {} total, {} critical, {} high, {} in progress",
                stats.get("total"), critical, high, stats.get("inProgress"));

        return stats;
    }

    /**
     * Get statistics by category (Departement)
     * @param agentId The agent's user ID
     * @return Map with category names and counts
     */
    public Map<String, Long> getCategoryStatistics(Long agentId) {
        List<Incident> incidents = incidentRepository.findByAgentId(agentId);

        Map<String, Long> categoryStats = new HashMap<>();

        incidents.forEach(incident -> {
            String category = incident.getCategorie().name();
            categoryStats.put(category, categoryStats.getOrDefault(category, 0L) + 1);
        });

        return categoryStats;
    }

    /**
     * Get statistics by status
     * @param agentId The agent's user ID
     * @return Map with status names and counts
     */
    public Map<String, Long> getStatusStatistics(Long agentId) {
        List<Incident> incidents = incidentRepository.findByAgentId(agentId);

        Map<String, Long> statusStats = new HashMap<>();

        incidents.forEach(incident -> {
            String status = incident.getStatut().name();
            statusStats.put(status, statusStats.getOrDefault(status, 0L) + 1);
        });

        return statusStats;
    }

    /**
     * Get incidents by specific priority
     * @param agentId The agent's user ID
     * @param priorite Priority level
     * @return List of incidents with specified priority
     */
    public List<Incident> getIncidentsByPriority(Long agentId, Priorite priorite) {
        return incidentRepository.findByAgentId(agentId).stream()
                .filter(i -> i.getPriorite() == priorite)
                .toList();
    }

    /**
     * Get incidents by specific status
     * @param agentId The agent's user ID
     * @param statut Status
     * @return List of incidents with specified status
     */
    public List<Incident> getIncidentsByStatus(Long agentId, StatutIncident statut) {
        return incidentRepository.findByAgentIdAndStatut(agentId, statut);
    }

    /**
     * Get critical incidents that need immediate attention
     * @param agentId The agent's user ID
     * @return List of critical priority incidents
     */
    public List<Incident> getCriticalIncidents(Long agentId) {
        return getIncidentsByPriority(agentId, Priorite.CRITIQUE);
    }

    /**
     * Get active incidents (not resolved or closed)
     * @param agentId The agent's user ID
     * @return List of active incidents
     */
    public List<Incident> getActiveIncidents(Long agentId) {
        return incidentRepository.findByAgentId(agentId).stream()
                .filter(i -> i.getStatut() != StatutIncident.RESOLU &&
                        i.getStatut() != StatutIncident.CLOTURE)
                .toList();
    }
}