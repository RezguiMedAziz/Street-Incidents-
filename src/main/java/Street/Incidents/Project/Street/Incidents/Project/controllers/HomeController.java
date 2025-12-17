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

//    @GetMapping("/")
//    public String home() {
//        return "redirect:/login-page";
//    }

        @GetMapping({"/", "/home"})
        public String home(HttpSession session) {
            log.info("Home page accessed");

            // If user is logged in, redirect to their dashboard
            if (session != null && session.getAttribute("userRole") != null) {
                String userRole = (String) session.getAttribute("userRole");
                log.info("User is logged in as {}, redirecting to dashboard", userRole);

                return switch (userRole) {
                    case "CITOYEN" -> "redirect:/citizen/dashboard";
                    case "ADMINISTRATEUR" -> "redirect:/admin/dashboard";
                    case "AGENT_MUNICIPAL" -> "redirect:/agent/dashboard";
                    default -> "redirect:/login-page";
                };
            }

            return "redirect:/login-page";
        }
    }