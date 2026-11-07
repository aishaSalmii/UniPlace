package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceTest extends IntegrationTest {

    @Autowired NotificationService service;

    @Test
    void notificationsArePaginatedTwentyPerPage() {
        User u = studentUser(student("3.00", List.of("Java")));
        for (int i = 0; i < 25; i++) {
            service.notify(u, "Subject " + i, "Body " + i);
        }

        assertEquals(20, service.forUser(u, 0).getContent().size());
        assertEquals(5, service.forUser(u, 1).getContent().size());
        assertEquals(2, service.forUser(u, 0).getTotalPages());
    }

    @Test
    void eachUserSeesOnlyTheirOwnNotificationsAndTheOutboxSeesAll() {
        User a = studentUser(student("3.00", List.of("Java")));
        User b = recruiter(company("Notify Co", CompanyStatus.APPROVED));
        service.notify(a, "For A", "Body");
        service.notify(b, "For B", "Body");

        assertEquals(List.of("For A"), service.forUser(a, 0).map(n -> n.getSubject()).getContent());
        assertEquals(2, service.all(0).getTotalElements());
    }

    @Test
    void negativePageNumbersShowTheFirstPage() {
        User u = studentUser(student("3.00", List.of("Java")));
        service.notify(u, "Only", "Body");
        assertEquals(1, service.forUser(u, -3).getContent().size());
    }
}
