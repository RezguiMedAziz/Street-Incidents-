//package Street.Incidents.Project.Street.Incidents.Project.controllers;
//
//import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
//import Street.Incidents.Project.Street.Incidents.Project.entities.User;
//import Street.Incidents.Project.Street.Incidents.Project.services.AgentDashboardService;
//import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.util.List;
//import java.util.Map;
//
//@Controller
//@RequestMapping("/agent")
//@RequiredArgsConstructor
//@Slf4j
//public class AgentDashboardController {
//
//    private final AgentDashboardService dashboardService;
//    private final UserService userService;
//
//    /**
//     * Display agent dashboard with assigned incidents
//     */
//    @GetMapping("/dashboard")
//    public String showDashboard(Authentication authentication, Model model) {
//        try {
//            // Get current authenticated user using existing UserService method
//            String email = authentication.getName();
//            User currentUser = userService.getUserByEmail(email);
//
//            log.info("Loading dashboard for agent: {} (ID: {})", currentUser.getNom(), currentUser.getId());
//
//            // Get all incidents assigned to this agent
//            List<Incident> assignedIncidents = dashboardService.getIncidentsAssignedToAgent(currentUser.getId());
//
//            // Get statistics
//            Map<String, Long> statistics = dashboardService.getAgentStatistics(currentUser.getId());
//
//            // Add data to model
//            model.addAttribute("userName", currentUser.getNom());
//            model.addAttribute("userEmail", currentUser.getEmail());
//            model.addAttribute("userRole", currentUser.getRole().name());
//            model.addAttribute("incidents", assignedIncidents);
//            model.addAttribute("totalIncidents", statistics.get("total"));
//            model.addAttribute("criticalCount", statistics.get("critical"));
//            model.addAttribute("highCount", statistics.get("high"));
//            model.addAttribute("mediumCount", statistics.get("medium"));
//            model.addAttribute("lowCount", statistics.get("low"));
//            model.addAttribute("inProgressCount", statistics.get("inProgress"));
//            model.addAttribute("activePage", "dashboard");
//
//            log.info("Dashboard loaded with {} incidents", assignedIncidents.size());
//
//            return "agent/dashboard";
//
//        } catch (Exception e) {
//            log.error("Error loading agent dashboard", e);
//            model.addAttribute("error", "Erreur lors du chargement du tableau de bord");
//            return "error";
//        }
//    }
//
//    /**
//     * Display all incidents assigned to agent
//     */
//    @GetMapping("/incidents")
//    public String showIncidents(Authentication authentication, Model model) {
//        try {
//            String email = authentication.getName();
//            User currentUser = userService.getUserByEmail(email);
//
//            List<Incident> assignedIncidents = dashboardService.getIncidentsAssignedToAgent(currentUser.getId());
//            Map<String, Long> statistics = dashboardService.getAgentStatistics(currentUser.getId());
//
//            model.addAttribute("userName", currentUser.getNom());
//            model.addAttribute("userEmail", currentUser.getEmail());
//            model.addAttribute("userRole", currentUser.getRole().name());
//            model.addAttribute("incidents", assignedIncidents);
//            model.addAttribute("totalIncidents", statistics.get("total"));
//            model.addAttribute("activePage", "incidents");
//
//            return "agent/incidents";
//
//        } catch (Exception e) {
//            log.error("Error loading agent incidents", e);
//            model.addAttribute("error", "Erreur lors du chargement des incidents");
//            return "error";
//        }
//    }
//
//    /**
//     * Display specific incident details
//     */
//    @GetMapping("/incident/{id}")
//    public String showIncidentDetail(@PathVariable Long id,
//                                     Authentication authentication,
//                                     Model model) {
//        try {
//            String email = authentication.getName();
//            User currentUser = userService.getUserByEmail(email);
//
//            Incident incident = dashboardService.getIncidentById(id);
//
//            // Verify that this incident is assigned to current agent
//            if (incident.getAgent() == null ||
//                    !incident.getAgent().getId().equals(currentUser.getId())) {
//                log.warn("Agent {} tried to access incident {} not assigned to them",
//                        currentUser.getId(), id);
//                model.addAttribute("error", "Vous n'avez pas accès à cet incident");
//                return "error";
//            }
//
//            model.addAttribute("userName", currentUser.getNom());
//            model.addAttribute("userEmail", currentUser.getEmail());
//            model.addAttribute("userRole", currentUser.getRole().name());
//            model.addAttribute("incident", incident);
//            model.addAttribute("activePage", "incidents");
//
//            return "agent/incident-detail";
//
//        } catch (Exception e) {
//            log.error("Error loading incident detail: {}", id, e);
//            model.addAttribute("error", "Incident non trouvé");
//            return "error";
//        }
//    }
//
//    /**
//     * Display agent statistics page
//     */
//    @GetMapping("/statistics")
//    public String showStatistics(Authentication authentication, Model model) {
//        try {
//            String email = authentication.getName();
//            User currentUser = userService.getUserByEmail(email);
//
//            Map<String, Long> statistics = dashboardService.getAgentStatistics(currentUser.getId());
//            Map<String, Long> categoryStats = dashboardService.getCategoryStatistics(currentUser.getId());
//            Map<String, Long> statusStats = dashboardService.getStatusStatistics(currentUser.getId());
//
//            model.addAttribute("userName", currentUser.getNom());
//            model.addAttribute("userEmail", currentUser.getEmail());
//            model.addAttribute("userRole", currentUser.getRole().name());
//            model.addAttribute("statistics", statistics);
//            model.addAttribute("categoryStats", categoryStats);
//            model.addAttribute("statusStats", statusStats);
//            model.addAttribute("activePage", "statistics");
//
//            return "agent/statistics";
//
//        } catch (Exception e) {
//            log.error("Error loading agent statistics", e);
//            model.addAttribute("error", "Erreur lors du chargement des statistiques");
//            return "error";
//        }
//    }
//}