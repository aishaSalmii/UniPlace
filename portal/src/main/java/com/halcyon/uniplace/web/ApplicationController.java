package com.halcyon.uniplace.web;

import org.springframework.validation.BindingResult;
import java.util.Map;
import com.halcyon.uniplace.ai.AiAssistantService;
import com.halcyon.uniplace.ai.AiRequestContext;
import com.halcyon.uniplace.ai.AiUnavailableException;
import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.AccessGuard;
import com.halcyon.uniplace.service.ApplicationService;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.InterviewService;
import com.halcyon.uniplace.service.NotEligibleException;
import com.halcyon.uniplace.service.OfferForm;
import com.halcyon.uniplace.service.OfferService;
import com.halcyon.uniplace.service.SettingsService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class ApplicationController {

    private final ApplicationService applications;
    private final InterviewService interviews;
    private final OfferService offers;
    private final AiAssistantService assistant;
    private final SettingsService settings;
    private final CurrentUser currentUser;
    private final AccessGuard guard;

    public ApplicationController(ApplicationService applications, InterviewService interviews, OfferService offers,
                                 AiAssistantService assistant, SettingsService settings, CurrentUser currentUser,
                                 AccessGuard guard) {
        this.applications = applications;
        this.interviews = interviews;
        this.offers = offers;
        this.assistant = assistant;
        this.settings = settings;
        this.currentUser = currentUser;
        this.guard = guard;
    }

    /** APP-01 to APP-05. */
    @PostMapping("/postings/{id}/apply")
    public String apply(@PathVariable Long id, @RequestParam(defaultValue = "") String letter, RedirectAttributes ra) {
        try {
            Application a = applications.submit(currentUser.get(), id, letter);
            ra.addFlashAttribute("success", "Your application was submitted. Match score: " + a.getMatchScore() + ".");
            return "redirect:/applications/" + a.getId();
        } catch (NotEligibleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("letter", letter);
            return "redirect:/postings/" + id;
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", "Unable to submit your application. Please check the posting and try again.");
            return "redirect:/postings/" + id;
        }
    }

    @GetMapping("/applications")
    @Transactional(readOnly = true)
    public String list(@RequestParam(required = false) ApplicationState state,
                       @RequestParam(required = false) Long postingId,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        User user = currentUser.get();
        if (user.is(Role.COORDINATOR)) {
            return "redirect:/coordinator/applications";
        }
        Page<Application> result = user.is(Role.STUDENT)
                ? applications.forStudent(user.getStudent(), page)
                : applications.forRecruiter(user, state, postingId, page);
        model.addAttribute("page", result);
        model.addAttribute("state", state);
        model.addAttribute("postingId", postingId);
        model.addAttribute("states", ApplicationState.values());
        model.addAttribute("user", user);
        return "applications/list";
    }

    @GetMapping("/applications/{id}")
    @Transactional(readOnly = true)
    public String view(@PathVariable Long id, Model model) {
        User user = currentUser.get();
        Application a = applications.get(id);
        fill(model, user, a);
        return "applications/view";
    }

    private void fill(Model model, User user, Application a) {
        Offer offer = applications.offerOf(a);
        model.addAttribute("app", a);
        model.addAttribute("user", user);
        model.addAttribute("offer", offer);
        model.addAttribute("lines", offers.linesOf(offer));
        model.addAttribute("interviews", interviews.forApplication(a));
        model.addAttribute("threshold", settings.shortlistThreshold());
        if (!model.containsAttribute("offerForm")) {
            model.addAttribute("offerForm", offers.formFor(a));
        }
    }

    @PostMapping("/applications/{id}/withdraw")
    public String withdraw(@PathVariable Long id, RedirectAttributes ra) {
        try {
            applications.withdraw(currentUser.get(), id);
            ra.addFlashAttribute("success", "Your application was withdrawn.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/shortlist")
    public String shortlist(@PathVariable Long id, @RequestParam(required = false) String justification,
                            RedirectAttributes ra) {
        try {
            applications.shortlist(currentUser.get(), id, justification);
            ra.addFlashAttribute("success", "Application shortlisted.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        try {
            applications.reject(currentUser.get(), id);
            ra.addFlashAttribute("success", "Application rejected.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/interview")
    public String schedule(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime from,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime to,
                           @RequestParam(defaultValue = "") String location, RedirectAttributes ra) {
        try {
            interviews.schedule(currentUser.get(), id, date, from, to, location);
            ra.addFlashAttribute("success", "Interview scheduled.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/interviews/{interviewId}/cancel")
    public String cancelInterview(@PathVariable Long interviewId, @RequestParam Long applicationId,
                                  RedirectAttributes ra) {
        try {
            interviews.cancel(currentUser.get(), interviewId);
            ra.addFlashAttribute("success", "Interview cancelled.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + applicationId;
    }

    @PostMapping("/applications/{id}/reschedule")
    public String reschedule(@PathVariable Long id, @RequestParam(required = false) String note,
                             RedirectAttributes ra) {
        try {
            interviews.requestReschedule(currentUser.get(), id, note);
            ra.addFlashAttribute("success", "Your reschedule request was sent to the recruiter.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    /** USA-01: the offer form's fields, named as the form labels them. */
    static final Map<String, String> OFFER_FIELDS = Map.of("monthlyAllowance", "Monthly allowance",
            "weeklyHours", "Weekly hours", "startDate", "Start", "endDate", "End");
    static final Map<String, String> OFFER_HINTS = Map.of("monthlyAllowance", "enter an amount, for example 3000.00.",
            "weeklyHours", "enter a whole number of hours.", "startDate", "choose a date.", "endDate", "choose a date.");

    @PostMapping("/applications/{id}/offer")
    public String issueOffer(@PathVariable Long id, @ModelAttribute("offerForm") OfferForm form,
                             BindingResult binding, RedirectAttributes ra) {
        if (binding.hasFieldErrors()) {
            ra.addFlashAttribute("error", binding.getFieldErrors().stream()
                    .map(e -> "Invalid value for " + e.getField() + ".")
                    .distinct().collect(java.util.stream.Collectors.joining(" ")));
            return "redirect:/applications/" + id;
        }
        try {
            offers.issue(currentUser.get(), id, form);
            ra.addFlashAttribute("success", "Offer issued.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("offerForm", form);
        }
        return "redirect:/applications/" + id;
    }

    /** AI-02: interview questions for the recruiter. */
    @PostMapping("/applications/{id}/questions")
    @Transactional(readOnly = true)
    public String questions(@PathVariable Long id, Model model) {
        User user = currentUser.get();
        Application a = applications.get(id);
        guard.assertRecruiterOwns(user, a);
        try {
            model.addAttribute("questions", assistant.interviewQuestions(
                    new AiRequestContext(a.getStudent(), a.getPosting(), a.getLetter())));
        } catch (AiUnavailableException ex) {
            model.addAttribute("aiError", "Assistant unavailable. Try again later.");
        }
        fill(model, user, a);
        return "applications/view";
    }

    @PostMapping("/offers/{id}/accept")
    public String accept(@PathVariable Long id, RedirectAttributes ra) {
        Offer o = offers.get(id);
        try {
            offers.accept(currentUser.get(), id);
            ra.addFlashAttribute("success", "Congratulations! You accepted the offer.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + o.getApplication().getId();
    }

    @PostMapping("/offers/{id}/decline")
    public String decline(@PathVariable Long id, RedirectAttributes ra) {
        Offer o = offers.get(id);
        try {
            offers.decline(currentUser.get(), id);
            ra.addFlashAttribute("success", "You declined the offer.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + o.getApplication().getId();
    }
}
