package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Server-side ownership rules (USR-05). Every controller that loads a record by id goes through here. */
@Component
public class AccessGuard {

    public boolean canView(User user, Application application) {
        if (user.is(Role.COORDINATOR)) {
            return true;
        }
        if (user.is(Role.STUDENT)) {
            return user.getStudent() != null
                    && Objects.equals(user.getStudent().getId(), application.getStudent().getId());
        }
        return ownsCompany(user, application.getPosting().getCompany());
    }

    public void assertCanView(User user, Application application) {
        if (!canView(user, application)) {
            throw new ForbiddenException();
        }
    }

    public void assertStudentOwns(User user, Application application) {
        if (!user.is(Role.STUDENT) || user.getStudent() == null
                || !Objects.equals(user.getStudent().getId(), application.getStudent().getId())) {
            throw new ForbiddenException();
        }
    }

    public void assertRecruiterOwns(User user, Posting posting) {
        if (!user.is(Role.RECRUITER) || !ownsCompany(user, posting.getCompany())) {
            throw new ForbiddenException();
        }
    }

    public void assertRecruiterOwns(User user, Application application) {
        assertRecruiterOwns(user, application.getPosting());
    }

    private static boolean ownsCompany(User user, Company company) {
        return user.is(Role.RECRUITER) && user.getCompany() != null
                && Objects.equals(user.getCompany().getId(), company.getId());
    }
}
