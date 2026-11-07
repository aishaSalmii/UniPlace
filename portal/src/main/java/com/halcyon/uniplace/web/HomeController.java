package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.repository.InterviewRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.ApplicationService;
import com.halcyon.uniplace.service.DashboardService;
import com.halcyon.uniplace.service.PostingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class HomeController {

    private final CurrentUser currentUser;
    private final DashboardService dashboard;
    private final ApplicationService applications;
    private final ApplicationRepository applicationRepository;
    private final OfferRepository offers;
    private final PostingService postings;
    private final InterviewRepository interviews;
    private final Clock clock;

    public HomeController(CurrentUser currentUser, DashboardService dashboard, ApplicationService applications,
                          ApplicationRepository applicationRepository, OfferRepository offers,
                          PostingService postings, InterviewRepository interviews, Clock clock) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
        this.applications = applications;
        this.applicationRepository = applicationRepository;
        this.offers = offers;
        this.postings = postings;
        this.interviews = interviews;
        this.clock = clock;
    }

    public record UpcomingInterview(Long applicationId, String posting, String company, LocalDate date,
                                    LocalTime from, LocalTime to, String location) {
    }

    /** Interview times in the format exchanged with the Engine. */
    private UpcomingInterview upcoming(Interview i) {
        LocalDateTime start = i.getStart().atZone(clock.getZone()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime end = i.getEnd().atZone(clock.getZone()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return new UpcomingInterview(i.getApplication().getId(), i.getApplication().getPosting().getTitle(),
                i.getApplication().getPosting().getCompany().getName(), start.toLocalDate(), start.toLocalTime(),
                end.toLocalTime(), i.getLocation());
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if ("locked".equals(error)) {
            model.addAttribute("error", "This account is locked for 15 minutes after 5 failed attempts.");
        } else if (error != null) {
            model.addAttribute("error", "Wrong username or password.");
        }
        return "login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied() {
        return "denied";
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        User user = currentUser.get();
        model.addAttribute("user", user);
        if (user.is(Role.COORDINATOR)) {
            model.addAttribute("figures", dashboard.coordinatorFigures());
        } else if (user.is(Role.STUDENT) && user.getStudent() != null) {
            model.addAttribute("student", user.getStudent());
            model.addAttribute("active", applications.countActive(user.getStudent()));
            model.addAttribute("limit", applications.activeLimitFor(user.getStudent()));
            model.addAttribute("pendingOffers", applicationRepository.findByStudentAndStateIn(user.getStudent(),
                    java.util.EnumSet.of(ApplicationState.OFFERED)).stream()
                    .map(a -> offers.findByApplication(a).orElse(null))
                    .filter(o -> o != null && o.getStatus() == OfferStatus.PENDING).toList());
            LocalDateTime now = LocalDateTime.now(clock);
            model.addAttribute("interviews", interviews.findActiveForStudent(user.getStudent()).stream()
                    .filter(i -> i.getEnd().isAfter(now))
                    .sorted(Comparator.comparing(Interview::getStart))
                    .map(this::upcoming).toList());
        } else if (user.is(Role.RECRUITER) && user.getCompany() != null) {
            model.addAttribute("company", user.getCompany());
            model.addAttribute("postings", postings.forCompany(user.getCompany()));
        }
        return "dashboard";
    }
}
