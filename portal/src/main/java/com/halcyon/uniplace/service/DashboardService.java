package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** ADM-04. */
@Service
public class DashboardService {

    private final PostingRepository postings;
    private final ApplicationRepository applications;
    private final OfferRepository offers;
    private final Clock clock;

    public DashboardService(PostingRepository postings, ApplicationRepository applications, OfferRepository offers,
                            Clock clock) {
        this.postings = postings;
        this.applications = applications;
        this.offers = offers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> coordinatorFigures() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Open postings", postings.countByStatus(PostingStatus.OPEN));
        m.put("Active applications", applications.countByStateIn(ApplicationState.ACTIVE));
        m.put("Pending offers", offers.countByStatus(OfferStatus.PENDING));
        LocalDateTime now = LocalDateTime.now(clock);
        m.put("Overdue offers", offers.findAll().stream()
                .filter(o -> o.getStatus() == OfferStatus.PENDING && o.getRespondBy().isBefore(now))
                .count());
        return m;
    }
}
