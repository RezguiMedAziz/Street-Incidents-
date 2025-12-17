package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;


    // ========================================
    // MÉTHODES POUR LES GRAPHIQUES ET KPI (OPTIMISÉES)
    // ========================================

    public Map<String, Long> getIncidentsByType() {
        return incidentRepository.countGroupByCategorie().stream()
                .collect(Collectors.toMap(
                        obj -> obj[0] == null ? "NON_DEFINI" : obj[0].toString(),
                        obj -> (Long) obj[1],
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Long> getIncidentsByStatus() {
        return incidentRepository.countGroupByStatut().stream()
                .collect(Collectors.toMap(
                        obj -> obj[0] == null ? "INCONNU" : obj[0].toString(),
                        obj -> (Long) obj[1],
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Long> getIncidentsByPriority() {
        return incidentRepository.countGroupByPriorite().stream()
                .collect(Collectors.toMap(
                        obj -> obj[0] == null ? "NORMALE" : obj[0].toString(),
                        obj -> (Long) obj[1],
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Long> getTop10Quartiers() {
        return incidentRepository.countTop10QuartiersRaw().stream()
                .limit(10)
                .collect(Collectors.toMap(
                        obj -> obj[0] == null ? "Inconnu" : obj[0].toString(),
                        obj -> (Long) obj[1],
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Long> getIncidentsTrendLast30Days() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        // Initialiser toutes les 30 dates à 0
        Map<String, Long> trendMap = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trendMap.put(date.format(formatter), 0L);
        }

        // Remplir avec les vraies données
        incidentRepository.countIncidentsLast30Days(start).forEach(row -> {
            // row[0] est maintenant directement un LocalDate (pas java.sql.Date)
            LocalDate date = (LocalDate) row[0];
            String label = date.format(formatter);
            trendMap.put(label, (Long) row[1]);
        });

        return trendMap;
    }

    public long getTotalIncidents() {
        return incidentRepository.countTotalIncidents();
    }

    public long getResolvedIncidents() {
        return incidentRepository.countResolvedIncidents();
    }

    public long getPendingIncidents() {
        return incidentRepository.countPendingIncidents();
    }

    public double getAverageResolutionTime() {
        List<Incident> resolvedIncidents = incidentRepository.findAll().stream()
                .filter(i -> i.getStatut() == StatutIncident.RESOLU &&
                        i.getDateResolution() != null &&
                        i.getDateCreation() != null)
                .toList();

        if (resolvedIncidents.isEmpty()) {
            return 0.0;
        }

        return resolvedIncidents.stream()
                .mapToLong(i -> java.time.Duration
                        .between(i.getDateCreation(), i.getDateResolution())
                        .toHours())
                .average()
                .orElse(0.0);
    }

    public double getResolutionRate() {
        long total = getTotalIncidents();
        if (total == 0) return 0.0;
        return (getResolvedIncidents() * 100.0) / total;
    }

    public long getActiveAgents() {
        return userRepository.findByRole(Role.AGENT_MUNICIPAL).size();
    }

    public long getTotalCitizens() {
        return userRepository.findByRole(Role.CITOYEN).size();
    }

    // ========================================
    // MÉTHODE PRINCIPALE DU DASHBOARD
    // ========================================

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalIncidents(getTotalIncidents());
        stats.setResolvedIncidents(getResolvedIncidents());
        stats.setPendingIncidents(getPendingIncidents());
        stats.setAvgResolutionTime(getAverageResolutionTime());
        stats.setResolutionRate(getResolutionRate());
        stats.setActiveAgents(getActiveAgents());
        stats.setTotalCitizens(getTotalCitizens());

        stats.setIncidentsByType(getIncidentsByType());
        stats.setIncidentsByStatus(getIncidentsByStatus());
        stats.setIncidentsByPriority(getIncidentsByPriority());
        stats.setTop10Quartiers(getTop10Quartiers());
        stats.setIncidentsTrend(getIncidentsTrendLast30Days());

        return stats;
    }

    // ========================================
    // CLASSE INTERNE DASHBOARD STATS
    // ========================================

    @lombok.Data
    public static class DashboardStats {
        private long totalIncidents;
        private long resolvedIncidents;
        private long pendingIncidents;
        private double avgResolutionTime;
        private double resolutionRate;
        private long activeAgents;
        private long totalCitizens;

        private Map<String, Long> incidentsByType;
        private Map<String, Long> incidentsByStatus;
        private Map<String, Long> incidentsByPriority;
        private Map<String, Long> top10Quartiers;
        private Map<String, Long> incidentsTrend;
    }

    // ========================================
    // MÉTHODES ANCIENNES (tu peux les garder ou les supprimer plus tard)
    // ========================================

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public List<Incident> getIncidentsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return incidentRepository.findByDeclarantId(user.getId());
    }

    public List<Incident> getIncidentsByAgent(String agentEmail) {
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
        return incidentRepository.findByAgentId(agent.getId());
    }

    public Optional<Incident> getIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    public List<Incident> getIncidentsByStatut(StatutIncident statut) {
        return incidentRepository.findByStatut(statut);
    }

    public long countIncidentsByStatutForUser(String userEmail, StatutIncident statut) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return incidentRepository.countByDeclarantIdAndStatut(user.getId(), statut);
    }

    public long countIncidentsByStatutForAgent(String agentEmail, StatutIncident statut) {
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
        return incidentRepository.countByAgentIdAndStatut(agent.getId(), statut);
    }
}