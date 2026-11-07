package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.RankRequest;
import com.halcyon.uniplace.engine.EngineDtos.RankResult;
import com.halcyon.uniplace.engine.EngineRequestMapper;
import com.halcyon.uniplace.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** APP-09: candidate ranking for a posting, computed by the Engine. */
@Service
public class RankingService {

    public static final int MAX_CANDIDATES = 500;

    private final ApplicationRepository applications;
    private final EngineClient engine;
    private final EngineRequestMapper mapper;

    public RankingService(ApplicationRepository applications, EngineClient engine, EngineRequestMapper mapper) {
        this.applications = applications;
        this.engine = engine;
        this.mapper = mapper;
    }

    public record RankedCandidate(int rank, Application application, boolean aboveCut) {
    }

    @Transactional(readOnly = true)
    public List<RankedCandidate> rank(Posting posting) {
        List<Application> active = applications.findByPostingAndStateInOrderBySubmittedAtAsc(posting,
                ApplicationState.ACTIVE);
        if (active.isEmpty()) {
            return List.of();
        }
        if (active.size() > MAX_CANDIDATES) {
            throw new BusinessRuleException("This posting has more than " + MAX_CANDIDATES
                    + " active candidates; filter the list instead.");
        }
        RankRequest request = new RankRequest();
        request.seats = posting.getSeats();
        Map<Long, Application> byId = new HashMap<>();
        for (Application a : active) {
            request.candidates.add(mapper.toCandidate(a));
            byId.put(a.getId(), a);
        }
        RankResult result = engine.rank(request);
        Set<Long> cut = new HashSet<>(result.aboveCut());
        List<RankedCandidate> out = new ArrayList<>();
        int position = 1;
        for (Long id : result.ranked()) {
            Application a = byId.get(id);
            if (a != null) {
                out.add(new RankedCandidate(position++, a, cut.contains(id)));
            }
        }
        return out;
    }
}
