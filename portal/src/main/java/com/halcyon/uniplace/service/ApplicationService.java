package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineRequestMapper;
import com.halcyon.uniplace.engine.MatchScoreService;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.StudentRepository;
import com.halcyon.uniplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ApplicationService {

    public static final int PAGE_SIZE = 20;
    public static final int MIN_LETTER = 100;
    public static final int MAX_LETTER = 2000;

    private final ApplicationRepository applications;
    private final StudentRepository students;
    private final PostingRepository postings;
    private final OfferRepository offers;
    private final UserRepository users;
    private final PostingService postingService;
    private final DocumentService documents;
    private final EngineClient engine;
    private final EngineRequestMapper mapper;
    private final MatchScoreService matchScores;
    private final SettingsService settings;
    private final NotificationService notifications;
    private final AuditService audit;
    private final AccessGuard guard;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, StudentRepository students,
                              PostingRepository postings, OfferRepository offers, UserRepository users,
                              PostingService postingService, DocumentService documents, EngineClient engine,
                              EngineRequestMapper mapper, MatchScoreService matchScores, SettingsService settings,
                              NotificationService notifications, AuditService audit, AccessGuard guard, Clock clock) {
        this.applications = applications;
        this.students = students;
        this.postings = postings;
        this.offers = offers;
        this.users = users;
        this.postingService = postingService;
        this.documents = documents;
        this.engine = engine;
        this.mapper = mapper;
        this.matchScores = matchScores;
        this.settings = settings;
        this.notifications = notifications;
        this.audit = audit;
        this.guard = guard;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Application get(Long id) {
        return applications.findDetailed(id).orElseThrow(() -> new NotFoundException("Application"));
    }

    /** APP-02: applications that count against the student's limit. */
    @Transactional(readOnly = true)
    public long countActive(Student student) {
        return applications.countByStudentAndStateNotIn(student,
                java.util.EnumSet.of(ApplicationState.REJECTED, ApplicationState.ACCEPTED));
    }

    /** APP-02: the limit depends on academic standing. */
    public int activeLimitFor(Student student) {
        return "PROBATION".equals(student.getStanding())
                ? settings.maxActiveProbation()
                : settings.maxActiveGood();
    }

    @Transactional(readOnly = true)
    public Page<Application> forStudent(Student student, int page) {
        return applications.findByStudent(student,
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "submittedAt", "id")));
    }

    @Transactional(readOnly = true)
    public Page<Application> forRecruiter(User recruiter, ApplicationState state, Long postingId, int page) {
        if (recruiter.getCompany() == null) {
            return Page.empty();
        }
        return applications.findForCompany(recruiter.getCompany(), state, postingId,
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "submittedAt", "id")));
    }

    /** APP-10: the coordinator's list. */
    @Transactional(readOnly = true)
    public Page<Application> pageForCoordinator(ApplicationState state, Long postingId, Long companyId, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        List<Application> matching = applications.findAll().stream()
                .filter(a -> state == null || a.getState() == state)
                .filter(a -> postingId == null || a.getPosting().getId().equals(postingId))
                .filter(a -> companyId == null || a.getPosting().getCompany().getId().equals(companyId))
                .sorted(Comparator.comparing(Application::getSubmittedAt)
                        .thenComparing(Application::getId).reversed())
                .toList();
        int from = Math.min((int) pageable.getOffset(), matching.size());
        int to = Math.min(from + PAGE_SIZE, matching.size());
        return new PageImpl<>(matching.subList(from, to), pageable, matching.size());
    }

    // ----------------------------------------------------------------- commands

    /** APP-01 to APP-05. */
    @Transactional
    public Application submit(User user, Long postingId, String letter) {
        if (!user.is(Role.STUDENT) || user.getStudent() == null) {
            throw new ForbiddenException();
        }
        Student student = students.findById(user.getStudent().getId())
                .orElseThrow(() -> new NotFoundException("Student"));
        Posting posting = postingService.get(postingId);
        LocalDate today = LocalDate.now(clock);

        if (!postingService.acceptsApplications(posting, today)) {
            throw new BusinessRuleException("This posting is not accepting applications. Its deadline was "
                    + posting.getDeadline() + ".");
        }
        String text = letter == null ? "" : letter.trim();
        if (text.length() < MIN_LETTER || text.length() > MAX_LETTER) {
            throw new BusinessRuleException("The motivation letter must have between " + MIN_LETTER + " and "
                    + MAX_LETTER + " characters (it has " + text.length() + ").");
        }
        if (applications.existsByStudentAndPostingAndStateNot(student, posting, ApplicationState.WITHDRAWN)) {
            throw new BusinessRuleException("You already have an active application for this posting.");
        }
        long active = countActive(student);
        int limit = activeLimitFor(student);
        if (active >= limit) {
            throw new BusinessRuleException("You already have " + active + " active applications, the maximum for "
                    + "students in " + student.getStanding().name().toLowerCase() + " standing.");
        }

        EligibilityResult eligibility = engine.checkEligibility(
                mapper.toEligibilityRequest(student, posting, documents.forEligibility(student), today));
        if (!eligibility.eligible()) {
            throw new NotEligibleException(eligibility.reasons());
        }
        int score = matchScores.score(student.getSkills(), posting);

        Application saved = applications.save(new Application(student, posting, text, score, LocalDateTime.now(clock)));
        notifications.notify(user, "Application submitted: " + posting.getTitle(),
                "Your application to " + posting.getTitle() + " at " + posting.getCompany().getName()
                        + " was submitted. Match score: " + score + ".");
        for (User recruiter : users.findByCompanyAndRole(posting.getCompany(), Role.RECRUITER)) {
            notifications.notify(recruiter, "New application: " + posting.getTitle(),
                    student.getFullName() + " applied to " + posting.getTitle() + " at "
                            + posting.getCompany().getName() + ".");
        }
        return saved;
    }

    /** APP-07. */
    @Transactional
    public Application shortlist(User recruiter, Long applicationId, String justification) {
        Application a = get(applicationId);
        guard.assertRecruiterOwns(recruiter, a);
        int threshold = settings.shortlistThreshold();
        boolean override = a.getMatchScore() <= threshold;
        if (override && (justification == null || justification.isBlank())) {
            throw new BusinessRuleException("The match score " + a.getMatchScore() + " is below the shortlist threshold "
                    + threshold + ". Give a justification to shortlist it anyway.");
        }
        transition(a, ApplicationState.SHORTLISTED);
        if (override) {
            a.setOverrideJustification(justification.trim());
            audit.record(recruiter.getUsername(), "SHORTLIST_OVERRIDE", "application " + a.getId(),
                    "score " + a.getMatchScore() + " < " + threshold, justification.trim());
        }
        notifyStudent(a, "Shortlisted: " + a.getPosting().getTitle(),
                "Your application to " + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName()
                        + " was shortlisted.");
        return a;
    }

    @Transactional
    public Application reject(User recruiter, Long applicationId) {
        Application a = get(applicationId);
        guard.assertRecruiterOwns(recruiter, a);
        transition(a, ApplicationState.REJECTED);
        offers.findByApplication(a).filter(o -> o.getStatus() == OfferStatus.PENDING)
                .ifPresent(o -> o.setStatus(OfferStatus.DECLINED));
        notifyStudent(a, "Application not selected: " + a.getPosting().getTitle(),
                "Your application to " + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName()
                        + " was not selected.");
        return a;
    }

    /** APP-08. */
    @Transactional
    public Application withdraw(User user, Long applicationId) {
        Application a = get(applicationId);
        guard.assertStudentOwns(user, a);
        assertNotFrozen(a);
        a.setState(ApplicationState.WITHDRAWN);
        a.setUpdatedAt(LocalDateTime.now(clock));
        offers.findByApplication(a).filter(o -> o.getStatus() == OfferStatus.PENDING)
                .ifPresent(o -> o.setStatus(OfferStatus.DECLINED));
        return a;
    }

    /** OFF-07: the coordinator records when the signed placement agreement was received. */
    @Transactional
    public Application recordAgreement(Long applicationId, LocalDate receivedOn) {
        Application a = get(applicationId);
        if (a.getState() != ApplicationState.ACCEPTED) {
            throw new BusinessRuleException("The agreement can only be recorded for an accepted placement.");
        }
        if (receivedOn == null || receivedOn.isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("Enter a date that is not in the future.");
        }
        a.setAgreementReceivedOn(receivedOn);
        return a;
    }

    // ----------------------------------------------------------------- internals

    /** Moves an application on behalf of a user; refused while the company is not approved (COM-02). */
    public void transition(Application a, ApplicationState to) {
        assertNotFrozen(a);
        move(a, to);
    }

    /** Moves an application on behalf of the system (automatic withdrawals, expiry, filled posting). */
    public void move(Application a, ApplicationState to) {
        ApplicationStateMachine.assertCanMove(a.getState(), to);
        a.setState(to);
        a.setUpdatedAt(LocalDateTime.now(clock));
    }

    public void assertNotFrozen(Application a) {
        if (a.getPosting().getCompany().getStatus() == CompanyStatus.PENDING) {
            throw new BusinessRuleException(a.getPosting().getCompany().getName()
                    + " is not approved at the moment; its applications are frozen.");
        }
    }

    public void notifyStudent(Application a, String subject, String body) {
        users.findByStudent(a.getStudent()).ifPresent(u -> notifications.notify(u, subject, body));
    }

    public Offer offerOf(Application a) {
        return offers.findByApplication(a).orElse(null);
    }
}
