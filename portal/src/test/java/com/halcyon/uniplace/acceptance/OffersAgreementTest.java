package com.halcyon.uniplace.acceptance;

import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceLine;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceRequest;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.service.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OFF-07 (agreement date recalculation) and ADM-01 (allowance Settings reach the Engine).
 * Fixed clock: "now" is 10 Feb 2027 10:00 (Asia/Qatar); the accepted placement runs
 * 1 Jan 2027 to 31 Mar 2027, so late agreement dates can be recorded without waiting.
 * The Engine is mocked: these tests check what the Portal sends and stores, while the
 * penalty arithmetic itself is covered by tests/acceptance/test_allowance_boundaries.py.
 * AI-assisted (Claude); reviewed and executed by Areeg AlGhazali.
 */
public class OffersAgreementTest extends IntegrationTest {
    static final ZoneId ZONE = ZoneId.of("Asia/Qatar");
    static final LocalDateTime NOW = LocalDateTime.of(2027, 2, 10, 10, 0);
    static final LocalDate START = LocalDate.of(2027, 1, 1);
    static final LocalDate END = LocalDate.of(2027, 3, 31);

    @Autowired OfferService service;
    @Autowired OfferRepository offers;
    @Autowired SettingsService settings;
    @MockBean EngineClient engine;
    @MockBean Clock fixedClock;

    Application placed;
    Offer offer;

    @BeforeEach
    void fixture() {
        Instant instant = NOW.atZone(ZONE).toInstant();
        when(fixedClock.instant()).thenReturn(instant);
        when(fixedClock.millis()).thenReturn(instant.toEpochMilli());
        when(fixedClock.getZone()).thenReturn(ZONE);
        when(engine.allowance(any())).thenReturn(new AllowanceResult(
                new BigDecimal("8550.00"),
                List.of(new AllowanceLine("Base allowance (3 months x 3000.00)", new BigDecimal("9000.00")),
                        new AllowanceLine("Late documents penalty (1 started weeks, 5%)", new BigDecimal("-450.00")),
                        new AllowanceLine("Total", new BigDecimal("8550.00")))));

        Company c = company("QA Agreements", CompanyStatus.APPROVED);
        Posting posting = openPosting(c, "QA Agreement Internship", List.of("Java"), 1);
        Student student = student("3.00", List.of("Java"));
        studentUser(student);
        recruiter(c);
        placed = application(student, posting, ApplicationState.ACCEPTED, 70);
        offer = new Offer(placed, new BigDecimal("3000"), START, END, 29,
                NOW.minusDays(60), NOW.minusDays(57));
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(NOW.minusDays(58));
        offer = offers.save(offer);
    }

    /** The last request the Portal sent to the Engine. */
    private AllowanceRequest lastRequest() {
        ArgumentCaptor<AllowanceRequest> captor = ArgumentCaptor.forClass(AllowanceRequest.class);
        verify(engine, atLeastOnce()).allowance(captor.capture());
        return captor.getValue();
    }

    private int lateDaysSentFor(LocalDate receivedOn) {
        clearInvocations(engine);
        service.recordAgreement(placed.getId(), receivedOn);
        return lastRequest().lateDays;
    }

    // ---- OFF-07: late days sent to the Engine when the agreement date is recorded ----

    @Test
    void G01_noAgreementDateMeansNoLateDays() {
        clearInvocations(engine);
        service.recalculate(offer.getId());
        assertEquals(0, lastRequest().lateDays);
    }

    @Test
    void G02_agreementOnStartDateIsNotLate() {
        assertEquals(0, lateDaysSentFor(START));
    }

    @Test
    void G03_agreementBeforeStartDateIsNotLate() {
        assertEquals(0, lateDaysSentFor(START.minusDays(5)));
    }

    @Test
    void G04_agreementOneDayAfterStartIsOneLateDay() {
        assertEquals(1, lateDaysSentFor(START.plusDays(1)));
    }

    @Test
    void G05_agreementSevenDaysAfterStartIsSevenLateDays() {
        assertEquals(7, lateDaysSentFor(START.plusDays(7)));
    }

    @Test
    void G06_agreementEightDaysAfterStartIsEightLateDays() {
        assertEquals(8, lateDaysSentFor(START.plusDays(8)));
    }

    @Test
    void G07_recordingTheAgreementStoresDateAndRecalculatedStatement() {
        service.recordAgreement(placed.getId(), START.plusDays(3));
        assertEquals(START.plusDays(3),
                applications.findById(placed.getId()).orElseThrow().getAgreementReceivedOn());
        Offer saved = offers.findById(offer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("8550.00").compareTo(saved.getAllowanceTotal()));
        assertTrue(saved.getAllowanceLines().contains("Late documents penalty"));
    }

    @Test
    void G08_futureAgreementDateIsRefused() {
        assertThrows(BusinessRuleException.class,
                () -> service.recordAgreement(placed.getId(), NOW.toLocalDate().plusDays(1)));
    }

    // ---- ADM-01: allowance Settings are sent to the Engine without restart ----

    @Test
    void G09_changedTransportAndPenaltySettingsAreSentToEngine() {
        settings.set(SettingsService.TRANSPORT_SUPPLEMENT, "400");
        settings.set(SettingsService.PENALTY_RATE, "0.10");
        settings.set(SettingsService.PENALTY_CAP, "0.30");
        clearInvocations(engine);
        service.recalculate(offer.getId());
        AllowanceRequest r = lastRequest();
        assertEquals(0, new BigDecimal("400").compareTo(r.transportSupplement));
        assertEquals(0, new BigDecimal("0.10").compareTo(r.penaltyRate));
        assertEquals(0, new BigDecimal("0.30").compareTo(r.penaltyCap));
    }

    @Test
    void G10_changedMonthlyCapSettingIsSentToEngine() {
        settings.set(SettingsService.MONTHLY_CAP, "4000");
        clearInvocations(engine);
        service.recalculate(offer.getId());
        AllowanceRequest r = lastRequest();
        assertNotNull(r.monthlyCap, "monthlyCap is not sent to the Engine");
        assertEquals(0, new BigDecimal("4000").compareTo(r.monthlyCap));
    }
}