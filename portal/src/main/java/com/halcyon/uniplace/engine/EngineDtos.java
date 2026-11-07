package com.halcyon.uniplace.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Request and response bodies of the Engine API (see engine/openapi.yaml). */
public final class EngineDtos {

    private EngineDtos() {
    }

    public static class DocumentInfo {
        public String type;
        public long sizeBytes;
        public String mimeType;
        public LocalDate issueDate;
    }

    public static class EligibilityRequest {
        public BigDecimal gpa;
        public int creditsCompleted;
        public String standing;
        public int priorInternships;
        public BigDecimal minGpa;
        public int minCredits;
        public LocalDate asOf;
        public List<DocumentInfo> documents = new ArrayList<>();
    }

    public record EligibilityResult(boolean eligible, List<String> reasons) {
        public static EligibilityResult ok() {
            return new EligibilityResult(true, List.of());
        }
    }

    public static class MatchRequest {
        public List<String> studentSkills;
        public List<String> requiredSkills;
        public List<String> niceToHaveSkills;
    }

    public record MatchResult(int score, List<String> matchedRequired, List<String> missingRequired,
                              List<String> matchedNiceToHave) {
    }

    public static class RankCandidate {
        public long applicationId;
        public int score;
        public BigDecimal gpa;
        public OffsetDateTime submittedAt;
    }

    public static class RankRequest {
        public int seats;
        public List<RankCandidate> candidates = new ArrayList<>();
    }

    public record RankResult(List<Long> ranked, List<Long> aboveCut) {
    }

    public static class Slot {
        public Object start;
        public Object end;

        public Slot() {
        }

        public Slot(Object start, Object end) {
            this.start = start;
            this.end = end;
        }
    }

    public static class SlotRequest {
        public Slot candidate;
        public List<Slot> recruiterSlots = new ArrayList<>();
        public List<Slot> studentSlots = new ArrayList<>();
    }

    public record SlotResult(boolean valid, List<String> conflicts) {
    }

    public static class AllowanceRequest {
        public BigDecimal monthlyAllowance;
        public LocalDate startDate;
        public LocalDate endDate;
        public int weeklyHours;
        public int lateDays;
        public BigDecimal transportSupplement;
        public BigDecimal monthlyCap;
        public BigDecimal penaltyRate;
        public BigDecimal penaltyCap;
    }

    public record AllowanceLine(String label, BigDecimal amount) {
    }

    public record AllowanceResult(BigDecimal total, List<AllowanceLine> lines) {
    }
}
