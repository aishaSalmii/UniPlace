package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void correctPasswordLogsIn() throws Exception {
        users.save(new User("coord", encoder.encode("Password123"), Role.COORDINATOR, "Coord", null));
        mvc.perform(formLogin("/login").user("coord").password("Password123"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        users.save(new User("coord", encoder.encode("Password123"), Role.COORDINATOR, "Coord", null));
        mvc.perform(formLogin("/login").user("coord").password("wrong"))
                .andExpect(redirectedUrl("/login?error=bad"));
    }

    @Test
    void studentCannotOpenCoordinatorSettings() throws Exception {
        mvc.perform(get("/coordinator/settings").with(user("s").roles("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotOpenRecruiterPages() throws Exception {
        mvc.perform(get("/recruiter/postings").with(user("s").roles("STUDENT"))).andExpect(status().isForbidden());
    }
}
