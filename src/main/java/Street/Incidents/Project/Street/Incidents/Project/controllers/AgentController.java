package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.AgentDashboardService;
import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentDashboardService dashboardService;
    private final UserService userService;
    private final IncidentService incidentService;
    private final UserRepository userRepository;

    /**
     * Display agent home page
     */
    @GetMapping({"", "/", "/home"})
    public String agentHome(Authentication authentication, Model model) {
        log.info("Loading agent home page");

        try {
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("userName", currentUser.getNom());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("activePage", "home");
            model.addAttribute("pageTitle", "Agent Dashboard");

            log.info("Agent home loaded for user: {} ({})", currentUser.getNom(), currentUser.getRole().name());

            return "agent/home";
        } catch (Exception e) {
            log.error("Error loading agent home", e);
            model.addAttribute("error", "Erreur lors du chargement de la page d'accueil");
            return "error";
        }
    }

    /**
     * Display agent dashboard with statistics
     */
    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {
        try {
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            log.info("Loading dashboard for agent: {} (ID: {})", currentUser.getNom(), currentUser.getId());

            // Get all incidents assigned to this agent
            List<Incident> assignedIncidents = dashboardService.getIncidentsAssignedToAgent(currentUser.getId());

            // Get statistics
            Map<String, Long> statistics = dashboardService.getAgentStatistics(currentUser.getId());

            // Add data to model
            model.addAttribute("userName", currentUser.getNom());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("incidents", assignedIncidents);
            model.addAttribute("totalIncidents", statistics.get("total"));
            model.addAttribute("criticalCount", statistics.get("critical"));
            model.addAttribute("highCount", statistics.get("high"));
            model.addAttribute("mediumCount", statistics.get("medium"));
            model.addAttribute("lowCount", statistics.get("low"));
            model.addAttribute("inProgressCount", statistics.get("inProgress"));
            model.addAttribute("activePage", "dashboard");
            model.addAttribute("pageTitle", "Agent Dashboard");

            log.info("Dashboard loaded with {} incidents", assignedIncidents.size());

            return "agent/dashboard";

        } catch (Exception e) {
            log.error("Error loading agent dashboard", e);
            model.addAttribute("error", "Erreur lors du chargement du tableau de bord");
            return "error";
        }
    }

    /**
     * Display assigned incidents with advanced filtering
     */
    @GetMapping("/my-incidents")
    public String showMyIncidents(
            Authentication authentication,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String gouvernorat,
            @RequestParam(required = false) String municipalite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Loading agent incidents page with filters - Status: {}, Department: {}, Gouvernorat: {}, " +
                        "Municipalite: {}, StartDate: {}, EndDate: {}",
                statut, departement, gouvernorat, municipalite, startDate, endDate);

        try {
            String email = authentication.getName();
            User agent = userService.getUserByEmail(email);

            model.addAttribute("userRole", agent.getRole().name());
            model.addAttribute("userName", agent.getNom());
            model.addAttribute("userEmail", agent.getEmail());
            model.addAttribute("activePage", "my-incidents");
            model.addAttribute("pageTitle", "My Assigned Incidents");

            // Pagination and sorting
            Sort sort = sortDir.equalsIgnoreCase("asc") ?
                    Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // Parse filter parameters
            StatutIncident statutEnum = null;
            if (statut != null && !statut.isEmpty()) {
                try {
                    statutEnum = StatutIncident.valueOf(statut);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", statut);
                }
            }

            Departement departementEnum = null;
            if (departement != null && !departement.isEmpty()) {
                try {
                    departementEnum = Departement.valueOf(departement);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid department value: {}", departement);
                }
            }

            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

            // Apply filters
            Page<Incident> incidentPage;
            boolean hasFilters = statutEnum != null || departementEnum != null ||
                    (gouvernorat != null && !gouvernorat.isEmpty()) ||
                    (municipalite != null && !municipalite.isEmpty()) ||
                    startDateTime != null || endDateTime != null;

            if (hasFilters) {
                incidentPage = incidentService.filterIncidents(
                        statutEnum, departementEnum, gouvernorat, municipalite,
                        agent.getId(), startDateTime, endDateTime, pageable
                );
                log.info("Filtered incidents found for agent {}: {}", agent.getId(), incidentPage.getTotalElements());
            } else {
                incidentPage = incidentService.getIncidentsByAgent(agent.getId(), pageable);
                log.info("All incidents found for agent {}: {}", agent.getId(), incidentPage.getTotalElements());
            }

            // Add pagination data to model
            model.addAttribute("incidents", incidentPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", incidentPage.getTotalPages());
            model.addAttribute("totalElements", incidentPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

            // Add selected filter values
            model.addAttribute("selectedStatut", statut);
            model.addAttribute("selectedDepartement", departement);
            model.addAttribute("selectedGouvernorat", gouvernorat);
            model.addAttribute("selectedMunicipalite", municipalite);
            model.addAttribute("selectedStartDate", startDate);
            model.addAttribute("selectedEndDate", endDate);

            // Add filter options
            model.addAttribute("statuts", new StatutIncident[]{
                    StatutIncident.PRIS_EN_CHARGE,
                    StatutIncident.EN_RESOLUTION,
                    StatutIncident.RESOLU
            });
            model.addAttribute("departements", Departement.values());
            model.addAttribute("gouvernorats", incidentService.getAllGouvernorats());
            model.addAttribute("municipalites", incidentService.getAllMunicipalites());

            // Add statistics
            model.addAttribute("totalIncidents", incidentPage.getTotalElements());
            model.addAttribute("prisEnChargeIncidents",
                    incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.PRIS_EN_CHARGE));
            model.addAttribute("enResolutionIncidents",
                    incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.EN_RESOLUTION));
            model.addAttribute("resoluIncidents",
                    incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.RESOLU));

            log.info("Agent incidents page loaded for user: {} ({})", agent.getNom(), agent.getRole().name());

            return "agent/my-incidents";

        } catch (Exception e) {
            log.error("Error loading agent incidents", e);
            model.addAttribute("error", "Erreur lors du chargement des incidents");
            return "error";
        }
    }

    /**
     * Display specific incident details
     */
    @GetMapping("/incident/{id}")
    public String showIncidentDetail(@PathVariable Long id,
                                     Authentication authentication,
                                     Model model) {
        try {
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            Incident incident = dashboardService.getIncidentById(id);

            // Verify that this incident is assigned to current agent
            if (incident.getAgent() == null ||
                    !incident.getAgent().getId().equals(currentUser.getId())) {
                log.warn("Agent {} tried to access incident {} not assigned to them",
                        currentUser.getId(), id);
                model.addAttribute("error", "Vous n'avez pas accès à cet incident");
                return "error";
            }

            model.addAttribute("userName", currentUser.getNom());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("incident", incident);
            model.addAttribute("activePage", "my-incidents");
            model.addAttribute("pageTitle", "Incident Details");

            return "agent/incident-detail";

        } catch (Exception e) {
            log.error("Error loading incident detail: {}", id, e);
            model.addAttribute("error", "Incident non trouvé");
            return "error";
        }
    }

    /**
     * Display agent statistics page
     */
    @GetMapping("/statistics")
    public String showStatistics(Authentication authentication, Model model) {
        try {
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            Map<String, Long> statistics = dashboardService.getAgentStatistics(currentUser.getId());
            Map<String, Long> categoryStats = dashboardService.getCategoryStatistics(currentUser.getId());
            Map<String, Long> statusStats = dashboardService.getStatusStatistics(currentUser.getId());

            model.addAttribute("userName", currentUser.getNom());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("statistics", statistics);
            model.addAttribute("categoryStats", categoryStats);
            model.addAttribute("statusStats", statusStats);
            model.addAttribute("activePage", "statistics");
            model.addAttribute("pageTitle", "Agent Statistics");

            return "agent/statistics";

        } catch (Exception e) {
            log.error("Error loading agent statistics", e);
            model.addAttribute("error", "Erreur lors du chargement des statistiques");
            return "error";
        }
    }

    /**
     * Update incident status (agent can only update their own incidents)
     */
    @PostMapping("/update-incident-status")
    public String updateIncidentStatus(
            Authentication authentication,
            @RequestParam Long incidentId,
            @RequestParam StatutIncident newStatus,
            RedirectAttributes redirectAttributes) {

        log.info("Agent request to update incident {} status to {}", incidentId, newStatus);

        try {
            String email = authentication.getName();
            User agent = userService.getUserByEmail(email);

            // Verify the incident is assigned to this agent
            Incident incident = incidentService.getIncidentById(incidentId)
                    .orElseThrow(() -> new IllegalArgumentException("Incident not found"));

            if (incident.getAgent() == null || !incident.getAgent().getId().equals(agent.getId())) {
                log.error("Agent {} tried to update incident {} which is not assigned to them",
                        agent.getId(), incidentId);
                redirectAttributes.addFlashAttribute("error",
                        "You can only update incidents assigned to you.");
                return "redirect:/agent/incidents";
            }

            // Validate status transition
            if (!isValidStatusTransition(incident.getStatut(), newStatus)) {
                log.error("Invalid status transition from {} to {}", incident.getStatut(), newStatus);
                redirectAttributes.addFlashAttribute("error",
                        "Invalid status transition. Please follow the workflow.");
                return "redirect:/agent/incidents";
            }

            // Update status and send notification to citizen
            incidentService.updateIncidentStatus(incidentId, newStatus);

            redirectAttributes.addFlashAttribute("success",
                    "Incident status updated successfully. Notification sent to citizen.");
            log.info("Agent {} updated incident {} status to {}", agent.getId(), incidentId, newStatus);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to update incident status: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while updating the incident status.");
            log.error("Unexpected error while updating incident status: {}", e.getMessage(), e);
        }

        return "redirect:/agent/incidents";
    }

    /**
     * Validate status transitions for agent workflow
     */
    private boolean isValidStatusTransition(StatutIncident currentStatus, StatutIncident newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case PRIS_EN_CHARGE -> newStatus == StatutIncident.EN_RESOLUTION;
            case EN_RESOLUTION -> newStatus == StatutIncident.RESOLU;
            case RESOLU -> false;
            default -> false;
        };
    }
}