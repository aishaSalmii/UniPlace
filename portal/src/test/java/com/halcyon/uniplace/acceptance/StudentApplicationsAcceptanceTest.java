package com.halcyon.uniplace.acceptance;

import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.engine.EngineUnavailableException;
import com.halcyon.uniplace.service.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Requirements-based, AI-assisted tests. Record actual execution results before reporting. */
public class StudentApplicationsAcceptanceTest extends IntegrationTest {
    @Autowired ApplicationService service;
    @MockBean EngineClient engine;
    Company company;
    Posting posting;
    Student student;
    User owner;
    User recruiter;

    @BeforeEach
    void fixture() {
        company = company("QA Applications", CompanyStatus.APPROVED);
        posting = openPosting(company, "QA Internship", List.of("Java"), 2);
        student = student("3.20", List.of("Java"));
        owner = studentUser(student);
        recruiter = recruiter(company);
        validDocuments(student);
        when(engine.checkEligibility(any())).thenReturn(EligibilityResult.ok());
        when(engine.match(any())).thenReturn(new MatchResult(70, List.of("Java"), List.of(), List.of()));
    }

    @Test void A01_letter99IsRefused() {
        assertThrows(BusinessRuleException.class, () -> service.submit(owner, posting.getId(), "x".repeat(99)));
        assertEquals(0, applications.count());
    }
    @Test void A02_letter100IsAccepted() {
        Application a = service.submit(owner, posting.getId(), "x".repeat(100));
        assertEquals(ApplicationState.SUBMITTED, a.getState());
        assertEquals(70, a.getMatchScore());
    }
    @Test void A03_letter2000IsAccepted() {
        assertEquals(ApplicationState.SUBMITTED,
                service.submit(owner, posting.getId(), "x".repeat(2000)).getState());
    }
    @Test void A04_letter2001IsRefused() {
        assertThrows(BusinessRuleException.class, () -> service.submit(owner, posting.getId(), "x".repeat(2001)));
        assertEquals(0, applications.count());
    }
    @Test void A05_withdrawnDoesNotCountAsActive() {
        application(student, posting, ApplicationState.WITHDRAWN, 70);
        assertEquals(0, service.countActive(student));
    }
    @Test void A06_probationLimitIsTwo() {
        Student weak = student("1.80", List.of("Java"));
        assertEquals(2, service.activeLimitFor(weak));
    }
    @Test void A07_reapplyAfterRejectedIsAllowed() {
        application(student, posting, ApplicationState.REJECTED, 70);
        Application next = service.submit(owner, posting.getId(), "x".repeat(120));
        assertEquals(ApplicationState.SUBMITTED, next.getState());
        assertEquals(2, applications.count());
    }
    @Test void A08_reapplyAfterWithdrawnIsAllowed() {
        application(student, posting, ApplicationState.WITHDRAWN, 70);
        assertEquals(ApplicationState.SUBMITTED,
                service.submit(owner, posting.getId(), "x".repeat(120)).getState());
    }
    @Test void A09_secondActiveApplicationIsRefused() {
        service.submit(owner, posting.getId(), "x".repeat(120));
        assertThrows(BusinessRuleException.class,
                () -> service.submit(owner, posting.getId(), "x".repeat(120)));
        assertEquals(1, applications.count());
    }
    @Test void A10_scoreExactlyThresholdNeedsNoOverride() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 60);
        service.shortlist(recruiter, a.getId(), null);
        assertEquals(ApplicationState.SHORTLISTED,
                applications.findById(a.getId()).orElseThrow().getState());
    }
    @Test void A11_acceptedCannotBeWithdrawn() {
        Application a = application(student, posting, ApplicationState.ACCEPTED, 70);
        assertThrows(BusinessRuleException.class, () -> service.withdraw(owner, a.getId()));
        assertEquals(ApplicationState.ACCEPTED,
                applications.findById(a.getId()).orElseThrow().getState());
    }
    @Test void A12_suspendedCompanyPreventsShortlisting() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 80);
        company.setStatus(CompanyStatus.SUSPENDED);
        companies.saveAndFlush(company);
        assertThrows(BusinessRuleException.class,
                () -> service.shortlist(recruiter, a.getId(), null));
    }
    @Test void A13_engineOutageRefusesSubmission() {
        when(engine.checkEligibility(any())).thenThrow(new EngineUnavailableException());
        assertThrows(EngineUnavailableException.class,
                () -> service.submit(owner, posting.getId(), "x".repeat(120)));
        assertEquals(0, applications.count());
    }
    @Test void A14_otherStudentCannotWithdraw() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 70);
        User stranger = studentUser(student("3.50", List.of("Java")));
        assertThrows(ForbiddenException.class, () -> service.withdraw(stranger, a.getId()));
    }
    @Test void A15_paginationReturns20ThenOne() {
        for (int i = 0; i < 21; i++) {
            Posting p = openPosting(company, "History " + i, List.of("Java"), 1);
            application(student, p, ApplicationState.REJECTED, 70);
        }
        assertEquals(20, service.forStudent(student, 0).getNumberOfElements());
        assertEquals(1, service.forStudent(student, 1).getNumberOfElements());
    }
    @Test void A16_everyEligibilityReasonIsRetained() {
        List<String> reasons = List.of("GPA below minimum", "CV is missing");
        when(engine.checkEligibility(any())).thenReturn(new EligibilityResult(false, reasons));
        NotEligibleException e = assertThrows(NotEligibleException.class,
                () -> service.submit(owner, posting.getId(), "x".repeat(120)));
        assertEquals(reasons, e.getReasons());
        assertEquals(0, applications.count());
    }
}
