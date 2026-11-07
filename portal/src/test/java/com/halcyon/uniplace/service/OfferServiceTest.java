package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceLine;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OfferServiceTest extends IntegrationTest {

    @Autowired OfferService service;
    @Autowired OfferRepository offers;
    @MockBean EngineClient engine;

    Posting posting;
    Student student;
    User studentUser;
    User recruiter;

    @BeforeEach
    void setUp() {
        Company company = company("Gulf Bank", CompanyStatus.APPROVED);
        posting = openPosting(company, "Risk Intern", List.of("Python"), 2);
        student = student("3.00", List.of("Python"));
        studentUser = studentUser(student);
        recruiter = recruiter(company);
        when(engine.allowance(any())).thenReturn(new AllowanceResult(new BigDecimal("9000.00"),
                List.of(new AllowanceLine("Total", new BigDecimal("9000.00")))));
    }

    private OfferForm form() {
        OfferForm f = new OfferForm();
        f.setMonthlyAllowance(new BigDecimal("3000"));
        f.setStartDate(posting.getStartDate());
        f.setEndDate(posting.getEndDate());
        f.setWeeklyHours(40);
        return f;
    }

    @Test
    void offerIsIssuedAfterAnInterview() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);

        Offer o = service.issue(recruiter, a.getId(), form());

        assertEquals(OfferStatus.PENDING, o.getStatus());
        assertEquals(ApplicationState.OFFERED, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Test
    void allowanceCannotExceedThePosting() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);
        OfferForm f = form();
        f.setMonthlyAllowance(new BigDecimal("3000.01"));

        assertThrows(BusinessRuleException.class, () -> service.issue(recruiter, a.getId(), f));
    }

    @Test
    void offerDatesMustBeWithinThePosting() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);
        OfferForm f = form();
        f.setStartDate(posting.getStartDate().minusDays(1));

        assertThrows(BusinessRuleException.class, () -> service.issue(recruiter, a.getId(), f));
    }

    @Test
    void acceptingTakesASeatAndStoresTheStatement() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);
        Offer o = service.issue(recruiter, a.getId(), form());

        service.accept(studentUser, o.getId());

        Offer saved = offers.findById(o.getId()).orElseThrow();
        assertEquals(OfferStatus.ACCEPTED, saved.getStatus());
        assertEquals(0, new BigDecimal("9000.00").compareTo(saved.getAllowanceTotal()));
        assertEquals(1, postings.findById(posting.getId()).orElseThrow().getSeatsTaken());
        assertEquals(ApplicationState.ACCEPTED, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Test
    void decliningRejectsTheApplication() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);
        Offer o = service.issue(recruiter, a.getId(), form());

        service.decline(studentUser, o.getId());

        assertEquals(ApplicationState.REJECTED, applications.findById(a.getId()).orElseThrow().getState());
        assertEquals(OfferStatus.DECLINED, offers.findById(o.getId()).orElseThrow().getStatus());
    }

    @Test
    void anotherStudentCannotAccept() {
        Application a = application(student, posting, ApplicationState.INTERVIEW, 70);
        Offer o = service.issue(recruiter, a.getId(), form());
        User other = studentUser(student("3.50", List.of("Python")));

        assertThrows(ForbiddenException.class, () -> service.accept(other, o.getId()));
    }
}
