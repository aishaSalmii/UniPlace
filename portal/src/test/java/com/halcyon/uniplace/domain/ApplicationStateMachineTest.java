package com.halcyon.uniplace.domain;

import com.halcyon.uniplace.service.ApplicationStateMachine;
import com.halcyon.uniplace.service.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStateMachineTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "SUBMITTED, SHORTLISTED", "SUBMITTED, REJECTED", "SUBMITTED, WITHDRAWN",
            "SHORTLISTED, INTERVIEW", "SHORTLISTED, REJECTED", "SHORTLISTED, WITHDRAWN",
            "INTERVIEW, OFFERED", "INTERVIEW, SHORTLISTED", "INTERVIEW, REJECTED", "INTERVIEW, WITHDRAWN",
            "OFFERED, ACCEPTED", "OFFERED, REJECTED", "OFFERED, WITHDRAWN"})
    void allowedTransitions(ApplicationState from, ApplicationState to) {
        assertTrue(ApplicationStateMachine.canMove(from, to));
        assertDoesNotThrow(() -> ApplicationStateMachine.assertCanMove(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1} is refused")
    @CsvSource({
            "SUBMITTED, INTERVIEW", "SUBMITTED, OFFERED", "SUBMITTED, ACCEPTED",
            "SHORTLISTED, OFFERED", "SHORTLISTED, ACCEPTED", "INTERVIEW, ACCEPTED",
            "ACCEPTED, WITHDRAWN", "ACCEPTED, REJECTED", "REJECTED, SUBMITTED", "WITHDRAWN, SUBMITTED"})
    void refusedTransitions(ApplicationState from, ApplicationState to) {
        assertFalse(ApplicationStateMachine.canMove(from, to));
        assertThrows(BusinessRuleException.class, () -> ApplicationStateMachine.assertCanMove(from, to));
    }

    @Test
    void refusalMessageNamesBothStates() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ApplicationStateMachine.assertCanMove(ApplicationState.REJECTED, ApplicationState.SHORTLISTED));
        assertEquals("An application that is rejected cannot become shortlisted.", ex.getMessage());
    }

    @Test
    void activeStates() {
        assertTrue(ApplicationState.OFFERED.isActive());
        assertFalse(ApplicationState.ACCEPTED.isActive());
        assertFalse(ApplicationState.WITHDRAWN.isActive());
    }
}
