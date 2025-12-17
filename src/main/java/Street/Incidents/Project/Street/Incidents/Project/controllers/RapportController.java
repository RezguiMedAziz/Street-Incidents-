package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.services.RapportService;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/citizen/export")
@RequiredArgsConstructor
public class RapportController {

    private final RapportService rapportService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Test endpoint to verify controller is working
     */
    @GetMapping("/test")
    public String test() {
        try {
            User user = getCurrentUser();
            return "Export Controller is working! User: " + user.getEmail() +
                    " | Role: " + user.getRole().name();
        } catch (Exception e) {
            return "Export Controller is working! But error getting user: " + e.getMessage();
        }
    }

    /**
     * Export incident to PDF
     */
    @GetMapping("/incident/{id}/pdf")
    public ResponseEntity<byte[]> exporterIncidentPDF(@PathVariable Long id) {

        System.out.println("=== PDF Export Called ===");
        System.out.println("Incident ID: " + id);

        try {
            // Get current user
            User currentUser = getCurrentUser();
            System.out.println("Current User: " + currentUser.getEmail());

            // Get incident
            Incident incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Incident non trouve"));

            System.out.println("Incident found: " + incident.getTitre());

            // Check if user is the owner
            if (incident.getDeclarant() == null) {
                System.out.println("ERROR: Declarant is NULL");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            System.out.println("Declarant: " + incident.getDeclarant().getEmail());

            if (!incident.getDeclarant().getId().equals(currentUser.getId())) {
                System.out.println("ERROR: User is not the owner");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            System.out.println("Generating PDF...");

            // Generate PDF
            byte[] pdfBytes = rapportService.genererRapportPDF(incident, currentUser);

            System.out.println("PDF Generated successfully. Size: " + pdfBytes.length + " bytes");

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "incident_" + id + "_" + System.currentTimeMillis() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (DocumentException e) {
            System.err.println("ERROR: DocumentException - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("ERROR: Exception - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export incident to CSV
     */
    @GetMapping("/incident/{id}/csv")
    public ResponseEntity<String> exporterIncidentCSV(@PathVariable Long id) {

        System.out.println("=== CSV Export Called ===");
        System.out.println("Incident ID: " + id);

        try {
            // Get current user
            User currentUser = getCurrentUser();
            System.out.println("Current User: " + currentUser.getEmail());

            // Get incident
            Incident incident = incidentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Incident non trouve"));

            System.out.println("Incident found: " + incident.getTitre());

            // Check if user is the owner
            if (incident.getDeclarant() == null) {
                System.out.println("ERROR: Declarant is NULL");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            System.out.println("Declarant: " + incident.getDeclarant().getEmail());

            if (!incident.getDeclarant().getId().equals(currentUser.getId())) {
                System.out.println("ERROR: User is not the owner");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            System.out.println("Generating CSV...");

            // Generate CSV
            String csvContent = rapportService.genererRapportCSV(incident, currentUser);

            System.out.println("CSV Generated successfully. Length: " + csvContent.length() + " chars");

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment",
                    "incident_" + id + "_" + System.currentTimeMillis() + ".csv");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("ERROR: Exception - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}