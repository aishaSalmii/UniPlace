package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.AuditEntry;
import com.halcyon.uniplace.repository.AuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuditService {

    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private String lastEntry;

    @Transactional
    public synchronized void record(String username, String action, String target, String oldValue, String newValue) {
        LocalDateTime at = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        // the same action by the same user within one second is a double submission of the form
        String entry = username + "|" + action + "|" + at;
        if (entry.equals(lastEntry)) {
            return;
        }
        lastEntry = entry;
        repository.save(new AuditEntry(username, at, action, target, oldValue, newValue));
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> page(int page) {
        return repository.findAllByOrderByAtDescIdDesc(PageRequest.of(Math.max(0, page), 20));
    }
}
