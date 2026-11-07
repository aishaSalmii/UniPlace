package com.halcyon.uniplace.ai;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Ten-minute cache of letter reviews. */
@Component
public class FeedbackCache {

    static final Duration TTL = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public FeedbackCache(Clock clock) {
        this.clock = clock;
    }

    public Optional<AiFeedback> get(Long studentId, Long postingId, String letter) {
        Entry e = entries.get(key(studentId, postingId, letter));
        if (e == null || e.expiresAt.isBefore(Instant.now(clock))) {
            return Optional.empty();
        }
        return Optional.of(e.feedback);
    }

    public void put(Long studentId, Long postingId, String letter, AiFeedback feedback) {
        entries.put(key(studentId, postingId, letter), new Entry(feedback, Instant.now(clock).plus(TTL)));
    }

    public void clear() {
        entries.clear();
    }

    static String key(Long studentId, Long postingId, String letter) {
        return studentId + "|" + postingId;
    }

    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Entry(AiFeedback feedback, Instant expiresAt) {
    }
}
