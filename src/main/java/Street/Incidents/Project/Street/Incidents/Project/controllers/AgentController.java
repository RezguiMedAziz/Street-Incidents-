package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    /**
     * Display agent home page
     */
    @GetMapping("/home")
    public String agentHome(HttpSession session, Model model) {
        log.info("Loading agent home page");

        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Agent Dashboard");

        log.info("Agent home loaded for user: {} ({})", userName, userRole);

        return "agent/home";
    }

    /**
     * Display assigned incidents with advanced filtering
     */
    @GetMapping("/incidents")
    public String showMyIncidents(
            HttpSession session,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            // ✅ Advanced filter parameters
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String gouvernorat,
            @RequestParam(required = false) String municipalite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Loading agent incidents page with filters - Status: {}, Department: {}, Gouvernorat: {}, " +
                        "Municipalite: {}, StartDate: {}, EndDate: {}",
                statut, departement, gouvernorat, municipalite, startDate, endDate);

        // ✅ Get logged-in agent info
        String userEmail = (String) session.getAttribute("userEmail");
        User agent = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "incidents");
        model.addAttribute("pageTitle", "My Assigned Incidents");

        // ✅ Pagination and sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ Parse filter parameters
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

        // ✅ Apply filters - ONLY for this agent's incidents
        Page<Incident> incidentPage;
        boolean hasFilters = statutEnum != null || departementEnum != null ||
                (gouvernorat != null && !gouvernorat.isEmpty()) ||
                (municipalite != null && !municipalite.isEmpty()) ||
                startDateTime != null || endDateTime != null;

        if (hasFilters) {
            // Filter incidents assigned to this agent
            incidentPage = incidentService.filterIncidents(
                    statutEnum, departementEnum, gouvernorat, municipalite,
                    agent.getId(), startDateTime, endDateTime, pageable
            );
            log.info("Filtered incidents found for agent {}: {}", agent.getId(), incidentPage.getTotalElements());
        } else {
            // Get all incidents assigned to this agent
            incidentPage = incidentService.getIncidentsByAgent(agent.getId(), pageable);
            log.info("All incidents found for agent {}: {}", agent.getId(), incidentPage.getTotalElements());
        }

        // ✅ Add pagination data to model
        model.addAttribute("incidents", incidentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidentPage.getTotalPages());
        model.addAttribute("totalElements", incidentPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        // ✅ Add selected filter values (to preserve form state)
        model.addAttribute("selectedStatut", statut);
        model.addAttribute("selectedDepartement", departement);
        model.addAttribute("selectedGouvernorat", gouvernorat);
        model.addAttribute("selectedMunicipalite", municipalite);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);

        // ✅ ADD FILTER OPTIONS FOR DROPDOWNS
        // Only show statuses that agent can work with
        model.addAttribute("statuts", new StatutIncident[]{
                StatutIncident.PRIS_EN_CHARGE,
                StatutIncident.EN_RESOLUTION,
                StatutIncident.RESOLU
        });
        model.addAttribute("departements", Departement.values());
        model.addAttribute("gouvernorats", incidentService.getAllGouvernorats());
        model.addAttribute("municipalites", incidentService.getAllMunicipalites());

        // ✅ Add statistics for this agent
        model.addAttribute("totalIncidents", incidentPage.getTotalElements());
        model.addAttribute("prisEnChargeIncidents",
                incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.PRIS_EN_CHARGE));
        model.addAttribute("enResolutionIncidents",
                incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.EN_RESOLUTION));
        model.addAttribute("resoluIncidents",
                incidentService.countIncidentsByAgentAndStatus(agent.getId(), StatutIncident.RESOLU));

        log.info("Agent incidents page loaded for user: {} ({})", userName, userRole);

        return "agent/incidents";
    }

    /**
     * Update incident status (agent can only update their own incidents)
     */
    @PostMapping("/update-incident-status")
    public String updateIncidentStatus(
            HttpSession session,
            @RequestParam Long incidentId,
            @RequestParam StatutIncident newStatus,
            RedirectAttributes redirectAttributes) {

        log.info("Agent request to update incident {} status to {}", incidentId, newStatus);

        String userEmail = (String) session.getAttribute("userEmail");
        User agent = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        try {
            // ✅ Verify the incident is assigned to this agent
            Incident incident = incidentService.getIncidentById(incidentId)
                    .orElseThrow(() -> new IllegalArgumentException("Incident not found"));

            if (incident.getAgent() == null || !incident.getAgent().getId().equals(agent.getId())) {
                log.error("Agent {} tried to update incident {} which is not assigned to them",
                        agent.getId(), incidentId);
                redirectAttributes.addFlashAttribute("error",
                        "You can only update incidents assigned to you.");
                return "redirect:/agent/incidents";
            }

            // ✅ Validate status transition
            if (!isValidStatusTransition(incident.getStatut(), newStatus)) {
                log.error("Invalid status transition from {} to {}", incident.getStatut(), newStatus);
                redirectAttributes.addFlashAttribute("error",
                        "Invalid status transition. Please follow the workflow.");
                return "redirect:/agent/incidents";
            }

            // ✅ Update status and send notification to citizen
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
     * Agent can only: PRIS_EN_CHARGE → EN_RESOLUTION → RESOLU
     */
    private boolean isValidStatusTransition(StatutIncident currentStatus, StatutIncident newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case PRIS_EN_CHARGE -> newStatus == StatutIncident.EN_RESOLUTION;
            case EN_RESOLUTION -> newStatus == StatutIncident.RESOLU;
            case RESOLU -> false; // Cannot change from RESOLU
            default -> false; // Agent cannot change SIGNALE or CLOTURE
        };
    }
}
