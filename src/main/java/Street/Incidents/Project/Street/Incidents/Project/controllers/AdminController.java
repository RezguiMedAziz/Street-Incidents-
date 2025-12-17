package Street.Incidents.Project.Street.Incidents.Project.controllers;
import java.util.ArrayList;
import java.util.Map;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.services.AdminDashboardService;
import Street.Incidents.Project.Street.Incidents.Project.services.AdminService;
import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
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

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final IncidentService incidentService;
    private final AdminDashboardService adminDashboardService;

    /**
     * Display the admin home page
     */
    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {
        log.info("Loading admin home page");

        Object roleObj = session.getAttribute("userRole");
        String userRole = null;

        if (roleObj instanceof Role) {
            userRole = ((Role) roleObj).name();
        } else if (roleObj instanceof String) {
            userRole = (String) roleObj;
        } else {
            userRole = "GUEST";
        }

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Admin Dashboard");

        log.info("Admin home loaded for user: {} ({})", userName, userRole);

        return "admin/home";
    }

    /**
     * Display the admin dashboard with paginated and filtered users
     */
    @GetMapping("/dashboard")
    public String showDashboard(
            HttpSession session,
            Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "roleFilter", required = false) String roleFilter,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "error", required = false) String error) {

        log.info("Loading admin dashboard - page: {}, size: {}, roleFilter: {}", page, size, roleFilter);

        // ==========================
        // Session user info
        // ==========================
        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Admin Dashboard");

        // ==========================
        // User pagination & filtering
        // ==========================
        Pageable userPageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage;

        if (roleFilter != null && !roleFilter.isEmpty() && !roleFilter.equals("ALL")) {
            try {
                Role role = Role.valueOf(roleFilter);
                userPage = adminService.getUsersByRole(role, userPageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role filter: {}", roleFilter);
                userPage = adminService.getUsersPage(userPageable);
            }
        } else {
            userPage = adminService.getUsersPage(userPageable);
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("roleFilter", roleFilter);
        model.addAttribute("allRoles", Role.values());

        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);

        // ==========================
        // DASHBOARD STATISTICS
        // ==========================
        try {
            AdminDashboardService.DashboardStats stats = adminDashboardService.getDashboardStats();

            // KPIs
            model.addAttribute("totalIncidents", stats.getTotalIncidents());
            model.addAttribute("resolvedIncidents", stats.getResolvedIncidents());
            model.addAttribute("pendingIncidents", stats.getPendingIncidents());
            model.addAttribute("avgResolutionTime", String.format("%.1f", stats.getAvgResolutionTime()));
            model.addAttribute("resolutionRate", String.format("%.1f", stats.getResolutionRate()));
            model.addAttribute("activeAgents", stats.getActiveAgents());
            model.addAttribute("totalCitizens", stats.getTotalCitizens());

            // Graphique par Type (Categorie = Departement)
            Map<String, Long> typeMap = stats.getIncidentsByType();
            model.addAttribute("incidentTypeLabels", new ArrayList<>(typeMap.keySet()));
            model.addAttribute("incidentTypeData", new ArrayList<>(typeMap.values()));

            // Graphique par Statut
            Map<String, Long> statusMap = stats.getIncidentsByStatus();
            model.addAttribute("statusLabels", new ArrayList<>(statusMap.keySet()));
            model.addAttribute("statusData", new ArrayList<>(statusMap.values()));

            // Graphique par Priorité
            Map<String, Long> priorityMap = stats.getIncidentsByPriority();
            model.addAttribute("priorityLabels", new ArrayList<>(priorityMap.keySet()));
            model.addAttribute("priorityData", new ArrayList<>(priorityMap.values()));

            // Top 10 Quartiers
            Map<String, Long> quartierMap = stats.getTop10Quartiers();
            model.addAttribute("quartierLabels", new ArrayList<>(quartierMap.keySet()));
            model.addAttribute("quartierData", new ArrayList<>(quartierMap.values()));

            // Évolution temporelle (30 derniers jours)
            Map<String, Long> trendMap = stats.getIncidentsTrend();
            model.addAttribute("dateLabels", new ArrayList<>(trendMap.keySet()));
            model.addAttribute("dateData", new ArrayList<>(trendMap.values()));

            log.info("Dashboard statistics loaded successfully: {} incidents total", stats.getTotalIncidents());

        } catch (Exception e) {
            log.error("Erreur critique lors du chargement des statistiques dashboard", e);

            // Valeurs par défaut en cas d'erreur (pour éviter des charts cassés)
            model.addAttribute("totalIncidents", 0L);
            model.addAttribute("resolvedIncidents", 0L);
            model.addAttribute("pendingIncidents", 0L);
            model.addAttribute("avgResolutionTime", "0.0");
            model.addAttribute("resolutionRate", "0.0");
            model.addAttribute("activeAgents", 0L);
            model.addAttribute("totalCitizens", 0L);

            model.addAttribute("incidentTypeLabels", new ArrayList<>());
            model.addAttribute("incidentTypeData", new ArrayList<>());
            model.addAttribute("statusLabels", new ArrayList<>());
            model.addAttribute("statusData", new ArrayList<>());
            model.addAttribute("priorityLabels", new ArrayList<>());
            model.addAttribute("priorityData", new ArrayList<>());
            model.addAttribute("quartierLabels", new ArrayList<>());
            model.addAttribute("quartierData", new ArrayList<>());
            model.addAttribute("dateLabels", new ArrayList<>());
            model.addAttribute("dateData", new ArrayList<>());

            model.addAttribute("error", "Impossible de charger les statistiques. Réessayez plus tard.");
        }
        log.info("Admin dashboard loaded for user: {} ({})", userName, userRole);

        return "admin/dashboard";
    }


    @PostMapping("/create-user")
    public String createUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            RedirectAttributes redirectAttributes) {

        log.info("Request to create user with email: {} and role: {}", email, role);

        try {
            User user = adminService.createUser(email, password, role, nom, prenom);

            redirectAttributes.addFlashAttribute("success",
                    "User created successfully. Account credentials have been sent to " + email + ".");
            log.info("User created successfully: {}", email);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to create user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while creating the user.");
            log.error("Unexpected error while creating user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/update-user")
    public String updateUser(
            @RequestParam Long userId,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam String email,
            @RequestParam Role role,
            @RequestParam(required = false) String newPassword,
            @RequestParam(defaultValue = "false") boolean sendNotification,
            RedirectAttributes redirectAttributes) {

        log.info("Request to update user ID: {} with notification: {}", userId, sendNotification);

        try {
            User updatedUser = adminService.updateUserWithNotification(
                    userId, nom, prenom, email, role, newPassword, sendNotification
            );

            String successMessage = "User updated successfully.";
            if (sendNotification) {
                successMessage += " Notification email has been sent to " + email + ".";
            }

            redirectAttributes.addFlashAttribute("success", successMessage);
            log.info("User updated successfully with ID: {}", userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to update user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the user.");
            log.error("Unexpected error while updating user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/change-role")
    public String changeRole(
            @RequestParam Long userId,
            @RequestParam Role newRole,
            @RequestParam(defaultValue = "false") boolean sendNotification,
            RedirectAttributes redirectAttributes) {

        log.info("Request to change role for user ID: {} to {} with notification: {}",
                userId, newRole, sendNotification);

        try {
            User user = adminService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            if (!user.getRole().equals(newRole)) {
                User updatedUser = adminService.updateUserWithNotification(
                        userId,
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        newRole,
                        null,
                        sendNotification
                );

                String successMessage = "User role updated successfully.";
                if (sendNotification) {
                    successMessage += " Notification email has been sent to " + user.getEmail() + ".";
                }

                redirectAttributes.addFlashAttribute("success", successMessage);
                log.info("User role updated successfully for user ID: {}", userId);
            } else {
                redirectAttributes.addFlashAttribute("success", "User already has this role.");
                log.info("User already has the requested role: {}", newRole);
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to change user role: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the user role.");
            log.error("Unexpected error while changing user role: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/delete-user")
    public String deleteUser(
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("Request to delete user with ID: {}", userId);

        try {
            adminService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
            log.info("User deleted successfully with ID: {}", userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to delete user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while deleting the user.");
            log.error("Unexpected error while deleting user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    // ========================================
    // ✅ INCIDENT MANAGEMENT WITH ADVANCED FILTERS
    // ========================================

    /**
     * Display incidents management page with advanced filtering
     */
    @GetMapping("/incidents")
    public String showIncidents(
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
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Loading incidents page with filters - Status: {}, Department: {}, Gouvernorat: {}, " +
                        "Municipalite: {}, AgentId: {}, StartDate: {}, EndDate: {}",
                statut, departement, gouvernorat, municipalite, agentId, startDate, endDate);

        // ✅ Session handling
        Object roleObj = session.getAttribute("userRole");
        String userRole = roleObj instanceof Role ? ((Role) roleObj).name() :
                roleObj instanceof String ? (String) roleObj : "GUEST";

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "incidents");
        model.addAttribute("pageTitle", "Manage Incidents");

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

        // ✅ Apply filters
        Page<Incident> incidentPage;
        boolean hasFilters = statutEnum != null || departementEnum != null ||
                (gouvernorat != null && !gouvernorat.isEmpty()) ||
                (municipalite != null && !municipalite.isEmpty()) ||
                agentId != null || startDateTime != null || endDateTime != null;

        if (hasFilters) {
            incidentPage = incidentService.filterIncidents(
                    statutEnum, departementEnum, gouvernorat, municipalite,
                    agentId, startDateTime, endDateTime, pageable
            );
            log.info("Filtered incidents found: {}", incidentPage.getTotalElements());
        } else {
            incidentPage = incidentService.getAllIncidents(pageable);
            log.info("All incidents found: {}", incidentPage.getTotalElements());
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
        model.addAttribute("selectedAgentId", agentId);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);

        // ✅ ADD FILTER OPTIONS FOR DROPDOWNS
        model.addAttribute("statuts", StatutIncident.values());
        model.addAttribute("departements", Departement.values());
        model.addAttribute("gouvernorats", incidentService.getAllGouvernorats());
        model.addAttribute("municipalites", incidentService.getAllMunicipalites());
        model.addAttribute("agents", adminService.getUsersByRoleList(Role.AGENT_MUNICIPAL));

        // ✅ Add statistics
        model.addAttribute("totalIncidents", incidentService.countTotalIncidents());
        model.addAttribute("unassignedIncidents", incidentService.countUnassignedIncidents());
        model.addAttribute("signaleIncidents", incidentService.countIncidentsByStatus(StatutIncident.SIGNALE));
        model.addAttribute("enCoursIncidents",
                incidentService.countIncidentsByStatus(StatutIncident.PRIS_EN_CHARGE) +
                        incidentService.countIncidentsByStatus(StatutIncident.EN_RESOLUTION));

        log.info("Incidents page loaded for user: {} ({})", userName, userRole);

        return "admin/incidents";
    }

    /**
     * Assign incident to agent
     */
    @PostMapping("/assign-incident")
    public String assignIncident(
            @RequestParam Long incidentId,
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "true") boolean sendNotification,
            RedirectAttributes redirectAttributes) {

        log.info("Request to assign incident {} to agent {} with notification: {}", incidentId, agentId, sendNotification);

        try {
            incidentService.assignIncidentToAgent(incidentId, agentId, sendNotification);

            String successMessage = "Incident assigned successfully.";
            if (sendNotification) {
                successMessage += " Notification emails have been sent to the agent and citizen.";
            }

            redirectAttributes.addFlashAttribute("success", successMessage);
            log.info("Incident {} assigned successfully to agent {}", incidentId, agentId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to assign incident: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while assigning the incident.");
            log.error("Unexpected error while assigning incident: {}", e.getMessage(), e);
        }

        return "redirect:/admin/incidents";
    }

    /**
     * Update incident status
     */
    @PostMapping("/update-incident-status")
    public String updateIncidentStatus(
            @RequestParam Long incidentId,
            @RequestParam StatutIncident newStatus,
            RedirectAttributes redirectAttributes) {

        log.info("Request to update incident {} status to {}", incidentId, newStatus);

        try {
            incidentService.updateIncidentStatus(incidentId, newStatus);
            redirectAttributes.addFlashAttribute("success",
                    "Incident status updated successfully. Notification sent to citizen.");
            log.info("Incident {} status updated to {}", incidentId, newStatus);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to update incident status: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the incident status.");
            log.error("Unexpected error while updating incident status: {}", e.getMessage(), e);
        }

        return "redirect:/admin/incidents";
    }

    // ========================================
    // ✅ NEW: CLOSE INCIDENT AFTER READING CITIZEN FEEDBACK
    // ========================================

    /**
     * Close incident (set status to CLOTURE)
     * Only works for incidents with status RESOLU
     * Should be called after admin reads citizen feedback
     */
    @PostMapping("/close-incident")
    public String closeIncident(
            @RequestParam("incidentId") Long incidentId,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Admin request to close incident with ID: {}", incidentId);

            // 1️⃣ Verify incident exists and is RESOLU
            Incident incident = incidentService.getIncidentById(incidentId)
                    .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + incidentId));

            // 2️⃣ Check current status
            if (incident.getStatut() != StatutIncident.RESOLU) {
                log.warn("Attempt to close incident {} with status {}", incidentId, incident.getStatut());
                redirectAttributes.addFlashAttribute("error",
                        "Only RESOLVED incidents can be closed. Current status: " + incident.getStatut());
                return "redirect:/admin/incidents";
            }

            // 3️⃣ Update status to CLOTURE (email sent automatically in service)
            incidentService.updateIncidentStatus(incidentId, StatutIncident.CLOTURE);

            // 4️⃣ Success message
            redirectAttributes.addFlashAttribute("success",
                    "Incident #" + incidentId + " has been successfully closed. Citizen has been notified by email.");

            log.info("Incident {} closed successfully by admin", incidentId);

        } catch (IllegalArgumentException e) {
            log.error("Incident not found: {}", incidentId);
            redirectAttributes.addFlashAttribute("error", "Incident not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to close incident {}: {}", incidentId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to close incident: " + e.getMessage());
        }

        return "redirect:/admin/incidents";
    }
}