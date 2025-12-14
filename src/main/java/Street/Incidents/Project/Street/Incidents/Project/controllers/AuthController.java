package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.AuthResponse;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.LoginRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.RegisterRequest;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.UserRepository;
import Street.Incidents.Project.Street.Incidents.Project.security.JwtUtil;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        System.out.println("DEBUG: Register endpoint called");
        AuthResponse response = userService.register(request);
        System.out.println("DEBUG: Registration successful, token generated");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        System.out.println("DEBUG: Login endpoint called for: " + request.getEmail());
        AuthResponse response = userService.login(request);
        System.out.println("DEBUG: Login successful, token generated");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("code") String code) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean verified = userService.verifyEmail(code);
            if (verified) {
                response.put("success", true);
                response.put("message", "Email verified successfully!");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", false);
        response.put("message", "Verification failed");
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean resent = userService.resendVerificationEmail(email);
            if (resent) {
                response.put("success", true);
                response.put("message", "Verification email resent successfully!");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", false);
        response.put("message", "Failed to resend verification email");
        return ResponseEntity.badRequest().body(response);
    }
    @GetMapping("/verify-email")
    public void verifyEmailGet(@RequestParam("code") String code,
                               HttpServletResponse response) throws IOException, IOException {
        // Redirect to the web page endpoint
        response.sendRedirect("/verify-email?code=" + code);
    }


}