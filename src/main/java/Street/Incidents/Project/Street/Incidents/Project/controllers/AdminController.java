package Street.Incidents.Project.Street.Incidents.Project.controllers;

import Street.Incidents.Project.Street.Incidents.Project.entities.Enums.Role;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    /**
     * Display the admin home page
     */
    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {
        log.info("Loading admin home page");

        Object roleObj = session.getAttribute("userRole");
        String userRole = null;

        if (roleObj instanceof Role) {
            userRole = ((Role) roleObj).name();
        } else if (roleObj instanceof String) {
            userRole = (String) roleObj;
        } else {
            userRole = "GUEST";
        }

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Admin Dashboard");

        log.info("Admin home loaded for user: {} ({})", userName, userRole);

        return "admin/home";
    }

    /**
     * Display the admin dashboard with paginated and filtered users
     */
    @GetMapping("/dashboard")
    public String showDashboard(
            HttpSession session,
            Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "roleFilter", required = false) String roleFilter,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "error", required = false) String error) {

        log.info("Loading admin dashboard - page: {}, size: {}, roleFilter: {}", page, size, roleFilter);

        // Session handling
        Object roleObj = session.getAttribute("userRole");
        String userRole = null;

        if (roleObj instanceof Role) {
            userRole = ((Role) roleObj).name();
            log.debug("User role retrieved as enum: {}", userRole);
        } else if (roleObj instanceof String) {
            userRole = (String) roleObj;
            log.debug("User role retrieved as string: {}", userRole);
        } else {
            log.warn("User role is null or unexpected type");
            userRole = "GUEST";
        }

        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");

        model.addAttribute("userRole", userRole);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("activePage", "users");
        model.addAttribute("pageTitle", "Manage Users");

        log.info("Dashboard loaded for user: {} ({})", userName, userRole);

        // Fetch paginated users sorted by ID with optional role filter
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage;

        if (roleFilter != null && !roleFilter.isEmpty() && !roleFilter.equals("ALL")) {
            try {
                Role role = Role.valueOf(roleFilter);
                userPage = adminService.getUsersByRole(role, pageable);
                log.info("Filtered users by role: {}", roleFilter);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role filter: {}", roleFilter);
                userPage = adminService.getUsersPage(pageable);
            }
        } else {
            userPage = adminService.getUsersPage(pageable);
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("roleFilter", roleFilter);

        // Add all Role enum values for dropdowns
        model.addAttribute("allRoles", Role.values());

        // Handle success and error messages
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }

        return "admin/dashboard";
    }

    @PostMapping("/create-user")
    public String createUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            RedirectAttributes redirectAttributes) {

        log.info("Request to create user with email: {} and role: {}", email, role);

        try {
            User user = adminService.createUser(email, password, role, nom, prenom);

            redirectAttributes.addFlashAttribute("success",
                    "User created successfully. Account credentials have been sent to " + email + ".");
            log.info("User created successfully: {}", email);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to create user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while creating the user.");
            log.error("Unexpected error while creating user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/update-user")
    public String updateUser(
            @RequestParam Long userId,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam String email,
            @RequestParam Role role,
            @RequestParam(required = false) String newPassword,
            @RequestParam(defaultValue = "false") boolean sendNotification,
            RedirectAttributes redirectAttributes) {

        log.info("Request to update user ID: {} with notification: {}", userId, sendNotification);

        try {
            // Use the new method that supports email notifications
            User updatedUser = adminService.updateUserWithNotification(
                    userId, nom, prenom, email, role, newPassword, sendNotification
            );

            String successMessage = "User updated successfully.";
            if (sendNotification) {
                successMessage += " Notification email has been sent to " + email + ".";
            }

            redirectAttributes.addFlashAttribute("success", successMessage);
            log.info("User updated successfully with ID: {}", userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to update user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the user.");
            log.error("Unexpected error while updating user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/change-role")
    public String changeRole(
            @RequestParam Long userId,
            @RequestParam Role newRole,
            @RequestParam(defaultValue = "false") boolean sendNotification,
            RedirectAttributes redirectAttributes) {

        log.info("Request to change role for user ID: {} to {} with notification: {}",
                userId, newRole, sendNotification);

        try {
            // Get the user first to get current details
            User user = adminService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // Check if role is actually changing
            if (!user.getRole().equals(newRole)) {
                // Update the user with notification
                User updatedUser = adminService.updateUserWithNotification(
                        userId,
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail(),
                        newRole,
                        null, // No password change
                        sendNotification
                );

                String successMessage = "User role updated successfully.";
                if (sendNotification) {
                    successMessage += " Notification email has been sent to " + user.getEmail() + ".";
                }

                redirectAttributes.addFlashAttribute("success", successMessage);
                log.info("User role updated successfully for user ID: {}", userId);
            } else {
                redirectAttributes.addFlashAttribute("success", "User already has this role.");
                log.info("User already has the requested role: {}", newRole);
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to change user role: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the user role.");
            log.error("Unexpected error while changing user role: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/delete-user")
    public String deleteUser(
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("Request to delete user with ID: {}", userId);

        try {
            adminService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
            log.info("User deleted successfully with ID: {}", userId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            log.error("Failed to delete user: {}", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while deleting the user.");
            log.error("Unexpected error while deleting user: {}", e.getMessage(), e);
        }

        return "redirect:/admin/dashboard";
    }
}