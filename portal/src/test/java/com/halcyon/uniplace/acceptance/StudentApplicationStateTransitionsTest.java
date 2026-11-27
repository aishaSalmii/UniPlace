package com.halcyon.uniplace.acceptance;

import com.halcyon.uniplace.domain.*;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.SlotResult;
import com.halcyon.uniplace.repository.InterviewRepository;
import com.halcyon.uniplace.service.*;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** AI-assisted APP-06 / APP-08 / INT-04 supplemental M05 checks.
 * Expected transitions transcribed from APP-06, not read from production map.
 * Isolated H2; Engine mocked. HTTP, UI, authorization matrix, offer service
 * and real-provider paths remain outside these checks. Do not claim full M05 completion.
 */
public class StudentApplicationStateTransitionsTest extends IntegrationTest {
    @Autowired ApplicationService service;
    @Autowired InterviewService interviewService;
    @Autowired InterviewRepository interviews;
    @MockBean EngineClient engine;
    Company company;
    Posting posting;
    Student student;
    User owner;
    User recruiter;

    @BeforeEach void fixture() {
        company = company("QA State Transitions", CompanyStatus.APPROVED);
        posting = openPosting(company, "State Internship", List.of("Java"), 2);
        student = student("3.20", List.of("Java"));
        owner = studentUser(student);
        recruiter = recruiter(company);
    }

    static boolean requiredTransition(ApplicationState from, ApplicationState to) {
        return switch (from) {
            case SUBMITTED -> List.of(ApplicationState.SHORTLISTED, ApplicationState.REJECTED,
                    ApplicationState.WITHDRAWN).contains(to);
            case SHORTLISTED -> List.of(ApplicationState.INTERVIEW, ApplicationState.REJECTED,
                    ApplicationState.WITHDRAWN).contains(to);
            case INTERVIEW -> List.of(ApplicationState.OFFERED, ApplicationState.SHORTLISTED,
                    ApplicationState.REJECTED, ApplicationState.WITHDRAWN).contains(to);
            case OFFERED -> List.of(ApplicationState.ACCEPTED, ApplicationState.REJECTED,
                    ApplicationState.WITHDRAWN).contains(to);
            default -> false;
        };
    }
    static Stream<Arguments> transitions() {
        return Stream.of(ApplicationState.values()).flatMap(from ->
                Stream.of(ApplicationState.values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name="M05a APP-06 {0} -> {1}")
    @MethodSource("transitions")
    void M05a_stateMachineContract(ApplicationState from, ApplicationState to) {
        boolean expected = requiredTransition(from, to);
        assertEquals(expected, ApplicationStateMachine.canMove(from, to));
        if (expected) assertDoesNotThrow(() -> ApplicationStateMachine.assertCanMove(from, to));
        else assertThrows(BusinessRuleException.class,
                () -> ApplicationStateMachine.assertCanMove(from, to));
    }

    @ParameterizedTest(name="M05b student withdrawal from {0}")
    @EnumSource(ApplicationState.class)
    void M05b_withdrawalEnforcesStateContract(ApplicationState from) {
        Application a = application(student, posting, from, 70);
        if (requiredTransition(from, ApplicationState.WITHDRAWN)) {
            service.withdraw(owner, a.getId());
            assertEquals(ApplicationState.WITHDRAWN,
                    applications.findById(a.getId()).orElseThrow().getState());
        } else {
            // Check persisted state even if the expected exception is absent.
            assertAll(
                () -> assertThrows(BusinessRuleException.class, () -> service.withdraw(owner, a.getId())),
                () -> assertEquals(from, applications.findById(a.getId()).orElseThrow().getState())
            );
        }
    }

    @Test void M05c_cancelInterviewRestoresShortlistedAndPreventsRepeat() {
        Application a = application(student, posting, ApplicationState.SHORTLISTED, 70);
        when(engine.validateSlot(any())).thenReturn(new SlotResult(true, List.of()));
        Interview i = interviewService.schedule(recruiter, a.getId(), today().plusDays(7),
                LocalTime.of(10, 0), LocalTime.of(11, 0), "QA Room");
        assertEquals(ApplicationState.INTERVIEW, applications.findById(a.getId()).orElseThrow().getState());
        interviewService.cancel(recruiter, i.getId());
        assertTrue(interviews.findById(i.getId()).orElseThrow().isCancelled());
        assertEquals(ApplicationState.SHORTLISTED, applications.findById(a.getId()).orElseThrow().getState());
        assertThrows(BusinessRuleException.class, () -> interviewService.cancel(recruiter, i.getId()));
        assertEquals(ApplicationState.SHORTLISTED, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Test void M05d_cancelAfterOfferIsRefusedWithoutMutation() {
        Application a = application(student, posting, ApplicationState.OFFERED, 70);
        Interview i = interviews.save(new Interview(a, recruiter,
                today().plusDays(7).atTime(10, 0), today().plusDays(7).atTime(11, 0), "QA Room"));
        assertThrows(BusinessRuleException.class, () -> interviewService.cancel(recruiter, i.getId()));
        assertFalse(interviews.findById(i.getId()).orElseThrow().isCancelled());
        assertEquals(ApplicationState.OFFERED, applications.findById(a.getId()).orElseThrow().getState());
    }
}
