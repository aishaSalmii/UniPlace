package com.halcyon.uniplace.security;

import com.halcyon.uniplace.repository.UserRepository;
import com.halcyon.uniplace.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PortalUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final Clock clock;

    public PortalUserDetailsService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(UserService.normalise(username))
                .map(u -> new PortalUserDetails(u, LocalDateTime.now(clock)))
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
