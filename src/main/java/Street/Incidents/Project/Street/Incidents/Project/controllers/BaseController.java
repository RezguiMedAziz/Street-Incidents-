package Street.Incidents.Project.Street.Incidents.Project.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public abstract class BaseController {

    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Add session attributes to model for all controllers
            model.addAttribute("userEmail", session.getAttribute("userEmail"));
            model.addAttribute("userName", session.getAttribute("userName"));
            model.addAttribute("userRole", session.getAttribute("userRole"));

            // Add app version (you can get this from properties or config)
            model.addAttribute("appVersion", "1.0.0");

            // Add notification count (you'll need to implement this)
            // model.addAttribute("notificationCount", getNotificationCount(session));
        }
    }
}