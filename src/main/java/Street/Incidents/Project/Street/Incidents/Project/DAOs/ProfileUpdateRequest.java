package Street.Incidents.Project.Street.Incidents.Project.DAOs;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nom;
    private String prenom;
    private String email;
}