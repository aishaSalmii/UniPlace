package com.halcyon.uniplace.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.halcyon.uniplace.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client for any OpenAI-compatible chat completions endpoint configured in Settings. */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    static final double TEMPERATURE = 1.3;
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final RestClient.Builder builder;
    private final SettingsService settings;
    private final JdkClientHttpRequestFactory requestFactory;

    public AiClient(RestClient.Builder builder, SettingsService settings) {
        this.builder = builder;
        this.settings = settings;
        this.requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    }

    public boolean configured() {
        return !provider().needsKey() || !settings.get(SettingsService.AI_API_KEY).isBlank();
    }

    public AiProvider provider() {
        return AiProvider.of(settings.get(SettingsService.AI_PROVIDER));
    }

    public String model() {
        String model = settings.get(SettingsService.AI_MODEL);
        return model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    String baseUrl() {
        String url = settings.get(SettingsService.AI_BASE_URL).trim();
        if (url.isEmpty()) {
            url = provider().getBaseUrl().isEmpty() ? AiProvider.OPENAI.getBaseUrl() : provider().getBaseUrl();
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Sends the messages and returns the content of the first choice. */
    public String complete(List<ChatMessage> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model());
        body.put("messages", messages);
        body.put("temperature", TEMPERATURE);
        try {
            JsonNode response = call(body);
            JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
            if (content == null || !content.isTextual()) {
                throw new AiUnavailableException("no content in provider answer");
            }
            return content.asText();
        } catch (RestClientException ex) {
            log.warn("AI provider call failed: {}", ex.getClass().getSimpleName());
            throw new AiUnavailableException("provider call failed");
        }
    }

    private JsonNode call(Map<String, Object> body) {
        String key = settings.get(SettingsService.AI_API_KEY).trim();
        AiProvider provider = provider();
        return builder.clone()
                .baseUrl(baseUrl())
                .requestFactory(requestFactory)
                .build()
                .post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    if (provider.getAuth() == AiProvider.Auth.API_KEY_HEADER) {
                        h.set("api-key", key);
                    } else if (!key.isEmpty()) {
                        h.setBearerAuth(key);
                    }
                })
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    public record ProbeResult(boolean ok, String message) {
    }

    /** "Test connection" on the Settings page: one short request; reports the answer or the provider's error. */
    public ProbeResult probe() {
        if (!configured()) {
            return new ProbeResult(false, "Enter and save the API key first.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model());
        body.put("messages", List.of(new ChatMessage("user", "Reply with the single word OK.")));
        body.put("temperature", TEMPERATURE);
        String key = settings.get(SettingsService.AI_API_KEY).trim();
        try {
            JsonNode response = call(body);
            String text = response == null ? ""
                    : response.path("choices").path(0).path("message").path("content").asText("").trim();
            return new ProbeResult(true, "Connected to " + provider().getLabel() + ", model " + model()
                    + ". Its answer: \"" + shorten(text, 60, key) + "\"");
        } catch (RestClientResponseException ex) {
            return new ProbeResult(false, provider().getLabel() + " refused the request (HTTP "
                    + ex.getStatusCode().value() + "): " + shorten(errorMessage(ex.getResponseBodyAsString()), 200, key));
        } catch (RestClientException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return new ProbeResult(false, "No answer from " + baseUrl() + " (" + cause.getClass().getSimpleName()
                    + "). Check the base URL and the network connection.");
        }
    }

    private static String errorMessage(String body) {
        try {
            JsonNode n = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            JsonNode e = n.isArray() ? n.path(0).path("error") : n.path("error");
            String message = e.isTextual() ? e.asText() : e.path("message").asText("");
            return message.isBlank() ? body : message;
        } catch (Exception notJson) {
            return body == null ? "" : body;
        }
    }

    /** Never shows the key, even when a provider echoes it back. */
    private static String shorten(String text, int max, String key) {
        String s = text == null ? "" : text.replace("\n", " ");
        if (key != null && key.length() >= 4) {
            s = s.replace(key, "****");
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
