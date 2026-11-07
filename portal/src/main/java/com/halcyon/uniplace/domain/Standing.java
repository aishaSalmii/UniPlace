package com.halcyon.uniplace.domain;

import java.math.BigDecimal;

public enum Standing {
    GOOD, PROBATION;

    public static final BigDecimal PROBATION_BELOW = new BigDecimal("2.00");

    public static Standing fromGpa(BigDecimal gpa) {
        return gpa.compareTo(PROBATION_BELOW) < 0 ? PROBATION : GOOD;
    }
}
