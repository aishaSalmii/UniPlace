package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.MatchScoreService;
import com.halcyon.uniplace.repository.PostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
public class PostingService {

    /** POS-07: closed postings stay visible to students for this many days after their deadline. */
    public static final int CLOSED_VISIBLE_DAYS = 7;
    static final int MAX_SEATS = 20;
    static final int MIN_HOURS = 10;
    static final int MAX_HOURS = 40;
    static final BigDecimal MAX_ALLOWANCE = new BigDecimal("5000");

    private final PostingRepository postings;
    private final SettingsService settings;
    private final MatchScoreService matchScores;
    private final Clock clock;

    public PostingService(PostingRepository postings, SettingsService settings, MatchScoreService matchScores,
                          Clock clock) {
        this.postings = postings;
        this.settings = settings;
        this.matchScores = matchScores;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Posting get(Long id) {
        return postings.findWithCompany(id).orElseThrow(() -> new NotFoundException("Posting"));
    }

    /** POS-03 and POS-04: an application is accepted until the end of the deadline day. */
    public boolean acceptsApplications(Posting posting, LocalDate today) {
        return posting.getStatus() == PostingStatus.OPEN
                && posting.getCompany().getStatus() == CompanyStatus.APPROVED
                && today.isBefore(posting.getDeadline());
    }

    public boolean visibleToStudents(Posting posting, LocalDate today) {
        if (posting.getCompany().getStatus() != CompanyStatus.APPROVED) {
            return false;
        }
        if (posting.getStatus() == PostingStatus.OPEN) {
            return true;
        }
        return posting.getStatus() != PostingStatus.DRAFT
                && !posting.getDeadline().isBefore(today.minusDays(CLOSED_VISIBLE_DAYS));
    }

    /** POS-06: search over the postings visible to students, earliest deadline first. */
    @Transactional(readOnly = true)
    public List<Posting> search(String skill, Long companyId, BigDecimal minAllowance) {
        LocalDate today = LocalDate.now(clock);
        String needle = skill == null ? "" : skill.trim().toLowerCase(Locale.ROOT);
        EnumSet<PostingStatus> statuses = EnumSet.of(PostingStatus.OPEN, PostingStatus.CLOSED, PostingStatus.FILLED);
        LocalDate closedSince = today.minusDays(CLOSED_VISIBLE_DAYS);
        List<Posting> visible = companyId == null
                ? postings.findVisible(CompanyStatus.APPROVED, statuses, closedSince)
                : postings.findVisibleForCompany(companyId, CompanyStatus.APPROVED, statuses, closedSince);
        return visible.stream()
                .filter(p -> needle.isEmpty() || hasSkill(p, needle))
                .filter(p -> minAllowance == null || p.getMonthlyAllowance().compareTo(minAllowance) >= 0)
                .toList();
    }

    private static boolean hasSkill(Posting p, String needle) {
        return p.getRequiredSkills().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(needle))
                || p.getNiceToHaveSkills().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(needle));
    }

    @Transactional(readOnly = true)
    public List<Posting> forCompany(Company company) {
        return postings.findByCompanyOrderByDeadlineAsc(company);
    }

    public PostingForm newForm() {
        PostingForm f = new PostingForm();
        LocalDate today = LocalDate.now(clock);
        f.setMinGpa(settings.getDecimal(SettingsService.DEFAULT_MIN_GPA));
        f.setMinCredits(settings.getInt(SettingsService.DEFAULT_MIN_CREDITS));
        f.setSeats(1);
        f.setDeadline(today.plusWeeks(3));
        f.setStartDate(today.plusWeeks(6));
        f.setEndDate(today.plusWeeks(6).plusMonths(3));
        f.setWeeklyHours(40);
        f.setMonthlyAllowance(new BigDecimal("3000"));
        return f;
    }

    public PostingForm formOf(Posting p) {
        PostingForm f = new PostingForm();
        f.setTitle(p.getTitle());
        f.setDescription(p.getDescription());
        f.setRequiredSkills(String.join(", ", p.getRequiredSkills()));
        f.setNiceToHaveSkills(String.join(", ", p.getNiceToHaveSkills()));
        f.setMinGpa(p.getMinGpa());
        f.setMinCredits(p.getMinCredits());
        f.setSeats(p.getSeats());
        f.setDeadline(p.getDeadline());
        f.setStartDate(p.getStartDate());
        f.setEndDate(p.getEndDate());
        f.setWeeklyHours(p.getWeeklyHours());
        f.setMonthlyAllowance(p.getMonthlyAllowance());
        return f;
    }

    @Transactional
    public Posting create(User recruiter, PostingForm form) {
        if (!recruiter.is(Role.RECRUITER) || recruiter.getCompany() == null) {
            throw new BusinessRuleException("Register your company before creating postings.");
        }
        Posting p = new Posting(recruiter.getCompany(), "", "");
        apply(p, form);
        p.setCreatedAt(LocalDateTime.now(clock));
        return postings.save(p);
    }

    @Transactional
    public Posting update(Long postingId, PostingForm form) {
        Posting p = get(postingId);
        if (p.getStatus() == PostingStatus.CLOSED || p.getStatus() == PostingStatus.FILLED) {
            throw new BusinessRuleException("A " + p.getStatus().name().toLowerCase() + " posting cannot be edited.");
        }
        boolean deadlineUnchanged = form.getDeadline() != null && form.getDeadline().equals(p.getDeadline());
        apply(p, form, deadlineUnchanged);
        matchScores.evict(p);
        return postings.save(p);
    }

    @Transactional
    public Posting publish(Long postingId) {
        Posting p = get(postingId);
        if (p.getStatus() != PostingStatus.DRAFT) {
            throw new BusinessRuleException("Only a draft posting can be published.");
        }
        if (p.getCompany().getStatus() != CompanyStatus.APPROVED) {
            throw new BusinessRuleException("Only approved companies can publish postings.");
        }
        if (p.getDeadline().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("The deadline has passed; change it before publishing.");
        }
        p.setStatus(PostingStatus.OPEN);
        return p;
    }

    @Transactional
    public Posting close(Long postingId) {
        Posting p = get(postingId);
        if (p.getStatus() != PostingStatus.OPEN) {
            throw new BusinessRuleException("Only an open posting can be closed.");
        }
        p.setStatus(PostingStatus.CLOSED);
        return p;
    }

    /** POS-03: postings close automatically once their deadline has passed. */
    @Transactional
    public int closeExpired() {
        List<Posting> expired = postings.findByStatusAndDeadlineBefore(PostingStatus.OPEN, LocalDate.now(clock));
        expired.forEach(p -> p.setStatus(PostingStatus.CLOSED));
        return expired.size();
    }

    private void apply(Posting p, PostingForm f) {
        apply(p, f, false);
    }

    private void apply(Posting p, PostingForm f, boolean deadlineUnchanged) {
        LocalDate today = LocalDate.now(clock);
        require(f.getTitle() != null && !f.getTitle().isBlank() && f.getTitle().length() <= 150,
                "The title is required (at most 150 characters).");
        require(f.getDescription() != null && !f.getDescription().isBlank() && f.getDescription().length() <= 4000,
                "The description is required (at most 4,000 characters).");
        List<String> required = StudentService.parseSkills(f.getRequiredSkills());
        require(!required.isEmpty(), "List at least one required skill.");
        List<String> nice = StudentService.parseSkills(f.getNiceToHaveSkills());
        BigDecimal minGpa = f.getMinGpa() == null ? settings.getDecimal(SettingsService.DEFAULT_MIN_GPA) : f.getMinGpa();
        int minCredits = f.getMinCredits() == null ? settings.getInt(SettingsService.DEFAULT_MIN_CREDITS) : f.getMinCredits();
        require(minGpa.signum() >= 0 && minGpa.compareTo(new BigDecimal("4.00")) <= 0, "The minimum GPA must be between 0.00 and 4.00.");
        require(minCredits >= 0 && minCredits <= 140, "The minimum credits must be between 0 and 140.");
        require(f.getSeats() != null && f.getSeats() >= 1 && f.getSeats() <= MAX_SEATS, "Seats must be between 1 and 20.");
        require(f.getDeadline() != null && f.getStartDate() != null && f.getEndDate() != null,
                "The deadline, start date and end date are required.");
        require(deadlineUnchanged || !f.getDeadline().isBefore(today), "The deadline must be today or later.");
        require(f.getStartDate().isAfter(f.getDeadline()), "The start date must be after the deadline.");
        require(!f.getEndDate().isBefore(f.getStartDate().plusWeeks(4)), "The internship must last at least 4 weeks.");
        require(!f.getEndDate().isAfter(f.getStartDate().plusMonths(6)), "The internship can last at most 6 months.");
        require(f.getWeeklyHours() != null && f.getWeeklyHours() >= MIN_HOURS && f.getWeeklyHours() <= MAX_HOURS,
                "Weekly hours must be between 10 and 40.");
        require(f.getMonthlyAllowance() != null && f.getMonthlyAllowance().signum() >= 0
                && f.getMonthlyAllowance().compareTo(MAX_ALLOWANCE) <= 0, "The monthly allowance must be between 0 and 5,000.");

        p.setTitle(f.getTitle().trim());
        p.setDescription(f.getDescription().trim());
        p.setRequiredSkills(required);
        p.setNiceToHaveSkills(nice);
        p.setMinGpa(minGpa.setScale(2, java.math.RoundingMode.HALF_UP));
        p.setMinCredits(minCredits);
        p.setSeats(f.getSeats());
        p.setDeadline(f.getDeadline());
        p.setStartDate(f.getStartDate());
        p.setEndDate(f.getEndDate());
        p.setWeeklyHours(f.getWeeklyHours());
        p.setMonthlyAllowance(f.getMonthlyAllowance().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessRuleException(message);
        }
    }
}
