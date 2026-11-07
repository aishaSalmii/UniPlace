package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchRequest;
import com.halcyon.uniplace.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EngineRequestMapperTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T06:00:00Z"), ZoneId.of("Asia/Qatar"));
    private final EngineRequestMapper mapper = new EngineRequestMapper(Mockito.mock(SettingsService.class), clock);

    private static Posting posting() {
        Posting p = new Posting(new Company("QTech", "Technology", "hr@qtech.qa"), "Intern", "d");
        p.setRequiredSkills(List.of("Java", "SQL"));
        p.setNiceToHaveSkills(List.of("Git"));
        p.setMinGpa(new BigDecimal("2.50"));
        p.setMinCredits(60);
        return p;
    }

    @Test
    void eligibilityRequestCarriesAcademicDataAndPostingMinimums() {
        Student s = new Student("20990001", "Test Student", "t@s.qa", "BSc", new BigDecimal("1.90"), 75, 0);

        EligibilityRequest r = mapper.toEligibilityRequest(s, posting(), List.of(), LocalDate.of(2026, 11, 20));

        assertEquals(new BigDecimal("1.90"), r.gpa);
        assertEquals(75, r.creditsCompleted);
        assertEquals("PROBATION", r.standing);
        assertEquals(60, r.minCredits);
        assertEquals(LocalDate.of(2026, 11, 20), r.asOf);
    }

    @Test
    void documentsAreDescribedWithoutTheirContent() {
        Student s = new Student("20990001", "Test Student", "t@s.qa", "BSc", new BigDecimal("3.00"), 75, 0);
        StudentDocument t = new StudentDocument(s, DocumentType.TRANSCRIPT, "t.pdf", "x.pdf", "application/pdf",
                1234, LocalDate.of(2026, 10, 1), LocalDateTime.now(clock));

        EligibilityRequest r = mapper.toEligibilityRequest(s, posting(), List.of(t), LocalDate.of(2026, 11, 20));

        assertEquals(1, r.documents.size());
        assertEquals("TRANSCRIPT", r.documents.get(0).type);
        assertEquals(1234, r.documents.get(0).sizeBytes);
        assertEquals(LocalDate.of(2026, 10, 1), r.documents.get(0).issueDate);
    }

    @Test
    void matchRequestCopiesBothSkillLists() {
        MatchRequest r = mapper.toMatchRequest(List.of("Java"), posting());
        assertEquals(List.of("Java", "SQL"), r.requiredSkills);
        assertEquals(List.of("Git"), r.niceToHaveSkills);
    }
}
