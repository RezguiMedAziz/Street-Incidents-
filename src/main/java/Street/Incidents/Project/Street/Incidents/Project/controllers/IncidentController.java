package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Departement;
import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Priorite;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.repositories.QuartierRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.incident.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private QuartierRepository quartierRepository;

    @GetMapping("/nouveau")
    public String showForm(Model model) {
        model.addAttribute("incident", new Incident());
        model.addAttribute("categories", Departement.values());
        model.addAttribute("priorites", Priorite.values());
        return "incident/incidents";
    }

    @PostMapping("/nouveau")
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
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Veuillez corriger les erreurs dans le formulaire.");
            model.addAttribute("categories", Departement.values());
            model.addAttribute("priorites", Priorite.values());
            return "incident/incidents";
        }

        try {
            incidentService.saveIncident(
                    incident,
                    principal.getName(),
                    gouvernorat,
                    municipalite,
                    photosFiles
            );

            // Pour le dashboard
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("alertMessage",
                    "Incident signalé avec succès ! Votre déclaration a été enregistrée.");

            return "redirect:/dashboard";

        } catch (Exception e) {
            e.printStackTrace();

            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage",
                    "Erreur lors de l'enregistrement de l'incident : " + e.getMessage());
            model.addAttribute("categories", Departement.values());
            model.addAttribute("priorites", Priorite.values());

            return "incident/incidents";
        }
    }
}