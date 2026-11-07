package com.halcyon.uniplace.security;

import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.UserRepository;
import com.halcyon.uniplace.service.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ForbiddenException();
        }
        return users.findByUsername(auth.getName()).orElseThrow(ForbiddenException::new);
    }
}
