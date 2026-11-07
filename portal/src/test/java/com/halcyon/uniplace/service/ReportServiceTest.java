package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest extends IntegrationTest {

    @Autowired ReportService reports;

    @Test
    void csvHasAHeaderAndEscapesCommas() {
        ReportService.Table t = new ReportService.Table("T", List.of("Company", "Score"),
                List.of(List.of("Gulf Bank, Doha", "70.0")));
        assertEquals("Company,Score\r\n\"Gulf Bank, Doha\",70.0\r\n", ReportService.toCsv(t));
    }

    @Test
    void averageScorePerCompany() {
        Company c = company("QTech", CompanyStatus.APPROVED);
        Posting p = openPosting(c, "Intern", List.of("Java"), 1);
        Student a = student("3.00", List.of("Java"));
        Student b = student("3.00", List.of("Java"));
        application(a, p, ApplicationState.SUBMITTED, 60);
        application(b, p, ApplicationState.SUBMITTED, 81);

        ReportService.Table t = reports.averageScorePerCompany();

        assertEquals(List.of(List.of("QTech", "70.5", "2")), t.rows());
    }
}
