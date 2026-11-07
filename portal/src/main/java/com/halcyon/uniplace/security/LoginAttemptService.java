package com.halcyon.uniplace.security;

import com.halcyon.uniplace.repository.UserRepository;
import com.halcyon.uniplace.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    public static final int MAX_FAILURES = 5;
    public static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final Clock clock;

    public LoginAttemptService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void failed(String username) {
        users.findByUsername(username).ifPresent(u -> {
            LocalDateTime now = LocalDateTime.now(clock);
            if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now)) {
                return;
            }
            int failures = u.getFailedAttempts() + 1;
            if (failures >= MAX_FAILURES) {
                u.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                failures = 0;
            }
            u.setFailedAttempts(failures);
        });
    }

    @Transactional
    public void succeeded(String username) {
        users.findByUsername(UserService.normalise(username)).ifPresent(u -> {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
        });
    }
}
