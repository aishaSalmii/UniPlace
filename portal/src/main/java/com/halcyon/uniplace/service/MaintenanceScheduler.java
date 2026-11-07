package com.halcyon.uniplace.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodic housekeeping: closes postings past their deadline, expires offers and sends reminders. */
@Component
@ConditionalOnProperty(name = "uniplace.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceScheduler {

    private final PostingService postings;
    private final OfferService offers;

    public MaintenanceScheduler(PostingService postings, OfferService offers) {
        this.postings = postings;
        this.offers = offers;
    }

    @Scheduled(initialDelay = 20_000, fixedDelay = 600_000)
    public void run() {
        postings.closeExpired();
        offers.expireOverdue();
        offers.sendReminders();
    }
}
