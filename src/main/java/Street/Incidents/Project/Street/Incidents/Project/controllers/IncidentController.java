package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.QuartierRepository;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/incidents")  // ✅ CHANGÉ: /incidents au lieu de /citizen/incidents
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private QuartierRepository quartierRepository;

    @Autowired
    private UserRepository utilisateurRepository;

    // ✅ GET /incidents/new (au lieu de /citizen/incidents)
    @GetMapping("/new")
    public String showForm(Model model, Principal principal) {
        // Add user session attributes for sidebar
        addUserSessionAttributes(model, principal);

        // Add form data
        model.addAttribute("incident", new Incident());
        model.addAttribute("categories", Departement.values());
        model.addAttribute("priorites", Priorite.values());

        // Set active page for sidebar highlighting
        model.addAttribute("activePage", "incidents");

        return "citizen/incidents"; // Thymeleaf template
    }

    // ✅ POST /incidents/submit (au lieu de /citizen/incidents)
    @PostMapping("/submit")
    public String saveIncident(
            @Valid @ModelAttribute Incident incident,
            BindingResult result,
            @RequestParam(value = "photosFiles", required = false) MultipartFile[] photosFiles,
            @RequestParam(value = "gouvernorat", required = false) String gouvernorat,
            @RequestParam(value = "municipalite", required = false) String municipalite,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            // Re-add user session attributes for sidebar
            addUserSessionAttributes(model, principal);

            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Veuillez corriger les erreurs dans le formulaire.");
            model.addAttribute("categories", Departement.values());
            model.addAttribute("priorites", Priorite.values());
            model.addAttribute("activePage", "incidents");

            return "citizen/incidents";
        }

        try {
            incidentService.saveIncident(
                    incident,
                    principal.getName(),
                    gouvernorat,
                    municipalite,
                    photosFiles
            );

            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage",
                    "Incident signalé avec succès ! Votre déclaration a été enregistrée.");

            return "redirect:/citizen/dashboard";
            // ✅ REDIRECT TO /home AFTER CREATING INCIDENT

        } catch (Exception e) {
            e.printStackTrace();

            // Re-add user session attributes for sidebar
            addUserSessionAttributes(model, principal);

            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage",
                    "Erreur lors de l'enregistrement de l'incident : " + e.getMessage());
            model.addAttribute("categories", Departement.values());
            model.addAttribute("priorites", Priorite.values());
            model.addAttribute("activePage", "incidents");

            return "citizen/incidents";
        }
    }

    /**
     * Helper method to add user session attributes to the model
     * This ensures the sidebar displays correctly with role-based menus
     */
    private void addUserSessionAttributes(Model model, Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            User user = utilisateurRepository.findByEmail(email).orElse(null);

            if (user != null) {
                model.addAttribute("userRole", user.getRole().name());
                model.addAttribute("userName", user.getNom() + " " + user.getPrenom());
                model.addAttribute("userEmail", user.getEmail());
            } else {
                // Fallback values
                model.addAttribute("userRole", "GUEST");
                model.addAttribute("userName", "Guest");
                model.addAttribute("userEmail", email);
            }
        } else {
            // Fallback values when not authenticated
            model.addAttribute("userRole", "GUEST");
            model.addAttribute("userName", "Guest");
            model.addAttribute("userEmail", "");
        }
    }
}
