package com.halcyon.uniplace.web;

import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.NotificationService;
import com.halcyon.uniplace.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserService users;
    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public AccountController(UserService users, NotificationService notifications, CurrentUser currentUser) {
        this.users = users;
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping("/account/password")
    public String passwordForm() {
        return "account/password";
    }

    @PostMapping("/account/password")
    public String changePassword(@RequestParam String current, @RequestParam String next,
                                 @RequestParam String confirm, RedirectAttributes ra) {
        if (!next.equals(confirm)) {
            ra.addFlashAttribute("error", "The new password and its confirmation do not match.");
            return "redirect:/account/password";
        }
        try {
            users.changePassword(currentUser.get(), current, next);
            ra.addFlashAttribute("success", "Password changed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/account/password";
    }

    /** Ends the visit: clears the session cookie in the browser and returns to the login page. */
    @PostMapping("/account/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }

    /** NOT-03: a user's own notifications. */
    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public String notifications(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", notifications.forUser(currentUser.get(), page));
        model.addAttribute("all", false);
        return "notifications";
    }
}
