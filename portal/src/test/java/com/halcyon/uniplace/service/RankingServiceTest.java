package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.RankResult;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankingServiceTest extends IntegrationTest {

    @Autowired RankingService service;
    @MockBean EngineClient engine;

    @Test
    void theEngineOrderIsShownWithPositionsAndCut() {
        Posting p = openPosting(company("Rank Co", CompanyStatus.APPROVED), "Intern", List.of("Java"), 1);
        Application first = application(student("3.00", List.of("Java")), p, ApplicationState.SUBMITTED, 60);
        Application second = application(student("3.50", List.of("Java")), p, ApplicationState.SHORTLISTED, 90);
        when(engine.rank(any())).thenReturn(new RankResult(List.of(second.getId(), first.getId()),
                List.of(second.getId())));

        List<RankingService.RankedCandidate> ranked = service.rank(p);

        assertEquals(2, ranked.size());
        assertEquals(second.getId(), ranked.get(0).application().getId());
        assertEquals(1, ranked.get(0).rank());
        assertTrue(ranked.get(0).aboveCut());
        assertEquals(2, ranked.get(1).rank());
        assertFalse(ranked.get(1).aboveCut());
    }

    @Test
    void closedApplicationsAreNotRanked() {
        Posting p = openPosting(company("Rank Co", CompanyStatus.APPROVED), "Intern", List.of("Java"), 1);
        application(student("3.00", List.of("Java")), p, ApplicationState.WITHDRAWN, 60);
        application(student("3.00", List.of("Java")), p, ApplicationState.REJECTED, 60);

        assertTrue(service.rank(p).isEmpty());
        verify(engine, never()).rank(any());
    }
}
