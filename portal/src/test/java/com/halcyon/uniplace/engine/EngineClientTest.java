package com.halcyon.uniplace.engine;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityRequest;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class EngineClientTest {

    EngineClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        SettingsService settings = Mockito.mock(SettingsService.class);
        Mockito.when(settings.engineBaseUrl()).thenReturn("http://127.0.0.1:" + wm.getHttpPort() + "/api/v1");
        client = new EngineClient(RestClient.builder(), settings);
    }

    private static EligibilityRequest request() {
        EligibilityRequest r = new EligibilityRequest();
        r.gpa = new BigDecimal("3.00");
        r.creditsCompleted = 60;
        r.standing = "GOOD";
        r.minGpa = new BigDecimal("2.00");
        r.minCredits = 45;
        r.asOf = LocalDate.of(2026, 11, 20);
        return r;
    }

    @Test
    void eligibilityAnswerIsParsed() {
        stubFor(post(urlEqualTo("/api/v1/eligibility"))
                .willReturn(okJson("{\"eligible\":false,\"reasons\":[\"CV is missing\",\"Transcript is missing\"]}")));

        EligibilityResult r = client.checkEligibility(request());

        assertEquals(false, r.eligible());
        assertEquals(List.of("CV is missing", "Transcript is missing"), r.reasons());
    }

    @Test
    void datesAreSentAsIsoText() {
        stubFor(post(urlEqualTo("/api/v1/eligibility")).willReturn(okJson("{\"eligible\":true,\"reasons\":[]}")));

        client.checkEligibility(request());

        verify(postRequestedFor(urlEqualTo("/api/v1/eligibility"))
                .withRequestBody(equalToJson("{\"asOf\":\"2026-11-20\"}", true, true)));
    }

    @Test
    void matchAnswerIsParsed() {
        stubFor(post(urlEqualTo("/api/v1/match")).willReturn(okJson(
                "{\"score\":85,\"matchedRequired\":[\"Java\"],\"missingRequired\":[],\"matchedNiceToHave\":[\"Git\"]}")));
        MatchRequest m = new MatchRequest();
        m.studentSkills = List.of("Java", "Git");
        m.requiredSkills = List.of("Java");
        m.niceToHaveSkills = List.of("Git", "Docker");

        MatchResult r = client.match(m);

        assertEquals(85, r.score());
        assertEquals(List.of("Git"), r.matchedNiceToHave());
    }

    @Test
    void validationErrorFromTheEngineIsReported() {
        stubFor(post(urlEqualTo("/api/v1/rank")).willReturn(aResponse().withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"validation\",\"field\":\"seats\",\"message\":\"is required\"}")));

        EngineUnavailableException ex = assertThrows(EngineUnavailableException.class,
                () -> client.rank(new EngineDtos.RankRequest()));
        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void engineUnavailableDegradesGracefully() {
        stubFor(post(urlEqualTo("/api/v1/eligibility")).willReturn(aResponse().withStatus(503)));

        EligibilityResult r = client.checkEligibility(request());

        assertTrue(r.eligible());
        assertTrue(r.reasons().isEmpty());
    }
}
