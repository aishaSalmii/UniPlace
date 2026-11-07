package com.halcyon.uniplace.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackCacheTest {

    private static final AiFeedback RESULT = new AiFeedback(60, List.of("a"), List.of("b"), List.of("c"), "test");

    static final class MovableClock extends Clock {
        Instant now = Instant.parse("2026-11-20T08:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void sameLetterIsServedFromTheCache() {
        FeedbackCache cache = new FeedbackCache(Clock.systemUTC());
        cache.put(1L, 2L, "letter", RESULT);
        assertEquals(RESULT, cache.get(1L, 2L, "letter").orElseThrow());
    }

    @Test
    void entriesExpireAfterTenMinutes() {
        MovableClock clock = new MovableClock();
        FeedbackCache cache = new FeedbackCache(clock);
        cache.put(1L, 2L, "letter", RESULT);
        clock.now = clock.now.plusSeconds(601);
        assertTrue(cache.get(1L, 2L, "letter").isEmpty());
    }
}
