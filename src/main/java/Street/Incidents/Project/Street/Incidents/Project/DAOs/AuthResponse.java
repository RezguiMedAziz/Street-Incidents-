package Street.Incidents.Project.Street.Incidents.Project.DAOs;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String nom;
    private String prenom;
    private String message;
    private Role role;}
