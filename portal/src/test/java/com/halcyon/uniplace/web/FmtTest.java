package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.ApplicationState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmtTest {

    private final Fmt fmt = new Fmt();

    @Test
    void datesUseDayMonthYear() {
        assertEquals("05/11/2026", fmt.date(LocalDate.of(2026, 11, 5)));
        assertEquals("05/11/2026 09:30", fmt.dateTime(LocalDateTime.of(2026, 11, 5, 9, 30)));
        assertEquals("", fmt.date(null));
    }

    @Test
    void moneyHasCurrencyTwoDecimalsAndSeparators() {
        assertEquals("QAR 12,500.00", fmt.money(new BigDecimal("12500")));
        assertEquals("QAR 0.50", fmt.money(new BigDecimal("0.5")));
    }

    @Test
    void enumLabels() {
        assertEquals("Shortlisted", fmt.label(ApplicationState.SHORTLISTED));
    }
}
