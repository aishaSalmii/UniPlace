package com.halcyon.uniplace.service;

import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsServiceTest extends IntegrationTest {

    @Autowired SettingsService settings;

    @Test
    void defaultsMatchTheSpecification() {
        assertEquals(72, settings.offerResponseHours());
        assertEquals("0.05", settings.get(SettingsService.PENALTY_RATE));
    }

    @Test
    void storedValueReplacesTheDefault() {
        String old = settings.set(SettingsService.OFFER_RESPONSE_HOURS, "48");
        assertEquals("72", old);
        assertEquals(48, settings.offerResponseHours());
    }

    @Test
    void unknownKeyIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> settings.set("no.such.key", "1"));
    }
}
