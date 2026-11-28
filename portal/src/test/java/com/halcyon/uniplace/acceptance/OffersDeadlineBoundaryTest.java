package com.halcyon.uniplace.acceptance;

import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceLine;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.service.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * OFF-02 and ADM-01: response-deadline boundaries with a fixed clock (no waiting 72 hours).
 * "Now" is always 20 Nov 2026 14:30 (Asia/Qatar).
 * AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
 */
public class OffersDeadlineBoundaryTest extends IntegrationTest {
    static final ZoneId ZONE = ZoneId.of("Asia/Qatar");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 14, 30);

    @Autowired OfferService service;
    @Autowired OfferRepository offers;
    @Autowired SettingsService settings;
    @MockBean EngineClient engine;
    @MockBean Clock fixedClock;

    Posting posting;
    Student student;
    User owner;

    @BeforeEach
    void fixture() {
        Instant instant = NOW.atZone(ZONE).toInstant();
        when(fixedClock.instant()).thenReturn(instant);
        when(fixedClock.millis()).thenReturn(instant.toEpochMilli());
        when(fixedClock.getZone()).thenReturn(ZONE);

        Company c = company("QA Deadlines", CompanyStatus.APPROVED);
        posting = openPosting(c, "QA Deadline Internship", List.of("Java"), 1);
        student = student("3.00", List.of("Java"));
        owner = studentUser(student);
        recruiter(c);
        when(engine.allowance(any())).thenReturn(new AllowanceResult(
                new BigDecimal("9000.00"),
                List.of(new AllowanceLine("Total", new BigDecimal("9000.00")))));
    }

    /** A PENDING offer on an OFFERED application whose deadline is respondBy. */
    private Offer pendingOffer(LocalDateTime respondBy) {
        Application a = application(student, posting, ApplicationState.OFFERED, 70);
        return offers.save(new Offer(a, new BigDecimal("3000"), posting.getStartDate(), posting.getEndDate(),
                30, respondBy.minusHours(72), respondBy));
    }

    private ApplicationState stateOf(Offer o) {
        return applications.findById(o.getApplication().getId()).orElseThrow().getState();
    }

    private OfferStatus statusOf(Offer o) {
        return offers.findById(o.getId()).orElseThrow().getStatus();
    }

    // ---- OFF-02: acceptance before, exactly at and after the deadline ----

    @Test
    void D01_acceptanceOneMinuteBeforeDeadlineSucceeds() {
        Offer o = pendingOffer(NOW.plusMinutes(1));
        service.accept(owner, o.getId());
        assertEquals(OfferStatus.ACCEPTED, statusOf(o));
        assertEquals(ApplicationState.ACCEPTED, stateOf(o));
    }

    @Test
    void D02_acceptanceExactlyAtDeadlineSucceeds() {
        Offer o = pendingOffer(NOW);
        service.accept(owner, o.getId());
        assertEquals(OfferStatus.ACCEPTED, statusOf(o));
        assertEquals(ApplicationState.ACCEPTED, stateOf(o));
    }

    @Test
    void D03_acceptanceOneMinuteAfterDeadlineIsRefused() {
        Offer o = pendingOffer(NOW.minusMinutes(1));
        assertThrows(BusinessRuleException.class, () -> service.accept(owner, o.getId()));
    }

    // ---- OFF-02: expiry of an unanswered offer ----

    @Test
    void D04_unansweredOfferPastDeadlineExpiresAndIsWithdrawn() {
        Offer o = pendingOffer(NOW.minusMinutes(1));
        assertEquals(1, service.expireOverdue());
        assertEquals(OfferStatus.EXPIRED, statusOf(o));
        assertEquals(ApplicationState.WITHDRAWN, stateOf(o));
    }

    @Test
    void D05_offerExactlyAtDeadlineIsNotExpiredYet() {
        Offer o = pendingOffer(NOW);
        assertEquals(0, service.expireOverdue());
        assertEquals(OfferStatus.PENDING, statusOf(o));
        assertEquals(ApplicationState.OFFERED, stateOf(o));
    }

    // ---- ADM-01: a changed response time applies to new deadlines without restart ----

    @Test
    void D06_changedResponseHoursAppliesWithoutRestart() {
        settings.set(SettingsService.OFFER_RESPONSE_HOURS, "96");
        LocalDateTime issued = LocalDateTime.of(2026, 11, 12, 0, 0);
        assertEquals(issued.plusHours(96), service.responseDeadline(issued));
    }

    @Test
    void D07_changedResponseHoursKeepsPartialDays() {
        settings.set(SettingsService.OFFER_RESPONSE_HOURS, "73");
        LocalDateTime issued = LocalDateTime.of(2026, 11, 12, 0, 0);
        assertEquals(issued.plusHours(73), service.responseDeadline(issued));
    }
}