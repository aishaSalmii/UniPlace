package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingServiceTest extends IntegrationTest {

    @Autowired PostingService service;

    Company company;
    User recruiter;

    @BeforeEach
    void setUp() {
        company = company("Pearl Logistics", CompanyStatus.APPROVED);
        recruiter = recruiter(company);
    }

    private PostingForm validForm() {
        PostingForm f = service.newForm();
        f.setTitle("Supply Chain Intern");
        f.setDescription("Work with the planning team.");
        f.setRequiredSkills("Excel, SQL");
        f.setNiceToHaveSkills("Power BI");
        return f;
    }

    @Test
    void newPostingStartsAsDraftWithDefaultMinimums() {
        Posting p = service.create(recruiter, validForm());

        assertEquals(PostingStatus.DRAFT, p.getStatus());
        assertEquals(0, new BigDecimal("2.00").compareTo(p.getMinGpa()));
        assertEquals(45, p.getMinCredits());
        assertEquals(List.of("Excel", "SQL"), p.getRequiredSkills());
    }

    @Test
    void atLeastOneRequiredSkill() {
        PostingForm f = validForm();
        f.setRequiredSkills(" , ");
        assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
    }

    @Test
    void seatsBetweenOneAndTwenty() {
        PostingForm f = validForm();
        f.setSeats(21);
        assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
        f.setSeats(0);
        assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
    }

    @Test
    void startMustBeAfterDeadline() {
        PostingForm f = validForm();
        f.setStartDate(f.getDeadline());
        assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
    }

    @Test
    void internshipLastsAtLeastFourWeeks() {
        PostingForm f = validForm();
        f.setEndDate(f.getStartDate().plusDays(20));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
        assertEquals("The internship must last at least 4 weeks.", ex.getMessage());
    }

    @Test
    void internshipLastsAtMostSixMonths() {
        PostingForm f = validForm();
        f.setEndDate(f.getStartDate().plusMonths(6).plusDays(1));
        assertThrows(BusinessRuleException.class, () -> service.create(recruiter, f));
    }

    @Test
    void publishMakesADraftOpen() {
        Posting p = service.create(recruiter, validForm());
        assertEquals(PostingStatus.OPEN, service.publish(p.getId()).getStatus());
    }

    @Test
    void pendingCompanyCannotPublish() {
        Company pending = company("New Co", CompanyStatus.PENDING);
        Posting p = service.create(recruiter(pending), validForm());
        assertThrows(BusinessRuleException.class, () -> service.publish(p.getId()));
    }

    @Test
    void openPostingAcceptsApplicationsBeforeItsDeadline() {
        Posting p = openPosting(company, "Intern", List.of("Excel"), 1);
        p.setDeadline(today().plusDays(1));
        assertTrue(service.acceptsApplications(service.get(postings.save(p).getId()), today()));
    }

    @Test
    void postingDoesNotAcceptApplicationsAfterItsDeadline() {
        Posting p = openPosting(company, "Intern", List.of("Excel"), 1);
        p.setDeadline(today().minusDays(1));
        assertFalse(service.acceptsApplications(service.get(postings.save(p).getId()), today()));
    }

    @Test
    void draftDoesNotAcceptApplications() {
        Posting p = service.create(recruiter, validForm());
        assertFalse(service.acceptsApplications(service.get(p.getId()), today()));
    }

    @Test
    void searchFiltersBySkillIgnoringCase() {
        openPosting(company, "Excel Intern", List.of("Excel"), 1);
        openPosting(company, "Java Intern", List.of("Java"), 1);

        List<Posting> found = service.search("excel", null, null);

        assertEquals(1, found.size());
        assertEquals("Excel Intern", found.get(0).getTitle());
    }

    @Test
    void searchIsSortedByDeadline() {
        Posting late = openPosting(company, "Late", List.of("Excel"), 1);
        late.setDeadline(today().plusDays(20));
        postings.save(late);
        openPosting(company, "Early", List.of("Excel"), 1);

        assertEquals("Early", service.search(null, null, null).get(0).getTitle());
    }

    @Test
    void closeExpiredClosesOnlyPastDeadlines() {
        Posting past = openPosting(company, "Past", List.of("Excel"), 1);
        past.setDeadline(today().minusDays(2));
        postings.save(past);
        openPosting(company, "Future", List.of("Excel"), 1);

        assertEquals(1, service.closeExpired());
        assertEquals(PostingStatus.CLOSED, postings.findById(past.getId()).orElseThrow().getStatus());
    }
}
