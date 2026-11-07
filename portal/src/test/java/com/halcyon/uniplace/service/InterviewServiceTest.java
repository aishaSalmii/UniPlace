package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.SlotResult;
import com.halcyon.uniplace.repository.InterviewRepository;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceTest extends IntegrationTest {

    @Autowired InterviewService service;
    @Autowired InterviewRepository interviews;
    @MockBean EngineClient engine;

    User recruiter;
    User studentUser;
    Application shortlisted;

    @BeforeEach
    void setUp() {
        Company c = company("Lusail Health", CompanyStatus.APPROVED);
        Posting p = openPosting(c, "Informatics Intern", List.of("SQL"), 1);
        Student s = student("3.00", List.of("SQL"));
        studentUser = studentUser(s);
        recruiter = recruiter(c);
        shortlisted = application(s, p, ApplicationState.SHORTLISTED, 70);
    }

    @Test
    void validSlotCreatesTheInterview() {
        when(engine.validateSlot(any())).thenReturn(new SlotResult(true, List.of()));

        Interview i = service.schedule(recruiter, shortlisted.getId(), LocalDate.of(2026, 11, 23),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Room 4");

        assertEquals("Room 4", i.getLocation());
        assertEquals(ApplicationState.INTERVIEW, applications.findById(shortlisted.getId()).orElseThrow().getState());
    }

    @Test
    void conflictsFromTheEngineAreShown() {
        when(engine.validateSlot(any())).thenReturn(new SlotResult(false, List.of("Interviews take place Sunday to Thursday")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.schedule(recruiter,
                shortlisted.getId(), LocalDate.of(2026, 11, 27), LocalTime.of(10, 0), LocalTime.of(11, 0), "Room 4"));
        assertTrue(ex.getMessage().contains("Sunday to Thursday"));
        assertEquals(0, interviews.count());
    }

    @Test
    void endBeforeStartIsRefusedWithoutCallingTheEngine() {
        assertThrows(BusinessRuleException.class, () -> service.schedule(recruiter, shortlisted.getId(),
                LocalDate.of(2026, 11, 23), LocalTime.of(11, 0), LocalTime.of(10, 0), "Room 4"));
        verify(engine, never()).validateSlot(any());
    }

    @Test
    void cancellingReturnsTheApplicationToShortlisted() {
        when(engine.validateSlot(any())).thenReturn(new SlotResult(true, List.of()));
        Interview i = service.schedule(recruiter, shortlisted.getId(), LocalDate.of(2026, 11, 23),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Room 4");

        service.cancel(recruiter, i.getId());

        assertEquals(ApplicationState.SHORTLISTED, applications.findById(shortlisted.getId()).orElseThrow().getState());
    }

    @Test
    void onlyOneRescheduleRequest() {
        when(engine.validateSlot(any())).thenReturn(new SlotResult(true, List.of()));
        service.schedule(recruiter, shortlisted.getId(), LocalDate.of(2026, 11, 23), LocalTime.of(10, 0),
                LocalTime.of(11, 0), "Room 4");

        service.requestReschedule(studentUser, shortlisted.getId(), "Exam that day");

        assertThrows(BusinessRuleException.class,
                () -> service.requestReschedule(studentUser, shortlisted.getId(), "Again"));
    }
}
