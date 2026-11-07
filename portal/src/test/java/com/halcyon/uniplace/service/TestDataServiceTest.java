package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDataServiceTest extends IntegrationTest {

    @Autowired TestDataService service;

    @Test
    void generatesTheRequestedNumberOfApplications() {
        company("Volume Co", CompanyStatus.APPROVED);

        TestDataService.Result r = service.generate(40);

        assertEquals(40, r.applications());
        assertEquals(40, applications.count());
        assertTrue(r.students() > 0 && r.postings() > 0);
        assertTrue(students.findAll().stream().allMatch(s -> s.getFullName().startsWith("Generated")));
    }

    @Test
    void theCountMustBeBetweenOneAndTenThousand() {
        company("Volume Co", CompanyStatus.APPROVED);
        assertThrows(BusinessRuleException.class, () -> service.generate(0));
        assertThrows(BusinessRuleException.class, () -> service.generate(TestDataService.MAX_PER_RUN + 1));
    }

    @Test
    void anApprovedCompanyIsNeeded() {
        company("Pending Co", CompanyStatus.PENDING);
        assertThrows(BusinessRuleException.class, () -> service.generate(5));
    }
}
