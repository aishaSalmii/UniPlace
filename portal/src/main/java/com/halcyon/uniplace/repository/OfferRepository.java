package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByApplication(Application application);

    List<Offer> findByStatusAndRespondByBefore(OfferStatus status, LocalDateTime time);

    List<Offer> findByStatusAndReminderSentFalse(OfferStatus status);

    long countByStatus(OfferStatus status);

    long countByStatusAndRespondByBefore(OfferStatus status, LocalDateTime time);

    @EntityGraph(attributePaths = {"application"})
    List<Offer> findAllByOrderByIdAsc();
}
