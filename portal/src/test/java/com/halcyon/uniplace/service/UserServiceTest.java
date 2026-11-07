package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.security.LoginAttemptService;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest extends IntegrationTest {

    @Autowired UserService service;
    @Autowired LoginAttemptService attempts;

    @ParameterizedTest
    @ValueSource(strings = {"short1", "longenoughbutnodigit", ""})
    void weakPasswordsAreRefused(String password) {
        assertThrows(BusinessRuleException.class, () -> UserService.checkPassword(password));
    }

    @Test
    void tenCharactersWithADigitIsAccepted() {
        assertDoesNotThrow(() -> UserService.checkPassword("abcdefghi1"));
    }

    @Test
    void usernamesAreUnique() {
        service.create("hana", "Password123", Role.RECRUITER, "Hana", null, null, null);
        assertThrows(BusinessRuleException.class,
                () -> service.create("HANA", "Password123", Role.RECRUITER, "Hana 2", null, null, null));
    }

    @Test
    void passwordsAreHashed() {
        User u = service.create("lina", "Password123", Role.COORDINATOR, "Lina", null, null, null);
        assertNotNull(u.getPasswordHash());
        assertEquals(false, u.getPasswordHash().contains("Password123"));
    }

    @Test
    void fiveFailuresLockTheAccount() {
        service.create("omar", "Password123", Role.COORDINATOR, "Omar", null, null, null);
        for (int i = 0; i < 4; i++) {
            attempts.failed("omar");
        }
        assertNull(users.findByUsername("omar").orElseThrow().getLockedUntil());
        attempts.failed("omar");
        assertNotNull(users.findByUsername("omar").orElseThrow().getLockedUntil());
    }

    @Test
    void usernamesAreStoredInLowerCaseAndValidated() {
        User u = service.create("  New.Coordinator ", "Password123", Role.COORDINATOR, "New", null, null, null);
        assertEquals("new.coordinator", u.getUsername());
        assertThrows(BusinessRuleException.class,
                () -> service.create("no spaces allowed", "Password123", Role.COORDINATOR, "X", null, null, null));
        assertThrows(BusinessRuleException.class, () -> service.assertUsernameAvailable("NEW.coordinator"));
    }

    @Test
    void unlockClearsTheLock() {
        User u = service.create("lockme", "Password123", Role.COORDINATOR, "Lock", null, null, null);
        for (int i = 0; i < 5; i++) {
            attempts.failed("lockme");
        }
        assertNotNull(users.findByUsername("lockme").orElseThrow().getLockedUntil());

        service.unlock(u.getId());

        assertNull(users.findByUsername("lockme").orElseThrow().getLockedUntil());
        assertEquals(0, users.findByUsername("lockme").orElseThrow().getFailedAttempts());
    }

    @Test
    void passwordChangeNeedsTheCurrentPassword() {
        User u = service.create("changer", "Password123", Role.COORDINATOR, "Changer", null, null, null);

        assertThrows(BusinessRuleException.class, () -> service.changePassword(u, "wrong", "NewPassword9"));
        assertDoesNotThrow(() -> service.changePassword(u, "Password123", "NewPassword9"));
    }

    @Test
    void usersAreListedByUsername() {
        service.create("zeta.user", "Password123", Role.COORDINATOR, "Z", null, null, null);
        service.create("alpha.user", "Password123", Role.COORDINATOR, "A", null, null, null);
        assertEquals("alpha.user", service.all().get(0).getUsername());
    }
}
