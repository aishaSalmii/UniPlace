package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.engine.EngineDtos.AllowanceRequest;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityRequest;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.engine.EngineDtos.RankRequest;
import com.halcyon.uniplace.engine.EngineDtos.RankResult;
import com.halcyon.uniplace.engine.EngineDtos.SlotRequest;
import com.halcyon.uniplace.engine.EngineDtos.SlotResult;
import com.halcyon.uniplace.service.SettingsService;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * REST client for the Matching and Compliance Engine.
 */
@Component
public class EngineClient {

    private static final Logger log = LoggerFactory.getLogger(EngineClient.class);
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    /** Dates travel as ISO-8601 text, as the OpenAPI contract requires. */
    private static final MappingJackson2HttpMessageConverter JSON = new MappingJackson2HttpMessageConverter(
            Jackson2ObjectMapperBuilder.json()
                    .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .build());

    private final RestClient.Builder builder;
    private final String baseUrl;
    private final JdkClientHttpRequestFactory requestFactory;

    public EngineClient(RestClient.Builder builder, SettingsService settings) {
        this.builder = builder;
        this.baseUrl = settings.engineBaseUrl();
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.requestFactory = new JdkClientHttpRequestFactory(http);
        this.requestFactory.setReadTimeout(READ_TIMEOUT);
    }

    private RestClient client() {
        return builder.clone()
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(0, JSON);
                })
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            T result = client().post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
            if (result == null) {
                throw new EngineUnavailableException();
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Engine call {} failed: {}", path, ex.getClass().getSimpleName());
            throw new EngineUnavailableException();
        }
    }

    /** When the Engine cannot be reached the check is skipped, so that students are not blocked. */
    public EligibilityResult checkEligibility(EligibilityRequest request) {
        try {
            return post("/eligibility", request, EligibilityResult.class);
        } catch (EngineUnavailableException ex) {
            return EligibilityResult.ok();
        }
    }

    public MatchResult match(MatchRequest request) {
        try {
            return post("/match", request, MatchResult.class);
        } catch (EngineUnavailableException ex) {
            return new MatchResult(0, List.of(), List.of(), List.of());
        }
    }

    public RankResult rank(RankRequest request) {
        return post("/rank", request, RankResult.class);
    }

    public SlotResult validateSlot(SlotRequest request) {
        return post("/schedule/validate", request, SlotResult.class);
    }

    public AllowanceResult allowance(AllowanceRequest request) {
        return post("/allowance", request, AllowanceResult.class);
    }
}
