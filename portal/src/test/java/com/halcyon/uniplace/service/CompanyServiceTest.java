package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.NotificationRepository;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyServiceTest extends IntegrationTest {

    @Autowired CompanyService service;
    @Autowired NotificationRepository notifications;

    private User recruiterWithoutCompany(String username) {
        return users.save(new User(username, "{noop}unused", Role.RECRUITER, "Recruiter", username + "@test.qa"));
    }

    @Test
    void registeredCompanyIsPendingAndLinkedToTheRecruiter() {
        User r = recruiterWithoutCompany("newrecruiter");

        Company c = service.register(r, "  Falcon Tech ", "Technology", "hr@falcon.qa");

        assertEquals(CompanyStatus.PENDING, c.getStatus());
        assertEquals("Falcon Tech", c.getName());
        assertEquals(c.getId(), users.findById(r.getId()).orElseThrow().getCompany().getId());
    }

    @Test
    void companyNamesAreUniqueIgnoringCaseOnRegistration() {
        company("Falcon Tech", CompanyStatus.APPROVED);
        User r = recruiterWithoutCompany("secondrecruiter");

        assertThrows(BusinessRuleException.class, () -> service.register(r, " falcon tech", "Technology", "a@b.qa"));
    }

    @Test
    void contactEmailMustBeValid() {
        User r = recruiterWithoutCompany("thirdrecruiter");
        assertThrows(BusinessRuleException.class, () -> service.register(r, "Oryx Labs", "Technology", "not-an-email"));
    }

    @Test
    void aRecruiterRegistersOnlyOneCompany() {
        User r = recruiter(company("Oryx Labs", CompanyStatus.APPROVED));
        assertThrows(BusinessRuleException.class, () -> service.register(r, "Second Co", "Technology", "x@y.qa"));
    }

    @Test
    void recruiterUpdatesTheCompanyDetails() {
        User r = recruiter(company("Oryx Labs", CompanyStatus.APPROVED));

        Company c = service.update(r, "Oryx Laboratories", "Research", "jobs@oryx.qa");

        assertEquals("Oryx Laboratories", c.getName());
        assertEquals("Research", c.getSector());
    }

    @Test
    void approvalNotifiesTheRecruiters() {
        Company c = company("Pending Co", CompanyStatus.PENDING);
        User r = recruiter(c);

        service.approve(c.getId());

        assertEquals(CompanyStatus.APPROVED, companies.findById(c.getId()).orElseThrow().getStatus());
        assertEquals(1, notifications.findAll().stream().filter(n -> n.getRecipient().getId().equals(r.getId())).count());
    }

    @Test
    void suspensionPutsActiveApplicationsOnHold() {
        Company c = company("Suspended Co", CompanyStatus.APPROVED);
        Posting p = openPosting(c, "Intern", List.of("Java"), 1);
        User u = studentUser(student("3.00", List.of("Java")));
        application(u.getStudent(), p, ApplicationState.SUBMITTED, 70);

        service.suspend(c.getId());

        assertEquals(CompanyStatus.SUSPENDED, companies.findById(c.getId()).orElseThrow().getStatus());
        assertTrue(notifications.findAll().stream().anyMatch(n -> n.getRecipient().getId().equals(u.getId())
                && n.getSubject().contains("on hold")));
    }

    @Test
    void onlyAnApprovedCompanyCanBeSuspended() {
        Company c = company("Pending Co", CompanyStatus.PENDING);
        assertThrows(BusinessRuleException.class, () -> service.suspend(c.getId()));
    }

    @Test
    void companiesAreListedByName() {
        company("Zeta Co", CompanyStatus.APPROVED);
        company("Alpha Co", CompanyStatus.PENDING);

        assertEquals(List.of("Alpha Co", "Zeta Co"), service.all().stream().map(Company::getName).toList());
    }
}
