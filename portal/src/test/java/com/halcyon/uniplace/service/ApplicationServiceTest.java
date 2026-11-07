package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.repository.AuditRepository;
import com.halcyon.uniplace.repository.NotificationRepository;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest extends IntegrationTest {

    static final String LETTER = "I am applying because this internship matches my studies in information systems "
            + "and my experience with databases in two course projects.";

    @Autowired ApplicationService service;
    @Autowired NotificationRepository notifications;
    @Autowired AuditRepository audit;
    @MockBean EngineClient engine;

    Company company;
    Posting posting;
    Student student;
    User studentUser;
    User recruiter;

    @BeforeEach
    void setUp() {
        company = company("QTech", CompanyStatus.APPROVED);
        posting = openPosting(company, "Backend Intern", List.of("Java", "SQL"), 2);
        student = student("3.20", List.of("Java", "SQL"));
        studentUser = studentUser(student);
        recruiter = recruiter(company);
        validDocuments(student);
        when(engine.checkEligibility(any())).thenReturn(EligibilityResult.ok());
        when(engine.match(any())).thenReturn(new MatchResult(70, List.of("Java", "SQL"), List.of(), List.of()));
    }

    @Test
    void submitStoresTheEngineScoreAndNotifies() {
        Application a = service.submit(studentUser, posting.getId(), LETTER);

        assertEquals(ApplicationState.SUBMITTED, a.getState());
        assertEquals(70, a.getMatchScore());
        assertEquals(2, notifications.count());
    }

    @Test
    void letterMustHaveAtLeast100Characters() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submit(studentUser, posting.getId(), "Too short."));
        assertTrue(ex.getMessage().contains("between 100 and 2000"));
        verify(engine, never()).checkEligibility(any());
    }

    @Test
    void letterMustHaveAtMost2000Characters() {
        assertThrows(BusinessRuleException.class,
                () -> service.submit(studentUser, posting.getId(), "x".repeat(2001)));
    }

    @Test
    void secondActiveApplicationToTheSamePostingIsRefused() {
        service.submit(studentUser, posting.getId(), LETTER);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submit(studentUser, posting.getId(), LETTER));
        assertEquals("You already have an active application for this posting.", ex.getMessage());
    }

    @Test
    void ineligibleStudentGetsTheEngineReasons() {
        when(engine.checkEligibility(any())).thenReturn(new EligibilityResult(false, List.of("CV is missing")));

        NotEligibleException ex = assertThrows(NotEligibleException.class,
                () -> service.submit(studentUser, posting.getId(), LETTER));
        assertEquals(List.of("CV is missing"), ex.getReasons());
        assertEquals(0, applications.count());
    }

    @Test
    void goodStandingStudentUsesStandardLimit() {
        assertEquals(5, service.activeLimitFor(student));
    }

    @Test
    void probationStudentUsesStandardLimit() {
        Student weak = student("1.80", List.of("Excel"));
        assertEquals(5, service.activeLimitFor(weak));
    }

    @Test
    void activeApplicationsAreCounted() {
        Posting other = openPosting(company, "Other", List.of("Java"), 1);
        application(student, posting, ApplicationState.SUBMITTED, 50);
        application(student, other, ApplicationState.INTERVIEW, 50);

        assertEquals(2, service.countActive(student));
    }

    @Test
    void withdrawnApplicationsRemainInHistoryCount() {
        for (int i = 0; i < 4; i++) {
            application(student, openPosting(company, "Old " + i, List.of("Java"), 1), ApplicationState.WITHDRAWN, 40);
        }
        application(student, posting, ApplicationState.SUBMITTED, 40);

        assertEquals(5, service.countActive(student));
    }

    @Test
    void shortlistAboveThresholdNeedsNoJustification() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 85);

        service.shortlist(recruiter, a.getId(), null);

        assertEquals(ApplicationState.SHORTLISTED, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Test
    void shortlistBelowThresholdNeedsJustification() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 35);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.shortlist(recruiter, a.getId(), " "));
        assertTrue(ex.getMessage().contains("below the shortlist threshold 60"));
    }

    @Test
    void shortlistOverrideIsAudited() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 35);

        service.shortlist(recruiter, a.getId(), "Strong portfolio shown at the career fair");

        assertEquals(1, audit.count());
        assertEquals("Strong portfolio shown at the career fair",
                applications.findById(a.getId()).orElseThrow().getOverrideJustification());
    }

    @Test
    void recruiterOfAnotherCompanyCannotShortlist() {
        User stranger = recruiter(company("Other Co", CompanyStatus.APPROVED));
        Application a = application(student, posting, ApplicationState.SUBMITTED, 85);

        assertThrows(ForbiddenException.class, () -> service.shortlist(stranger, a.getId(), null));
    }

    @Test
    void studentCanWithdrawASubmittedApplication() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 50);

        service.withdraw(studentUser, a.getId());

        assertEquals(ApplicationState.WITHDRAWN, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Test
    void rejectedApplicationCannotBeShortlisted() {
        Application a = application(student, posting, ApplicationState.REJECTED, 90);

        assertThrows(BusinessRuleException.class, () -> service.shortlist(recruiter, a.getId(), null));
    }
}
