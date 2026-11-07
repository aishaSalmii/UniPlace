package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    public static final int MIN_PASSWORD_LENGTH = 10;
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,60}");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Usernames are stored in lower case; what the user types is compared the same way (USR-03). */
    public static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || !DIGIT.matcher(password).matches()) {
            throw new BusinessRuleException("The password must have at least " + MIN_PASSWORD_LENGTH
                    + " characters, including at least one digit.");
        }
    }

    @Transactional(readOnly = true)
    public void assertUsernameAvailable(String username) {
        String name = username == null ? "" : username.trim().toLowerCase();
        if (!USERNAME.matcher(name).matches()) {
            throw new BusinessRuleException("The username must have 3 to 60 lower-case letters, digits, dots, dashes or underscores.");
        }
        if (users.existsByUsername(name)) {
            throw new BusinessRuleException("The username " + name + " is already taken.");
        }
    }

    @Transactional
    public User create(String username, String password, Role role, String displayName, String email,
                       Student student, Company company) {
        String name = username == null ? "" : username.trim().toLowerCase();
        if (!USERNAME.matcher(name).matches()) {
            throw new BusinessRuleException("The username must have 3 to 60 lower-case letters, digits, dots, dashes or underscores.");
        }
        if (users.existsByUsername(name)) {
            throw new BusinessRuleException("The username " + name + " is already taken.");
        }
        checkPassword(password);
        User user = new User(name, encoder.encode(password), role, displayName, email);
        user.setStudent(student);
        user.setCompany(company);
        return users.save(user);
    }

    @Transactional
    public void changePassword(User user, String current, String next) {
        if (!encoder.matches(current == null ? "" : current, user.getPasswordHash())) {
            throw new BusinessRuleException("The current password is not correct.");
        }
        if (next == null || next.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("The new password must have at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        user.setPasswordHash(encoder.encode(next));
        users.save(user);
    }

    @Transactional
    public void unlock(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAllByOrderByUsernameAsc();
    }
}
