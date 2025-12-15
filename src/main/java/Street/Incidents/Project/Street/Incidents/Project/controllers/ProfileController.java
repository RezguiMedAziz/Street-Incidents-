package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.DAOs.ProfileUpdateRequest;
import Street.Incidents.Project.Street.Incidents.Project.DAOs.ChangePasswordRequest;
import Street.Incidents.Project.Street.Incidents.Project.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        log.info("=== PROFILE PAGE ACCESS ATTEMPT ===");

        if (session == null || session.getAttribute("token") == null) {
            log.warn("No session or token found, redirecting to login");
            return "redirect:/login-page?error";
        }

        String email = (String) session.getAttribute("userEmail");

        try {
            // Get user details
            var user = userService.getUserByEmail(email);
            model.addAttribute("userDetails", user);

            // Add profile update request for form
            model.addAttribute("profileUpdateRequest", new ProfileUpdateRequest());

            // Add change password request for form
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());

        } catch (Exception e) {
            log.error("Error getting user details: {}", e.getMessage());
            model.addAttribute("error", "Unable to load profile details");
        }

        // Set active page for sidebar highlighting
        model.addAttribute("activePage", "profile");
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("pageTitle", "Profile - Street Incidents");

        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest profileUpdateRequest,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("token") == null) {
            return "redirect:/login-page?error";
        }

        String currentEmail = (String) session.getAttribute("userEmail");

        try {
            // Update user profile
            userService.updateUserProfile(currentEmail, profileUpdateRequest);

            // Update session attributes if email changed
            if (profileUpdateRequest.getEmail() != null &&
                    !profileUpdateRequest.getEmail().isEmpty() &&
                    !profileUpdateRequest.getEmail().equals(currentEmail)) {

                session.setAttribute("userEmail", profileUpdateRequest.getEmail());
            }

            // Update name in session
            String newFullName = profileUpdateRequest.getNom() + " " + profileUpdateRequest.getPrenom();
            session.setAttribute("userName", newFullName);

            log.info("Profile updated successfully for user: {}", currentEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");

        } catch (Exception e) {
            log.error("Profile update failed for {}: {}", currentEmail, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}