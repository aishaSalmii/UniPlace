package com.halcyon.uniplace.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandingTest {

    @ParameterizedTest(name = "GPA {0} gives {1}")
    @CsvSource({"0.00, PROBATION", "1.99, PROBATION", "2.00, GOOD", "2.01, GOOD", "4.00, GOOD"})
    void standingIsDerivedFromGpa(String gpa, Standing expected) {
        assertEquals(expected, Standing.fromGpa(new BigDecimal(gpa)));
    }
}
