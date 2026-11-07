package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Notification;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.CompanyRepository;
import com.halcyon.uniplace.repository.NotificationRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.StudentRepository;
import com.halcyon.uniplace.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ADM-05: synthetic volume for performance testing. Generated students, postings and applications are marked
 * with the prefix "Generated" and do not have login accounts.
 */
@Service
public class TestDataService {

    public static final int MAX_PER_RUN = 10_000;
    private static final String[] SKILLS = {"Java", "SQL", "Python", "Excel", "Communication", "Spring", "Git",
            "JavaScript", "Safety", "Writing", "Statistics", "Power BI"};

    private final StudentRepository students;
    private final PostingRepository postings;
    private final CompanyRepository companies;
    private final ApplicationRepository applications;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EntityManager em;
    private final Clock clock;

    public TestDataService(StudentRepository students, PostingRepository postings, CompanyRepository companies,
                           ApplicationRepository applications, NotificationRepository notifications,
                           UserRepository users, EntityManager em, Clock clock) {
        this.students = students;
        this.postings = postings;
        this.companies = companies;
        this.applications = applications;
        this.notifications = notifications;
        this.users = users;
        this.em = em;
        this.clock = clock;
    }

    public record Result(int students, int postings, int applications, int notifications) {
    }

    @Transactional
    public Result generate(int count) {
        if (count < 1 || count > MAX_PER_RUN) {
            throw new BusinessRuleException("Generate between 1 and " + MAX_PER_RUN + " applications per run.");
        }
        Random rnd = new Random(count * 31L + applications.count());
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Company> approved = companies.findByStatusOrderByNameAsc(CompanyStatus.APPROVED);
        if (approved.isEmpty()) {
            throw new BusinessRuleException("At least one approved company is needed.");
        }
        long base = students.count() + 1;

        List<Student> newStudents = new ArrayList<>();
        int studentCount = Math.max(1, count / 10);
        for (int i = 0; i < studentCount; i++) {
            String number = String.valueOf(70_000_000L + base * 1000 + i);
            Student s = new Student(number.substring(number.length() - 8), "Generated Student " + (base + i),
                    "generated" + (base + i) + "@students.uniplace.edu.qa", "BSc Information Systems",
                    BigDecimal.valueOf(200 + rnd.nextInt(200)).movePointLeft(2), 40 + rnd.nextInt(90), 0);
            s.setSkills(new ArrayList<>(List.of(SKILLS[rnd.nextInt(SKILLS.length)], SKILLS[rnd.nextInt(SKILLS.length)])));
            newStudents.add(students.save(s));
        }

        List<Posting> newPostings = new ArrayList<>();
        int postingCount = Math.max(1, count / 50);
        for (int i = 0; i < postingCount; i++) {
            Company c = approved.get(i % approved.size());
            Posting p = new Posting(c, "Generated Internship " + (postings.count() + 1),
                    "Generated posting for volume testing.");
            p.setRequiredSkills(new ArrayList<>(List.of(SKILLS[rnd.nextInt(SKILLS.length)])));
            p.setNiceToHaveSkills(new ArrayList<>(List.of(SKILLS[rnd.nextInt(SKILLS.length)])));
            p.setMinGpa(new BigDecimal("2.00"));
            p.setMinCredits(30);
            p.setSeats(1 + rnd.nextInt(5));
            p.setDeadline(today.plusDays(5 + rnd.nextInt(30)));
            p.setStartDate(p.getDeadline().plusDays(21));
            p.setEndDate(p.getStartDate().plusMonths(3));
            p.setWeeklyHours(20 + rnd.nextInt(21));
            p.setMonthlyAllowance(BigDecimal.valueOf(2000 + rnd.nextInt(3000)));
            p.setStatus(PostingStatus.OPEN);
            p.setCreatedAt(now);
            newPostings.add(postings.save(p));
        }

        ApplicationState[] states = {ApplicationState.SUBMITTED, ApplicationState.SUBMITTED, ApplicationState.SHORTLISTED,
                ApplicationState.REJECTED, ApplicationState.WITHDRAWN};
        User recipient = users.findByRole(Role.COORDINATOR).stream().findFirst().orElse(null);
        int notified = 0;
        for (int i = 0; i < count; i++) {
            Student s = newStudents.get(i % newStudents.size());
            Posting p = newPostings.get((i / newStudents.size() + i) % newPostings.size());
            Application a = new Application(s, p, "Generated application letter for volume testing. ".repeat(3),
                    30 + rnd.nextInt(71), now.minusMinutes(rnd.nextInt(60 * 24 * 30)));
            a.setState(states[rnd.nextInt(states.length)]);
            applications.save(a);
            if (recipient != null) {
                notifications.save(new Notification(recipient, "Generated notification " + (i + 1),
                        "Generated for volume testing.", now));
                notified++;
            }
            if (i % 500 == 499) {
                em.flush();
                em.clear();
            }
        }
        return new Result(newStudents.size(), newPostings.size(), count, notified);
    }
}
