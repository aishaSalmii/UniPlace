package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.SettingEntry;
import com.halcyon.uniplace.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Coordinator-editable settings. */
@Service
public class SettingsService {

    public static final String MAX_ACTIVE_GOOD = "limits.active.good";
    public static final String MAX_ACTIVE_PROBATION = "limits.active.probation";
    public static final String SHORTLIST_THRESHOLD = "shortlist.threshold";
    public static final String OFFER_RESPONSE_HOURS = "offer.response.hours";
    public static final String DEFAULT_MIN_GPA = "posting.default.minGpa";
    public static final String DEFAULT_MIN_CREDITS = "posting.default.minCredits";
    public static final String TRANSPORT_SUPPLEMENT = "allowance.transport";
    public static final String MONTHLY_CAP = "allowance.monthlyCap";
    public static final String PENALTY_RATE = "allowance.penaltyRate";
    public static final String PENALTY_CAP = "allowance.penaltyCap";
    public static final String ENGINE_BASE_URL = "engine.baseUrl";
    public static final String AI_PROVIDER = "ai.provider";
    public static final String AI_BASE_URL = "ai.baseUrl";
    public static final String AI_API_KEY = "ai.apiKey";
    public static final String AI_MODEL = "ai.model";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";
    public static final String SMTP_FROM = "smtp.from";

    private final SettingRepository repository;
    private final Map<String, String> defaults = new LinkedHashMap<>();

    public SettingsService(SettingRepository repository,
                           @Value("${uniplace.engine.default-url}") String engineDefaultUrl) {
        this.repository = repository;
        defaults.put(MAX_ACTIVE_GOOD, "5");
        defaults.put(MAX_ACTIVE_PROBATION, "2");
        defaults.put(SHORTLIST_THRESHOLD, "60");
        defaults.put(OFFER_RESPONSE_HOURS, "72");
        defaults.put(DEFAULT_MIN_GPA, "2.00");
        defaults.put(DEFAULT_MIN_CREDITS, "45");
        defaults.put(TRANSPORT_SUPPLEMENT, "300");
        defaults.put(MONTHLY_CAP, "5000");
        defaults.put(PENALTY_RATE, "0.05");
        defaults.put(PENALTY_CAP, "0.20");
        defaults.put(ENGINE_BASE_URL, engineDefaultUrl);
        defaults.put(AI_PROVIDER, "openai");
        defaults.put(AI_BASE_URL, "");
        defaults.put(AI_API_KEY, "");
        defaults.put(AI_MODEL, "");
        defaults.put(SMTP_HOST, "");
        defaults.put(SMTP_PORT, "587");
        defaults.put(SMTP_USERNAME, "");
        defaults.put(SMTP_PASSWORD, "");
        defaults.put(SMTP_FROM, "uniplace@localhost");
    }

    public Map<String, String> keysWithDefaults() {
        return Map.copyOf(defaults);
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findById(key).map(SettingEntry::getValue).orElse(defaults.get(key));
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key).trim());
    }

    public BigDecimal getDecimal(String key) {
        return new BigDecimal(get(key).trim());
    }

    /** Stores a value and returns the previous one. */
    @Transactional
    public String set(String key, String value) {
        if (!defaults.containsKey(key)) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        String old = get(key);
        SettingEntry entry = repository.findById(key).orElseGet(() -> new SettingEntry(key, value));
        entry.setValue(value);
        repository.save(entry);
        return old;
    }

    public boolean changed(String key, String value) {
        return !Objects.equals(get(key), value);
    }

    private static volatile Integer maxActiveGood;
    private static volatile Integer maxActiveProbation;
    private static volatile Integer shortlistThreshold;

    public int maxActiveGood() {
        if (maxActiveGood == null) {
            maxActiveGood = getInt(MAX_ACTIVE_GOOD);
        }
        return maxActiveGood;
    }

    public int maxActiveProbation() {
        if (maxActiveProbation == null) {
            maxActiveProbation = getInt(MAX_ACTIVE_PROBATION);
        }
        return maxActiveProbation;
    }

    public int shortlistThreshold() {
        if (shortlistThreshold == null) {
            shortlistThreshold = getInt(SHORTLIST_THRESHOLD);
        }
        return shortlistThreshold;
    }

    public int offerResponseHours() {
        return getInt(OFFER_RESPONSE_HOURS);
    }

    public String engineBaseUrl() {
        return get(ENGINE_BASE_URL);
    }
}
