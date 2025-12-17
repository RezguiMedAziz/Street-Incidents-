package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentDashboardService {

    private final IncidentRepository incidentRepository;

    // ========== NEW ID-BASED METHODS (Recommended) ==========

    // Récupérer tous les incidents d'un citoyen par ID
    public List<Incident> getIncidentsByDeclarantId(Long declarantId) {
        return incidentRepository.findByDeclarantId(declarantId);
    }

    // Récupérer les incidents récents d'un citoyen par ID
    public List<Incident> getRecentIncidentsByDeclarantId(Long declarantId) {
        return incidentRepository.findRecentByDeclarantId(declarantId);
    }

    // Récupérer les 5 derniers incidents par ID
    public List<Incident> getTop5RecentIncidents(Long declarantId) {
        return incidentRepository.findTop5ByDeclarantIdOrderByDateCreationDesc(declarantId);
    }

    // Compter les incidents par statut pour un citoyen par ID
    public Map<StatutIncident, Long> countIncidentsByStatutForDeclarantId(Long declarantId) {
        Map<StatutIncident, Long> counts = new HashMap<>();

        for (StatutIncident statut : StatutIncident.values()) {
            long count = incidentRepository.countByDeclarantIdAndStatut(declarantId, statut);
            counts.put(statut, count);
        }

        return counts;
    }

    // Compter tous les incidents d'un citoyen par ID
    public long countTotalIncidents(Long declarantId) {
        return incidentRepository.countByDeclarantId(declarantId);
    }

    // Récupérer un incident par ID (avec vérification que c'est bien le déclarant)
    public Incident getIncidentByIdAndDeclarantId(Long id, Long declarantId) {
        return incidentRepository.findById(id)
                .filter(incident -> incident.getDeclarant() != null &&
                        incident.getDeclarant().getId().equals(declarantId))
                .orElseThrow(() -> new RuntimeException("Incident non trouvé ou accès non autorisé"));
    }

    // ========== BACKWARD COMPATIBILITY METHODS (Delegate to ID-based methods) ==========

    // Récupérer tous les incidents d'un citoyen
    public List<Incident> getIncidentsByDeclarant(User declarant) {
        if (declarant == null || declarant.getId() == null) {
            return java.util.Collections.emptyList();
        }
        return getIncidentsByDeclarantId(declarant.getId());
    }

    // Récupérer les incidents récents d'un citoyen
    public List<Incident> getRecentIncidentsByDeclarant(User declarant) {
        if (declarant == null || declarant.getId() == null) {
            return java.util.Collections.emptyList();
        }
        return getRecentIncidentsByDeclarantId(declarant.getId());
    }

    // Récupérer les 5 derniers incidents
    public List<Incident> getTop5RecentIncidents(User declarant) {
        if (declarant == null || declarant.getId() == null) {
            return java.util.Collections.emptyList();
        }
        return getTop5RecentIncidents(declarant.getId());
    }

    // Compter les incidents par statut pour un citoyen
    public Map<StatutIncident, Long> countIncidentsByStatutForDeclarant(User declarant) {
        if (declarant == null || declarant.getId() == null) {
            Map<StatutIncident, Long> emptyCounts = new HashMap<>();
            for (StatutIncident statut : StatutIncident.values()) {
                emptyCounts.put(statut, 0L);
            }
            return emptyCounts;
        }
        return countIncidentsByStatutForDeclarantId(declarant.getId());
    }

    // Récupérer un incident par ID (avec vérification que c'est bien le déclarant)
    public Incident getIncidentByIdAndDeclarant(Long id, User declarant) {
        if (declarant == null || declarant.getId() == null) {
            throw new RuntimeException("Utilisateur non valide");
        }
        return getIncidentByIdAndDeclarantId(id, declarant.getId());
    }

    // ========== CREATE METHOD ==========

    // Créer un nouvel incident
    @Transactional
    public Incident createIncident(Incident incident, User declarant) {
        incident.setDeclarant(declarant);
        incident.setStatut(StatutIncident.SIGNALE); // Statut initial
        return incidentRepository.save(incident);
    }
}