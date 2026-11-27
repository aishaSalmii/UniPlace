package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.support.IntegrationTest;
import com.halcyon.uniplace.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired SettingsService settings;

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

    @Test
    void studentCannotViewAnotherStudentsApplication() throws Exception {
        Student owner = student("3.50", List.of("Java"));
        Student other = student("3.20", List.of("Python"));

        studentUser(owner);
        studentUser(other);

        Company company = company("Test Company", CompanyStatus.values()[0]);
        Posting posting = openPosting(company, "Software Intern", List.of("Java"), 2);

        Application app = application(owner, posting, ApplicationState.SUBMITTED, 85);

        mvc.perform(get("/applications/" + app.getId())
                .with(user("student" + other.getId()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotViewAnotherCompanyApplication() throws Exception {
        Company companyA = company("Gulf Bank", CompanyStatus.values()[0]);
        Company companyB = company("QTech", CompanyStatus.values()[0]);

        User recruiterB = recruiter(companyB);

        Student owner = student("3.50", List.of("Java"));
        Posting posting = openPosting(companyA, "Software Intern", List.of("Java"), 2);
        Application app = application(owner, posting, ApplicationState.SUBMITTED, 85);

        mvc.perform(get("/applications/" + app.getId())
                .with(user(recruiterB.getUsername()).roles("RECRUITER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/account/logout")
                .with(user("s").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }


@Test
void coordinatorSettingsWithoutCsrfTokenIsForbidden() throws Exception {
    users.save(new User("coord", encoder.encode("Password123"),
            Role.COORDINATOR, "Coord", null));

    org.junit.jupiter.api.Assertions.assertEquals(
            72, settings.offerResponseHours());

    mvc.perform(post("/coordinator/settings")
            .param(SettingsService.OFFER_RESPONSE_HOURS, "48")
            .with(user("coord").roles("COORDINATOR")))
            .andExpect(status().isForbidden());

    org.junit.jupiter.api.Assertions.assertEquals(
            72, settings.offerResponseHours());
}

}
