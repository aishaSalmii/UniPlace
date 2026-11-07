package com.halcyon.uniplace.web;

import com.halcyon.uniplace.ai.AiAssistantService;
import com.halcyon.uniplace.ai.AiRequestContext;
import com.halcyon.uniplace.ai.AiUnavailableException;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.AccessGuard;
import com.halcyon.uniplace.service.ApplicationService;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.CompanyService;
import com.halcyon.uniplace.service.ForbiddenException;
import com.halcyon.uniplace.service.PostingForm;
import com.halcyon.uniplace.service.PostingService;
import com.halcyon.uniplace.service.RankingService;
import com.halcyon.uniplace.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Controller
public class PostingController {

    private final PostingService postings;
    private final CompanyService companies;
    private final ApplicationService applications;
    private final RankingService ranking;
    private final AiAssistantService assistant;
    private final CurrentUser currentUser;
    private final AccessGuard guard;
    private final SettingsService settings;
    private final Clock clock;

    public PostingController(PostingService postings, CompanyService companies, ApplicationService applications,
                             RankingService ranking, AiAssistantService assistant, CurrentUser currentUser,
                             AccessGuard guard, SettingsService settings, Clock clock) {
        this.settings = settings;
        this.postings = postings;
        this.companies = companies;
        this.applications = applications;
        this.ranking = ranking;
        this.assistant = assistant;
        this.currentUser = currentUser;
        this.guard = guard;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- students

    @GetMapping("/postings")
    public String list(@RequestParam(required = false) String skill, @RequestParam(required = false) Long companyId,
                       @RequestParam(required = false) BigDecimal minAllowance, Model model) {
        model.addAttribute("postings", postings.search(skill, companyId, minAllowance));
        model.addAttribute("companies", companies.all());
        model.addAttribute("skill", skill);
        model.addAttribute("companyId", companyId);
        model.addAttribute("minAllowance", minAllowance);
        model.addAttribute("today", LocalDate.now(clock));
        return "postings/list";
    }

    @GetMapping("/postings/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        User user = currentUser.get();
        Posting p = postings.get(id);
        LocalDate today = LocalDate.now(clock);
        if (user.is(Role.STUDENT) && !postings.visibleToStudents(p, today)) {
            throw new com.halcyon.uniplace.service.NotFoundException("Posting");
        }
        if (user.is(Role.RECRUITER) && !p.getCompany().equals(user.getCompany())
                && !postings.visibleToStudents(p, today)) {
            throw new ForbiddenException();
        }
        model.addAttribute("posting", p);
        model.addAttribute("open", postings.acceptsApplications(p, today));
        model.addAttribute("user", user);
        if (!model.containsAttribute("letter")) {
            model.addAttribute("letter", "");
        }
        return "postings/detail";
    }

    /** AI-01 and AI-05: letter feedback on the application form. */
    @PostMapping("/postings/{id}/feedback")
    @Transactional(readOnly = true)
    public String feedback(@PathVariable Long id, @RequestParam(defaultValue = "") String letter, Model model) {
        User user = currentUser.get();
        if (!user.is(Role.STUDENT) || user.getStudent() == null) {
            throw new ForbiddenException();
        }
        Posting p = postings.get(id);
        if (letter.isBlank()) {
            model.addAttribute("aiError", "Write your letter first, then ask for feedback.");
        } else {
            try {
                model.addAttribute("feedback", assistant.feedback(new AiRequestContext(user.getStudent(), p, letter)));
            } catch (AiUnavailableException ex) {
                model.addAttribute("aiError", "Assistant unavailable. You can still submit your application.");
            }
        }
        model.addAttribute("letter", letter);
        return detail(id, model);
    }

    // -------------------------------------------------------------- recruiters

    @GetMapping("/recruiter/postings")
    @Transactional(readOnly = true)
    public String mine(Model model) {
        User user = currentUser.get();
        model.addAttribute("company", user.getCompany());
        model.addAttribute("postings", user.getCompany() == null ? java.util.List.of() : postings.forCompany(user.getCompany()));
        return "postings/mine";
    }

    @GetMapping("/recruiter/postings/new")
    public String newForm(Model model) {
        model.addAttribute("form", postings.newForm());
        model.addAttribute("postingId", null);
        return "postings/form";
    }

    @PostMapping("/recruiter/postings")
    public String create(@ModelAttribute("form") PostingForm form, Model model, RedirectAttributes ra) {
        try {
            Posting p = postings.create(currentUser.get(), form);
            ra.addFlashAttribute("success", "Posting saved as a draft.");
            return "redirect:/postings/" + p.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("postingId", null);
            return "postings/form";
        }
    }

    @GetMapping("/recruiter/postings/{id}/edit")
    @Transactional(readOnly = true)
    public String editForm(@PathVariable Long id, Model model) {
        Posting p = postings.get(id);
        guard.assertRecruiterOwns(currentUser.get(), p);
        model.addAttribute("form", postings.formOf(p));
        model.addAttribute("postingId", id);
        model.addAttribute("status", p.getStatus());
        return "postings/form";
    }

    @PostMapping("/recruiter/postings/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") PostingForm form, Model model,
                         RedirectAttributes ra) {
        try {
            postings.update(id, form);
            return "redirect:/recruiter/postings";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("postingId", id);
            return "postings/form";
        }
    }

    @PostMapping("/recruiter/postings/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        guard.assertRecruiterOwns(currentUser.get(), postings.get(id));
        try {
            postings.publish(id);
            ra.addFlashAttribute("success", "Posting published.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/postings/" + id;
    }

    @PostMapping("/recruiter/postings/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes ra) {
        guard.assertRecruiterOwns(currentUser.get(), postings.get(id));
        try {
            postings.close(id);
            ra.addFlashAttribute("success", "Posting closed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/postings/" + id;
    }

    @GetMapping("/recruiter/postings/{id}/candidates")
    @Transactional(readOnly = true)
    public String candidates(@PathVariable Long id, Model model) {
        Posting p = postings.get(id);
        guard.assertRecruiterOwns(currentUser.get(), p);
        model.addAttribute("posting", p);
        try {
            model.addAttribute("candidates", ranking.rank(p));
        } catch (BusinessRuleException ex) {
            model.addAttribute("candidates", java.util.List.of());
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("threshold", settings.shortlistThreshold());
        return "postings/candidates";
    }
}
