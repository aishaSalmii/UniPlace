package com.halcyon.uniplace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceLine;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.engine.EngineRequestMapper;
import com.halcyon.uniplace.engine.EngineUnavailableException;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.List;

/** OFF-01 to OFF-07. */
@Service
public class OfferService {

    private static final Logger log = LoggerFactory.getLogger(OfferService.class);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OfferRepository offers;
    private final ApplicationRepository applicationRepository;
    private final PostingRepository postings;
    private final UserRepository users;
    private final ApplicationService applications;
    private final EngineClient engine;
    private final EngineRequestMapper mapper;
    private final SettingsService settings;
    private final NotificationService notifications;
    private final AccessGuard guard;
    private final ObjectMapper json;
    private final Clock clock;

    public OfferService(OfferRepository offers, ApplicationRepository applicationRepository, PostingRepository postings,
                        UserRepository users, ApplicationService applications, EngineClient engine,
                        EngineRequestMapper mapper, SettingsService settings, NotificationService notifications,
                        AccessGuard guard, ObjectMapper json, Clock clock) {
        this.offers = offers;
        this.applicationRepository = applicationRepository;
        this.postings = postings;
        this.users = users;
        this.applications = applications;
        this.engine = engine;
        this.mapper = mapper;
        this.settings = settings;
        this.notifications = notifications;
        this.guard = guard;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Offer get(Long id) {
        return offers.findById(id).orElseThrow(() -> new NotFoundException("Offer"));
    }

    /** OFF-02: the student has until the end of the response window. */
    public LocalDateTime responseDeadline(LocalDateTime issuedAt) {
        return issuedAt.toLocalDate().plusDays(settings.offerResponseHours() / 24).atStartOfDay();
    }

    public OfferForm formFor(Application a) {
        Posting p = a.getPosting();
        OfferForm f = new OfferForm();
        f.setMonthlyAllowance(p.getMonthlyAllowance());
        f.setStartDate(p.getStartDate());
        f.setEndDate(p.getEndDate());
        f.setWeeklyHours(p.getWeeklyHours());
        return f;
    }

    @Transactional
    public Offer issue(User recruiter, Long applicationId, OfferForm form) {
        Application a = applications.get(applicationId);
        guard.assertRecruiterOwns(recruiter, a);
        Posting p = a.getPosting();
        if (form.getMonthlyAllowance() == null || form.getStartDate() == null || form.getEndDate() == null
                || form.getWeeklyHours() == null) {
            throw new BusinessRuleException("Enter the allowance, dates and weekly hours.");
        }
        if (form.getMonthlyAllowance().signum() < 0 || form.getMonthlyAllowance().compareTo(p.getMonthlyAllowance()) > 0) {
            throw new BusinessRuleException("Invalid offer: monthlyAllowance > posting.monthlyAllowance ("
                    + p.getMonthlyAllowance() + ").");
        }
        if (form.getStartDate().isBefore(p.getStartDate()) || form.getEndDate().isAfter(p.getEndDate())
                || form.getEndDate().isBefore(form.getStartDate())) {
            throw new BusinessRuleException("Invalid offer: startDate/endDate outside [" + p.getStartDate() + ", "
                    + p.getEndDate() + "].");
        }
        if (form.getWeeklyHours() < 10 || form.getWeeklyHours() > 40) {
            throw new BusinessRuleException("Invalid offer: weeklyHours not in [10, 40].");
        }
        if (offers.findByApplication(a).isPresent()) {
            throw new BusinessRuleException("An offer was already issued for this application.");
        }
        applications.assertNotFrozen(a);
        LocalDateTime now = LocalDateTime.now(clock);
        a.setState(ApplicationState.OFFERED);
        a.setUpdatedAt(now);
        Offer offer = offers.save(new Offer(a, form.getMonthlyAllowance(), form.getStartDate(), form.getEndDate(),
                form.getWeeklyHours(), now, responseDeadline(now)));
        applications.notifyStudent(a, "Offer received: " + p.getTitle(),
                p.getCompany().getName() + " offers you the " + p.getTitle() + " internship. Please answer by "
                        + offer.getRespondBy().format(DISPLAY) + ".");
        return offer;
    }

    /**
     * OFF-03 and OFF-04.
     */
    @Transactional
    public Offer accept(User student, Long offerId) {
        Offer offer = get(offerId);
        Application a = applications.get(offer.getApplication().getId());
        guard.assertStudentOwns(student, a);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This offer is " + offer.getStatus().name().toLowerCase() + ".");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        applications.assertNotFrozen(a);
        Posting posting = a.getPosting();
        if (posting.seatsLeft() <= 0) {
            throw new BusinessRuleException("All seats of " + posting.getTitle() + " are already taken.");
        }
        posting.setSeatsTaken(posting.getSeatsTaken() + 1);
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(now);
        applications.move(a, ApplicationState.ACCEPTED);

        if (posting.seatsLeft() == 0) {
            posting.setStatus(PostingStatus.FILLED);
        }

        tryComputeAllowance(offer, 0);
        applications.notifyStudent(a, "Offer accepted: " + posting.getTitle(),
                "You accepted the " + posting.getTitle() + " internship at " + posting.getCompany().getName() + ".");
        notifyRecruiters(posting, "Offer accepted: " + posting.getTitle(),
                a.getStudent().getFullName() + " accepted the offer for " + posting.getTitle() + " at "
                        + posting.getCompany().getName() + ".");
        return offer;
    }

    @Transactional
    public Offer decline(User student, Long offerId) {
        Offer offer = get(offerId);
        Application a = applications.get(offer.getApplication().getId());
        guard.assertStudentOwns(student, a);
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This offer is " + offer.getStatus().name().toLowerCase() + ".");
        }
        offer.setStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(LocalDateTime.now(clock));
        applications.transition(a, ApplicationState.REJECTED);
        notifyRecruiters(a.getPosting(), "Offer declined: " + a.getPosting().getTitle(),
                a.getStudent().getFullName() + " declined the offer for " + a.getPosting().getTitle() + " at "
                        + a.getPosting().getCompany().getName() + ".");
        return offer;
    }

    /** OFF-05 and OFF-07: recomputes the statement once the agreement date is known. */
    @Transactional
    public Offer recordAgreement(Long applicationId, java.time.LocalDate receivedOn) {
        Application a = applications.recordAgreement(applicationId, receivedOn);
        Offer offer = offers.findByApplication(a).orElseThrow(() -> new NotFoundException("Offer"));
        int lateDays = (int) Math.max(0, ChronoUnit.DAYS.between(offer.getStartDate(), receivedOn));
        computeAllowance(offer, lateDays);
        return offer;
    }

    @Transactional
    public Offer recalculate(Long offerId) {
        Offer offer = get(offerId);
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new BusinessRuleException("The statement exists only for accepted offers.");
        }
        Application a = offer.getApplication();
        int lateDays = a.getAgreementReceivedOn() == null ? 0
                : (int) Math.max(0, ChronoUnit.DAYS.between(offer.getStartDate(), a.getAgreementReceivedOn()));
        computeAllowance(offer, lateDays);
        return offer;
    }

    void computeAllowance(Offer offer, int lateDays) {
        AllowanceResult result = engine.allowance(mapper.toAllowanceRequest(offer, lateDays));
        offer.setAllowanceTotal(result.total());
        try {
            offer.setAllowanceLines(json.writeValueAsString(result.lines()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void tryComputeAllowance(Offer offer, int lateDays) {
        try {
            computeAllowance(offer, lateDays);
        } catch (EngineUnavailableException ex) {
            offer.setAllowanceTotal(null);
            offer.setAllowanceLines(null);
            log.warn("Allowance for offer {} not computed: matching service unavailable", offer.getId());
        }
    }

    public List<AllowanceLine> linesOf(Offer offer) {
        if (offer == null || offer.getAllowanceLines() == null) {
            return List.of();
        }
        try {
            return json.readValue(offer.getAllowanceLines(), new TypeReference<List<AllowanceLine>>() { });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /** OFF-02: expires unanswered offers. */
    @Transactional
    public int expireOverdue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Offer> overdue = offers.findByStatusAndRespondByBefore(OfferStatus.PENDING, now);
        for (Offer o : overdue) {
            o.setStatus(OfferStatus.EXPIRED);
            Application a = o.getApplication();
            if (a.getState() == ApplicationState.OFFERED) {
                applications.move(a, ApplicationState.WITHDRAWN);
            }
            applications.notifyStudent(a, "Offer expired: " + a.getPosting().getTitle(),
                    "The offer for " + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName()
                            + " expired without an answer.");
            notifyRecruiters(a.getPosting(), "Offer expired: " + a.getPosting().getTitle(),
                    "The offer to " + a.getStudent().getFullName() + " for " + a.getPosting().getTitle() + " at "
                            + a.getPosting().getCompany().getName() + " expired without an answer.");
        }
        return overdue.size();
    }

    /** NOT-04: reminder 24 hours before the response deadline. */
    @Transactional
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (Offer o : offers.findByStatusAndReminderSentFalse(OfferStatus.PENDING)) {
            if (!now.isBefore(o.getRespondBy().minusHours(24)) && now.isBefore(o.getRespondBy())) {
                Application a = o.getApplication();
                applications.notifyStudent(a, "Reminder: answer your offer for " + a.getPosting().getTitle(),
                        "The offer for " + a.getPosting().getTitle() + " at " + a.getPosting().getCompany().getName()
                                + " expires on " + o.getRespondBy().format(DISPLAY) + ".");
                sent++;
            }
        }
        return sent;
    }

    private void notifyRecruiters(Posting posting, String subject, String body) {
        for (User r : users.findByCompanyAndRole(posting.getCompany(), Role.RECRUITER)) {
            notifications.notify(r, subject, body);
        }
    }

    public BigDecimal totalOf(Offer offer) {
        return offer == null ? null : offer.getAllowanceTotal();
    }
}
