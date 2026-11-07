package com.halcyon.uniplace.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationState {
    SUBMITTED, SHORTLISTED, INTERVIEW, OFFERED, ACCEPTED, REJECTED, WITHDRAWN;

    public static final Set<ApplicationState> ACTIVE = EnumSet.of(SUBMITTED, SHORTLISTED, INTERVIEW, OFFERED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
