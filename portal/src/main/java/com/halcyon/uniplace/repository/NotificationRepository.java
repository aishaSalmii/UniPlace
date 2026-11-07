package com.halcyon.uniplace.repository;

import com.halcyon.uniplace.domain.Notification;
import com.halcyon.uniplace.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientOrderByCreatedAtDescIdDesc(User recipient, Pageable pageable);

    @EntityGraph(attributePaths = "recipient")
    Page<Notification> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "recipient")
    List<Notification> findAllByOrderByCreatedAtDescIdDesc();

    List<Notification> findByRecipient(User recipient);
}
