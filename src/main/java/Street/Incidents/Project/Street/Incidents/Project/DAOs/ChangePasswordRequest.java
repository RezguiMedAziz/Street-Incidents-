package Street.Incidents.Project.Street.Incidents.Project.DAOs;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}