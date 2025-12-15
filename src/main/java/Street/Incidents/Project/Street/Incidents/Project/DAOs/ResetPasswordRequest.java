package Street.Incidents.Project.Street.Incidents.Project.DAOs;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
    private String confirmPassword;
}