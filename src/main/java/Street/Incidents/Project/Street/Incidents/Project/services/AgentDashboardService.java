package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentDashboardService {

    private final IncidentRepository incidentRepository;

    /**
     * Get all incidents assigned to a specific agent
     */
    public List<Incident> getIncidentsAssignedToAgent(Long agentId) {
        log.info("Fetching incidents assigned to agent ID: {}", agentId);
        return incidentRepository.findAllByAgentId(agentId);
    }

    /**
     * Get incidents by specific status for an agent
     */
    public List<Incident> getIncidentsByStatus(Long agentId, StatutIncident statut) {
        return incidentRepository.findAllByAgentIdAndStatut(agentId, statut);
    }

    /**
     * Get incidents by specific priority for an agent
     */
    public List<Incident> getIncidentsByPriority(Long agentId, Priorite priorite) {
        return getIncidentsAssignedToAgent(agentId).stream()
                .filter(i -> i.getPriorite() == priorite)
                .collect(Collectors.toList());
    }

    /**
     * Get active incidents (not resolved or closed)
     */
    public List<Incident> getActiveIncidents(Long agentId) {
        return getIncidentsAssignedToAgent(agentId).stream()
                .filter(i -> i.getStatut() != StatutIncident.RESOLU &&
                        i.getStatut() != StatutIncident.CLOTURE)
                .collect(Collectors.toList());
    }

    /**
     * Get critical incidents that need immediate attention
     */
    public List<Incident> getCriticalIncidents(Long agentId) {
        return getIncidentsByPriority(agentId, Priorite.CRITIQUE);
    }

    /**
     * Get statistics for an agent
     */
    public Map<String, Long> getAgentStatistics(Long agentId) {
        log.info("Calculating statistics for agent ID: {}", agentId);
        Map<String, Long> stats = new HashMap<>();

        List<Incident> allIncidents = getIncidentsAssignedToAgent(agentId);

        stats.put("total", (long) allIncidents.size());

        // Count by priority
        stats.put("critical", (long) allIncidents.stream().filter(i -> i.getPriorite() == Priorite.CRITIQUE).count());
        stats.put("high", (long) allIncidents.stream().filter(i -> i.getPriorite() == Priorite.ELEVEE).count());
        stats.put("medium", (long) allIncidents.stream().filter(i -> i.getPriorite() == Priorite.MOYENNE).count());
        stats.put("low", (long) allIncidents.stream().filter(i -> i.getPriorite() == Priorite.FAIBLE).count());

        // Count by status
        stats.put("signale", (long) allIncidents.stream().filter(i -> i.getStatut() == StatutIncident.SIGNALE).count());
        stats.put("prisEnCharge", (long) allIncidents.stream().filter(i -> i.getStatut() == StatutIncident.PRIS_EN_CHARGE).count());
        stats.put("enResolution", (long) allIncidents.stream().filter(i -> i.getStatut() == StatutIncident.EN_RESOLUTION).count());
        stats.put("resolu", (long) allIncidents.stream().filter(i -> i.getStatut() == StatutIncident.RESOLU).count());
        stats.put("cloture", (long) allIncidents.stream().filter(i -> i.getStatut() == StatutIncident.CLOTURE).count());

        stats.put("inProgress", stats.get("prisEnCharge") + stats.get("enResolution"));

        log.info("Statistics calculated for agent {}: {}", agentId, stats);
        return stats;
    }

    /**
     * Get statistics by category (Departement)
     */
    public Map<String, Long> getCategoryStatistics(Long agentId) {
        List<Incident> incidents = getIncidentsAssignedToAgent(agentId);
        Map<String, Long> categoryStats = new HashMap<>();
        incidents.forEach(i -> {
            String category = i.getCategorie().name();
            categoryStats.put(category, categoryStats.getOrDefault(category, 0L) + 1);
        });
        return categoryStats;
    }

    /**
     * Get statistics by status
     */
    public Map<String, Long> getStatusStatistics(Long agentId) {
        List<Incident> incidents = getIncidentsAssignedToAgent(agentId);
        Map<String, Long> statusStats = new HashMap<>();
        incidents.forEach(i -> {
            String status = i.getStatut().name();
            statusStats.put(status, statusStats.getOrDefault(status, 0L) + 1);
        });
        return statusStats;
    }

    /**
     * Get incident by ID
     */
    public Incident getIncidentById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + incidentId));
    }
}