package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.ApplicationState;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.halcyon.uniplace.domain.ApplicationState.ACCEPTED;
import static com.halcyon.uniplace.domain.ApplicationState.INTERVIEW;
import static com.halcyon.uniplace.domain.ApplicationState.OFFERED;
import static com.halcyon.uniplace.domain.ApplicationState.REJECTED;
import static com.halcyon.uniplace.domain.ApplicationState.SHORTLISTED;
import static com.halcyon.uniplace.domain.ApplicationState.SUBMITTED;
import static com.halcyon.uniplace.domain.ApplicationState.WITHDRAWN;

/** APP-06 and INT-04: the only transitions an application may make. */
public final class ApplicationStateMachine {

    private static final Map<ApplicationState, Set<ApplicationState>> ALLOWED = new EnumMap<>(ApplicationState.class);

    static {
        ALLOWED.put(SUBMITTED, EnumSet.of(SHORTLISTED, REJECTED, WITHDRAWN));
        ALLOWED.put(SHORTLISTED, EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN));
        ALLOWED.put(INTERVIEW, EnumSet.of(OFFERED, SHORTLISTED, REJECTED, WITHDRAWN));
        ALLOWED.put(OFFERED, EnumSet.of(ACCEPTED, REJECTED, WITHDRAWN));
        ALLOWED.put(ACCEPTED, EnumSet.noneOf(ApplicationState.class));
        ALLOWED.put(REJECTED, EnumSet.noneOf(ApplicationState.class));
        ALLOWED.put(WITHDRAWN, EnumSet.noneOf(ApplicationState.class));
    }

    private ApplicationStateMachine() {
    }

    public static boolean canMove(ApplicationState from, ApplicationState to) {
        return ALLOWED.get(from).contains(to);
    }

    public static void assertCanMove(ApplicationState from, ApplicationState to) {
        if (!canMove(from, to)) {
            throw new BusinessRuleException("An application that is " + from.name().toLowerCase()
                    + " cannot become " + to.name().toLowerCase() + ".");
        }
    }
}
