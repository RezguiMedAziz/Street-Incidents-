package Street.Incidents.Project.Street.Incidents.Project.services.incident;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.StatutIncident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Quartier;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.IncidentRepository;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuartierService quartierService;

    @Autowired
    private PhotoService photoService;

    @Transactional
    public void saveIncident(
            Incident incident,
            String userEmail,
            String gouvernorat,
            String municipalite,
            MultipartFile[] photos
    ) throws IOException {

        // 1️⃣ User
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        incident.setDeclarant(user);

        // 2️⃣ Quartier
        Quartier quartier = quartierService.findOrCreateQuartier(
                gouvernorat, municipalite
        );
        incident.setQuartier(quartier);

        // 3️⃣ Business fields
        incident.setDateCreation(LocalDateTime.now());
        incident.setStatut(StatutIncident.SIGNALE);

        // 4️⃣ Save incident first
        Incident savedIncident = incidentRepo.save(incident);

        // 5️⃣ Save photos
        if (photos != null && photos.length > 0) {
            photoService.savePhotos(photos, savedIncident);
        }
    }
}