package Street.Incidents.Project.Street.Incidents.Project.DAOs;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private Role role;
}

