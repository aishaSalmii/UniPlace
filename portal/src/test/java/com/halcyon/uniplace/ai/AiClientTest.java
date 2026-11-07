package com.halcyon.uniplace.ai;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.halcyon.uniplace.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class AiClientTest {

    static final String ANSWER = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"OK\"}}]}";

    final Map<String, String> values = new HashMap<>();
    AiClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        SettingsService settings = Mockito.mock(SettingsService.class);
        Mockito.when(settings.get(Mockito.anyString())).thenAnswer(i -> values.getOrDefault(i.getArgument(0), ""));
        values.put(SettingsService.AI_BASE_URL, "http://127.0.0.1:" + wm.getHttpPort() + "/v1");
        values.put(SettingsService.AI_MODEL, "test-model");
        client = new AiClient(RestClient.builder(), settings);
        stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(ANSWER)));
    }

    @Test
    void openAiCompatibleProvidersUseABearerToken() {
        values.put(SettingsService.AI_PROVIDER, "gemini");
        values.put(SettingsService.AI_API_KEY, "key-123");

        assertEquals("OK", client.complete(List.of(new ChatMessage("user", "hi"))));
        verify(postRequestedFor(urlEqualTo("/v1/chat/completions")).withHeader("Authorization", equalTo("Bearer key-123")));
    }

    @Test
    void azureUsesTheApiKeyHeader() {
        values.put(SettingsService.AI_PROVIDER, "azure");
        values.put(SettingsService.AI_API_KEY, "azure-key");

        client.complete(List.of(new ChatMessage("user", "hi")));

        verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withHeader("api-key", equalTo("azure-key")).withHeader("Authorization", absent()));
    }

    @Test
    void ollamaNeedsNoKey() {
        values.put(SettingsService.AI_PROVIDER, "ollama");

        assertTrue(client.configured());
        client.complete(List.of(new ChatMessage("user", "hi")));
        verify(postRequestedFor(urlEqualTo("/v1/chat/completions")).withHeader("Authorization", absent()));
    }

    @Test
    void withoutAKeyTheSimulatedAssistantIsUsed() {
        values.put(SettingsService.AI_PROVIDER, "openai");
        assertFalse(client.configured());
    }

    @Test
    void testConnectionReportsTheProviderErrorWithoutTheKey() {
        values.put(SettingsService.AI_PROVIDER, "openai");
        values.put(SettingsService.AI_API_KEY, "sk-wrong-1234");
        stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":{\"message\":\"Incorrect API key provided: sk-wrong-1234\"}}")));

        AiClient.ProbeResult r = client.probe();

        assertFalse(r.ok());
        assertTrue(r.message().contains("HTTP 401"), r.message());
        assertFalse(r.message().contains("sk-wrong-1234"), r.message());
    }

    @Test
    void testConnectionShowsTheAnswer() {
        values.put(SettingsService.AI_PROVIDER, "deepseek");
        values.put(SettingsService.AI_API_KEY, "key-123");

        AiClient.ProbeResult r = client.probe();

        assertTrue(r.ok(), r.message());
        assertTrue(r.message().contains("test-model"), r.message());
    }
}
