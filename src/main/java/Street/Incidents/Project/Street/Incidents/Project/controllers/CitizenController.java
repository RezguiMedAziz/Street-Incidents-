package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.IncidentDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;

import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/citizen")
@RequiredArgsConstructor
@Slf4j
public class CitizenController {

    private final IncidentDashboardService incidentDashboardService;
    private final UserRepository userRepository;
    private final IncidentService incidentService;

    @GetMapping("/dashboard")
    public String citizenDashboard(Principal principal, Model model) {
        log.info("Citizen dashboard accessed");

        if (principal == null) {
            log.warn("No principal found, redirecting to login");
            return "redirect:/login-page";
        }

        // Get user from database using Principal (email)
        String userEmail = principal.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        log.info("User found - ID: {}, Name: {}, Email: {}, Role: {}",
                currentUser.getId(),
                currentUser.getNom() + " " + currentUser.getPrenom(),
                currentUser.getEmail(),
                currentUser.getRole());

        // Add user info to model
        model.addAttribute("userRole", currentUser.getRole().name());
        model.addAttribute("userName", currentUser.getNom() + " " + currentUser.getPrenom());
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("pageTitle", "Citizen Dashboard");
        model.addAttribute("activePage", "dashboard");

        try {
            // Use the ID-based methods from the service
            Long userId = currentUser.getId();

            // Fetch incidents using userId directly
            List<Incident> incidents = incidentDashboardService.getIncidentsByDeclarantId(userId);
            Map<StatutIncident, Long> statusCounts = incidentDashboardService.countIncidentsByStatutForDeclarantId(userId);

            log.info("Fetched {} incidents for user ID: {}", incidents.size(), userId);

            // Add data to model
            model.addAttribute("incidents", incidents);
            model.addAttribute("signaleCount", statusCounts.getOrDefault(StatutIncident.SIGNALE, 0L));
            model.addAttribute("prisEnChargeCount", statusCounts.getOrDefault(StatutIncident.PRIS_EN_CHARGE, 0L));
            model.addAttribute("enResolutionCount", statusCounts.getOrDefault(StatutIncident.EN_RESOLUTION, 0L));
            model.addAttribute("resoluCount", statusCounts.getOrDefault(StatutIncident.RESOLU, 0L));
            model.addAttribute("clotureCount", statusCounts.getOrDefault(StatutIncident.CLOTURE, 0L));

            // Log detailed incident info for debugging
            incidents.forEach(inc -> log.debug("Incident: ID={}, Title={}, Status={}, Declarant ID={}",
                    inc.getId(), inc.getTitre(), inc.getStatut(),
                    inc.getDeclarant() != null ? inc.getDeclarant().getId() : "NULL"));

        } catch (Exception e) {
            log.error("Error loading incidents for user {}: {}", userEmail, e.getMessage(), e);
            // Set empty values on error
            model.addAttribute("incidents", java.util.Collections.emptyList());
            model.addAttribute("signaleCount", 0L);
            model.addAttribute("prisEnChargeCount", 0L);
            model.addAttribute("enResolutionCount", 0L);
            model.addAttribute("resoluCount", 0L);
            model.addAttribute("clotureCount", 0L);
        }

        return "citizen/dashboard";
    }

    @GetMapping("/incident/{id}")
    public String showIncidentDetails(@PathVariable Long id, Principal principal, Model model) {
        log.info("Incident details accessed for ID: {}", id);

        if (principal == null) {
            log.warn("No principal found, redirecting to login");
            return "redirect:/login-page";
        }

        // Get user from database
        String userEmail = principal.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        // Add user info to model
        model.addAttribute("userRole", currentUser.getRole().name());
        model.addAttribute("userName", currentUser.getNom() + " " + currentUser.getPrenom());
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("pageTitle", "Incident Details");
        // FIXED: Keep dashboard active since we're viewing from dashboard
        model.addAttribute("activePage", "dashboard");

        try {
            // Get incident and verify it belongs to the user
            Incident incident = incidentDashboardService.getIncidentByIdAndDeclarantId(id, currentUser.getId());
            model.addAttribute("incident", incident);
            log.info("Loaded incident {} for user {}", id, currentUser.getId());
        } catch (Exception e) {
            log.error("Error loading incident {}: {}", id, e.getMessage(), e);
            return "redirect:/citizen/dashboard";
        }

        return "citizen/incident-details";
    }

    @GetMapping("/incidents/new")
    public String newIncident() {
        log.info("Redirecting to new incident form");
        return "redirect:/citizen/incidents";
    }


    /**
     * Display citizen home page
     */
    @GetMapping("/home")
    public String citizenHome(HttpSession session, Model model) {
        log.info("Loading citizen home page");

        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Citizen Dashboard");

        log.info("Citizen home loaded for user: {} ({})", userName, userRole);

        return "citizen/home";
    }

    /**
     * Display citizen's incidents with advanced filtering
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

        log.info("Loading citizen incidents page with filters - Status: {}, Department: {}, Gouvernorat: {}, " +
                        "Municipalite: {}, StartDate: {}, EndDate: {}",
                statut, departement, gouvernorat, municipalite, startDate, endDate);

        // ✅ Get logged-in citizen info
        String userEmail = (String) session.getAttribute("userEmail");
        User citizen = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "incidents");
        model.addAttribute("pageTitle", "My Incidents");

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

        // ✅ Apply filters - ONLY for this citizen's incidents
        Page<Incident> incidentPage;
        boolean hasFilters = statutEnum != null || departementEnum != null ||
                (gouvernorat != null && !gouvernorat.isEmpty()) ||
                (municipalite != null && !municipalite.isEmpty()) ||
                startDateTime != null || endDateTime != null;

        if (hasFilters) {
            // Filter incidents created by this citizen
            incidentPage = incidentService.filterIncidentsByDeclarant(
                    citizen.getId(), statutEnum, departementEnum, gouvernorat, municipalite,
                    startDateTime, endDateTime, pageable
            );
            log.info("Filtered incidents found for citizen {}: {}", citizen.getId(), incidentPage.getTotalElements());
        } else {
            // Get all incidents created by this citizen
            incidentPage = incidentService.getIncidentsByDeclarant(citizen.getId(), pageable);
            log.info("All incidents found for citizen {}: {}", citizen.getId(), incidentPage.getTotalElements());
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
        model.addAttribute("statuts", StatutIncident.values());
        model.addAttribute("departements", Departement.values());
        model.addAttribute("gouvernorats", incidentService.getAllGouvernorats());
        model.addAttribute("municipalites", incidentService.getAllMunicipalites());

        // ✅ Add statistics for this citizen
        model.addAttribute("totalIncidents", incidentPage.getTotalElements());
        model.addAttribute("signaleIncidents",
                incidentService.countIncidentsByDeclarantAndStatus(citizen.getId(), StatutIncident.SIGNALE));
        model.addAttribute("prisEnChargeIncidents",
                incidentService.countIncidentsByDeclarantAndStatus(citizen.getId(), StatutIncident.PRIS_EN_CHARGE));
        model.addAttribute("enResolutionIncidents",
                incidentService.countIncidentsByDeclarantAndStatus(citizen.getId(), StatutIncident.EN_RESOLUTION));
        model.addAttribute("resoluIncidents",
                incidentService.countIncidentsByDeclarantAndStatus(citizen.getId(), StatutIncident.RESOLU));
        model.addAttribute("clotureIncidents",
                incidentService.countIncidentsByDeclarantAndStatus(citizen.getId(), StatutIncident.CLOTURE));

        log.info("Citizen incidents page loaded for user: {} ({})", userName, userRole);

        return "citizen/incidents";
    }

    /**
     * Add or update citizen feedback/comment on incident
     */
    @PostMapping("/add-feedback")
    public String addFeedback(
            HttpSession session,
            @RequestParam Long incidentId,
            @RequestParam String commentaire,
            RedirectAttributes redirectAttributes) {

        log.info("Citizen request to add feedback to incident {}", incidentId);

        String userEmail = (String) session.getAttribute("userEmail");
        User citizen = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        try {
            // ✅ Verify the incident belongs to this citizen
            Incident incident = incidentService.getIncidentById(incidentId)
                    .orElseThrow(() -> new IllegalArgumentException("Incident not found"));

            if (incident.getDeclarant() == null || !incident.getDeclarant().getId().equals(citizen.getId())) {
                log.error("Citizen {} tried to add feedback to incident {} which they did not create",
                        citizen.getId(), incidentId);
                redirectAttributes.addFlashAttribute("error",
                        "You can only add feedback to your own incidents.");
                return "redirect:/citizen/incidents";
            }

            // ✅ Update commentaire
            incidentService.updateCitizenFeedback(incidentId, commentaire);

            redirectAttributes.addFlashAttribute("success",
                    "Your feedback has been added successfully.");
            log.info("Citizen {} added feedback to incident {}", citizen.getId(), incidentId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to add feedback: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while adding your feedback.");
            log.error("Unexpected error while adding feedback: {}", e.getMessage(), e);
        }

        return "redirect:/citizen/incidents";
    }
}
