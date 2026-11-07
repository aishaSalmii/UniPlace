package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class MatchScoreServiceTest {

    @Test
    void repeatedRequestForTheSameSkillsUsesTheCache() {
        EngineClient engine = Mockito.mock(EngineClient.class);
        when(engine.match(any())).thenReturn(new MatchResult(64, List.of(), List.of(), List.of()));
        MatchScoreService service = new MatchScoreService(engine,
                new EngineRequestMapper(Mockito.mock(com.halcyon.uniplace.service.SettingsService.class), Clock.systemUTC()));
        Posting p = new Posting(new Company("C", "S", "e@c.qa"), "T", "D");
        ReflectionTestUtils.setField(p, "id", 7L);
        p.setRequiredSkills(List.of("Java"));

        assertEquals(64, service.score(List.of("Java", "SQL"), p));
        assertEquals(64, service.score(List.of("Java", "SQL"), p));

        Mockito.verify(engine, times(1)).match(any());
    }
}
