package com.halcyon.uniplace.web;

import com.halcyon.uniplace.ai.AiClient;
import com.halcyon.uniplace.ai.AiProvider;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.AuditService;
import com.halcyon.uniplace.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** ADM-01 and ADM-03. */
@Controller
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    static final Set<String> SECRETS = Set.of(SettingsService.AI_API_KEY, SettingsService.SMTP_PASSWORD);
    static final Set<String> INTEGERS = Set.of(SettingsService.MAX_ACTIVE_GOOD, SettingsService.MAX_ACTIVE_PROBATION,
            SettingsService.SHORTLIST_THRESHOLD, SettingsService.OFFER_RESPONSE_HOURS,
            SettingsService.DEFAULT_MIN_CREDITS, SettingsService.SMTP_PORT);
    static final Set<String> DECIMALS = Set.of(SettingsService.DEFAULT_MIN_GPA, SettingsService.TRANSPORT_SUPPLEMENT,
            SettingsService.MONTHLY_CAP, SettingsService.PENALTY_RATE, SettingsService.PENALTY_CAP);

    public static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(SettingsService.MAX_ACTIVE_GOOD, "Active applications, good standing");
        LABELS.put(SettingsService.MAX_ACTIVE_PROBATION, "Active applications, probation");
        LABELS.put(SettingsService.SHORTLIST_THRESHOLD, "Shortlist threshold (match score)");
        LABELS.put(SettingsService.OFFER_RESPONSE_HOURS, "Offer response time (hours)");
        LABELS.put(SettingsService.DEFAULT_MIN_GPA, "Default minimum GPA");
        LABELS.put(SettingsService.DEFAULT_MIN_CREDITS, "Default minimum credits");
        LABELS.put(SettingsService.TRANSPORT_SUPPLEMENT, "Transport supplement per month (QAR)");
        LABELS.put(SettingsService.MONTHLY_CAP, "Monthly allowance cap (QAR)");
        LABELS.put(SettingsService.PENALTY_RATE, "Late-documents penalty per started week (0.05 = 5%)");
        LABELS.put(SettingsService.PENALTY_CAP, "Late-documents penalty cap (0.20 = 20%)");
        LABELS.put(SettingsService.ENGINE_BASE_URL, "Engine base URL");
        LABELS.put(SettingsService.AI_PROVIDER, "AI provider");
        LABELS.put(SettingsService.AI_BASE_URL, "AI provider base URL");
        LABELS.put(SettingsService.AI_API_KEY, "AI provider API key");
        LABELS.put(SettingsService.AI_MODEL, "AI model name");
        LABELS.put(SettingsService.SMTP_HOST, "SMTP host (empty = no e-mail)");
        LABELS.put(SettingsService.SMTP_PORT, "SMTP port");
        LABELS.put(SettingsService.SMTP_USERNAME, "SMTP username");
        LABELS.put(SettingsService.SMTP_PASSWORD, "SMTP password");
        LABELS.put(SettingsService.SMTP_FROM, "E-mail sender address");
    }

    private final SettingsService settings;
    private final AuditService audit;
    private final CurrentUser currentUser;
    private final AiClient ai;

    public SettingsController(SettingsService settings, AuditService audit, CurrentUser currentUser, AiClient ai) {
        this.settings = settings;
        this.audit = audit;
        this.currentUser = currentUser;
        this.ai = ai;
    }

    public record Row(String key, String label, String value, boolean secret, String hint) {
    }

    @GetMapping("/coordinator/settings")
    public String form(Model model) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : LABELS.entrySet()) {
            String value = settings.get(e.getKey());
            boolean secret = SECRETS.contains(e.getKey());
            rows.add(new Row(e.getKey(), e.getValue(), value, secret, secret ? mask(value) : ""));
        }
        model.addAttribute("rows", rows);
        model.addAttribute("providers", AiProvider.values());
        return "coordinator/settings";
    }

    @PostMapping("/coordinator/settings")
    public String save(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        String username = currentUser.get().getUsername();
        List<String> errors = new ArrayList<>();
        Map<String, String> changes = new LinkedHashMap<>();
        for (String key : LABELS.keySet()) {
            if (!params.containsKey(key)) {
                continue;
            }
            String value = params.get(key).trim();
            if (SECRETS.contains(key) && value.isEmpty() && !"on".equals(params.get(key + ".clear"))) {
                continue;
            }
            String problem = validate(key, value);
            if (problem != null) {
                errors.add(LABELS.get(key) + ": " + problem);
            } else if (settings.changed(key, value)) {
                changes.put(key, value);
            }
        }
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("error", String.join(" ", errors));
            return "redirect:/coordinator/settings";
        }
        for (Map.Entry<String, String> c : changes.entrySet()) {
            String old = settings.set(c.getKey(), c.getValue());
            boolean secret = SECRETS.contains(c.getKey());
            audit.record(username, "SETTING_CHANGED", c.getKey(), secret ? mask(old) : old,
                    secret ? mask(c.getValue()) : c.getValue());
            log.info("Setting {} changed from '{}' to '{}' by {}", c.getKey(), old, c.getValue(), username);
        }
        ra.addFlashAttribute("success", changes.isEmpty() ? "Nothing changed." : "Settings saved.");
        return "redirect:/coordinator/settings";
    }

    /** "Test connection": one short request to the saved AI provider settings. */
    @PostMapping("/coordinator/settings/test-ai")
    public String testAi(RedirectAttributes ra) {
        AiClient.ProbeResult result = ai.probe();
        ra.addFlashAttribute(result.ok() ? "success" : "error", result.message());
        return "redirect:/coordinator/settings";
    }

    static String validate(String key, String value) {
        try {
            if (INTEGERS.contains(key)) {
                int v = Integer.parseInt(value);
                if (v < 0) {
                    return "must not be negative.";
                }
            } else if (DECIMALS.contains(key)) {
                BigDecimal d = new BigDecimal(value);
                if (d.signum() < 0) {
                    return "must not be negative.";
                }
            } else if (SettingsService.AI_PROVIDER.equals(key)
                    && java.util.Arrays.stream(AiProvider.values()).noneMatch(p -> p.getId().equals(value))) {
                return "choose one of the listed providers.";
            } else if (SettingsService.AI_BASE_URL.equals(key) && !value.isEmpty()
                    && !(value.startsWith("http://") || value.startsWith("https://"))) {
                return "must start with http:// or https://.";
            } else if (SettingsService.ENGINE_BASE_URL.equals(key)
                    && !(value.startsWith("http://") || value.startsWith("https://"))) {
                return "must start with http:// or https://.";
            }
        } catch (NumberFormatException ex) {
            return "must be a number.";
        }
        return null;
    }

    static String mask(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "not set";
        }
        return "stored, ending in " + (secret.length() <= 4 ? "****" : secret.substring(secret.length() - 4));
    }
}
