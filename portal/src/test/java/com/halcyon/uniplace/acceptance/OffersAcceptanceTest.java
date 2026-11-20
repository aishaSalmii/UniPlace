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
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** AI-assisted starter. Review and run before counting as team execution evidence. */
public class OffersAcceptanceTest extends IntegrationTest {
    @Autowired OfferService service;
    @Autowired OfferRepository offers;
    @MockBean EngineClient engine;
    Posting posting;
    Student student;
    User owner;
    User recruiter;

    @BeforeEach
    void fixture() {
        Company c = company("QA Offers", CompanyStatus.APPROVED);
        posting = openPosting(c, "QA Internship", List.of("Java"), 1);
        student = student("3.00", List.of("Java"));
        owner = studentUser(student);
        recruiter = recruiter(c);
        when(engine.allowance(any())).thenReturn(new AllowanceResult(
                new BigDecimal("9000.00"),
                List.of(new AllowanceLine("Total", new BigDecimal("9000.00")))));
    }

    private OfferForm validForm() { return service.formFor(application); }
    private Application application;

    @Test
    void O01_interviewApplicationCanReceiveOffer() {
        application = application(student, posting, ApplicationState.INTERVIEW, 70);
        Offer o = service.issue(recruiter, application.getId(), validForm());
        assertEquals(OfferStatus.PENDING, o.getStatus());
        assertEquals(ApplicationState.OFFERED,
                applications.findById(application.getId()).orElseThrow().getState());
    }

    @Test
    void O02_submittedApplicationCannotReceiveOffer() {
        application = application(student, posting, ApplicationState.SUBMITTED, 70);
        assertThrows(BusinessRuleException.class,
                () -> service.issue(recruiter, application.getId(), validForm()));
    }

    @Test
    void O03_allowanceAbovePostingIsRefused() {
        application = application(student, posting, ApplicationState.INTERVIEW, 70);
        OfferForm f = validForm();
        f.setMonthlyAllowance(posting.getMonthlyAllowance().add(new BigDecimal("0.01")));
        assertThrows(BusinessRuleException.class,
                () -> service.issue(recruiter, application.getId(), f));
    }

    @Test
    void O04_responseDeadlinePreservesTime() {
        LocalDateTime issued = LocalDateTime.of(2026, 11, 12, 14, 30);
        assertEquals(issued.plusHours(72), service.responseDeadline(issued));
    }

    @Test
    void O05_acceptanceAfterDeadlineIsRefused() {
        application = application(student, posting, ApplicationState.OFFERED, 70);
        LocalDateTime now = LocalDateTime.now(clock);
        Offer o = offers.save(new Offer(application, new BigDecimal("3000"),
                posting.getStartDate(), posting.getEndDate(), 30,
                now.minusHours(80), now.minusHours(1)));
        assertThrows(BusinessRuleException.class, () -> service.accept(owner, o.getId()));
    }

    @Test
    void O06_acceptanceWithdrawsOtherActiveApplication() {
        application = application(student, posting, ApplicationState.INTERVIEW, 70);
        Posting otherPosting = openPosting(posting.getCompany(), "Another Role", List.of("Java"), 1);
        Application other = application(student, otherPosting, ApplicationState.SUBMITTED, 70);
        Offer o = service.issue(recruiter, application.getId(), validForm());
        service.accept(owner, o.getId());
        assertEquals(ApplicationState.WITHDRAWN,
                applications.findById(other.getId()).orElseThrow().getState());
    }

    @Test
    void O07_lastSeatRejectsRemainingActiveApplications() {
        application = application(student, posting, ApplicationState.INTERVIEW, 70);
        Student otherStudent = student("3.20", List.of("Java"));
        Application other = application(otherStudent, posting, ApplicationState.SUBMITTED, 75);
        Offer o = service.issue(recruiter, application.getId(), validForm());
        service.accept(owner, o.getId());
        assertEquals(PostingStatus.FILLED, postings.findById(posting.getId()).orElseThrow().getStatus());
        assertEquals(ApplicationState.REJECTED,
                applications.findById(other.getId()).orElseThrow().getState());
    }

    @Test
    void O08_otherStudentCannotAcceptOffer() {
        application = application(student, posting, ApplicationState.INTERVIEW, 70);
        Offer o = service.issue(recruiter, application.getId(), validForm());
        User unrelated = studentUser(student("3.50", List.of("Java")));
        assertThrows(ForbiddenException.class, () -> service.accept(unrelated, o.getId()));
    }
}
