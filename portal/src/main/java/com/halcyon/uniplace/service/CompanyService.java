package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.CompanyRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CompanyService {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final CompanyRepository companies;
    private final UserRepository users;
    private final PostingRepository postings;
    private final ApplicationRepository applications;
    private final NotificationService notifications;

    public CompanyService(CompanyRepository companies, UserRepository users, PostingRepository postings,
                          ApplicationRepository applications, NotificationService notifications) {
        this.companies = companies;
        this.users = users;
        this.postings = postings;
        this.applications = applications;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Company get(Long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company"));
    }

    @Transactional(readOnly = true)
    public List<Company> all() {
        return companies.findAllByOrderByNameAsc();
    }

    @Transactional
    public Company register(User recruiter, String name, String sector, String contactEmail) {
        if (!recruiter.is(Role.RECRUITER)) {
            throw new ForbiddenException();
        }
        if (recruiter.getCompany() != null) {
            throw new BusinessRuleException("You are already linked to a company.");
        }
        validate(name, sector, contactEmail, null);
        Company c = companies.save(new Company(name.trim(), sector.trim(), contactEmail.trim()));
        recruiter.setCompany(c);
        users.save(recruiter);
        return c;
    }

    @Transactional
    public Company update(User recruiter, String name, String sector, String contactEmail) {
        Company c = recruiter.getCompany();
        if (c == null) {
            throw new ForbiddenException();
        }
        validate(name, sector, contactEmail, c.getId());
        c.setName(name.trim());
        c.setSector(sector.trim());
        c.setContactEmail(contactEmail.trim());
        return companies.save(c);
    }

    @Transactional
    public void approve(Long id) {
        Company c = get(id);
        c.setStatus(CompanyStatus.APPROVED);
        notifyRecruiters(c, "Company approved", c.getName() + " is approved on UniPlace. Its postings can now be published.");
    }

    @Transactional
    public void suspend(Long id) {
        Company c = get(id);
        if (c.getStatus() != CompanyStatus.APPROVED) {
            throw new BusinessRuleException("Only an approved company can be suspended.");
        }
        c.setStatus(CompanyStatus.SUSPENDED);
        notifyRecruiters(c, "Company suspended", c.getName() + " is suspended on UniPlace. Its postings are hidden "
                + "and its applications are frozen until it is approved again.");
        Set<Long> notified = new HashSet<>();
        for (Posting p : postings.findByCompany(c)) {
            for (Application a : applications.findByPosting(p)) {
                if (a.getState().isActive() && notified.add(a.getStudent().getId())) {
                    users.findByStudent(a.getStudent()).ifPresent(u -> notifications.notify(u,
                            "Applications to " + c.getName() + " are on hold",
                            "Your application to " + p.getTitle() + " at " + c.getName()
                                    + " is on hold while the company is suspended."));
                }
            }
        }
    }

    private void notifyRecruiters(Company c, String subject, String body) {
        for (User u : users.findByCompanyAndRole(c, Role.RECRUITER)) {
            notifications.notify(u, subject, body);
        }
    }

    private void validate(String name, String sector, String email, Long excludeId) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new BusinessRuleException("The company name is required (at most 120 characters).");
        }
        if (sector == null || sector.isBlank() || sector.length() > 80) {
            throw new BusinessRuleException("The sector is required (at most 80 characters).");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessRuleException("Enter a valid contact e-mail.");
        }
        boolean taken = excludeId == null
                ? companies.nameTaken(name.trim(), null)
                : companies.existsByNameAndIdNot(name.trim(), excludeId);
        if (taken) {
            throw new BusinessRuleException("A company named " + name.trim() + " already exists.");
        }
    }
}
