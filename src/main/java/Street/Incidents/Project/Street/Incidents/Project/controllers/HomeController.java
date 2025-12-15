package Street.Incidents.Project.Street.Incidents.Project.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController extends BaseController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login-page";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        log.info("=== DASHBOARD ACCESS ATTEMPT ===");

        if (session == null || session.getAttribute("token") == null) {
            log.warn("No session or token found, redirecting to login");
            return "redirect:/login-page?error";
        }

        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userRole", session.getAttribute("userRole"));
        // Add active page for sidebar highlighting
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard - Street Incidents");

        return "dashboard";
    }
}