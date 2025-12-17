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

@Controller
@RequestMapping("/citizen")
@RequiredArgsConstructor
@Slf4j
public class CitizenController {

    private final IncidentDashboardService incidentService;
    private final UserRepository userRepository;

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
            List<Incident> incidents = incidentService.getIncidentsByDeclarantId(userId);
            Map<StatutIncident, Long> statusCounts = incidentService.countIncidentsByStatutForDeclarantId(userId);

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

    @GetMapping("/my-incidents")
    public String myIncidents(Principal principal, Model model) {
        log.info("Citizen incidents list page accessed");

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
        model.addAttribute("pageTitle", "My Incidents");
        model.addAttribute("activePage", "incidents");

        try {
            List<Incident> userIncidents = incidentService.getIncidentsByDeclarantId(currentUser.getId());
            model.addAttribute("incidents", userIncidents);
            log.info("Loaded {} incidents for incidents list page", userIncidents.size());
        } catch (Exception e) {
            log.error("Error loading incidents: {}", e.getMessage(), e);
            model.addAttribute("incidents", java.util.Collections.emptyList());
        }

        return "citizen/incidents-list";
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
            Incident incident = incidentService.getIncidentByIdAndDeclarantId(id, currentUser.getId());
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
}