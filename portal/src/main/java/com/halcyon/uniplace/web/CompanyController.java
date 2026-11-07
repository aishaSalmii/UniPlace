package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CompanyController {

    private final CompanyService companies;
    private final CurrentUser currentUser;

    public CompanyController(CompanyService companies, CurrentUser currentUser) {
        this.companies = companies;
        this.currentUser = currentUser;
    }

    @GetMapping("/recruiter/company")
    @Transactional(readOnly = true)
    public String view(Model model) {
        User user = currentUser.get();
        model.addAttribute("company", user.getCompany());
        return "company/form";
    }

    @PostMapping("/recruiter/company")
    public String save(@RequestParam String name, @RequestParam String sector, @RequestParam String contactEmail,
                       RedirectAttributes ra) {
        User user = currentUser.get();
        try {
            if (user.getCompany() == null) {
                companies.register(user, name, sector, contactEmail);
                ra.addFlashAttribute("success", "Company registered. A coordinator will review it.");
            } else {
                companies.update(user, name, sector, contactEmail);
                ra.addFlashAttribute("success", "Company details saved.");
            }
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/recruiter/company";
    }
}
