package com.halcyon.uniplace.support;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.CompanyRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.StudentDocumentRepository;
import com.halcyon.uniplace.repository.StudentRepository;
import com.halcyon.uniplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Base class for tests that need the Spring context and a clean in-memory database. */
@SpringBootTest
public abstract class IntegrationTest {

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected UserRepository users;
    @Autowired protected StudentRepository students;
    @Autowired protected StudentDocumentRepository documents;
    @Autowired protected CompanyRepository companies;
    @Autowired protected PostingRepository postings;
    @Autowired protected ApplicationRepository applications;
    @Autowired protected Clock clock;
    @Autowired private com.halcyon.uniplace.engine.MatchScoreService matchScores;
    @Autowired private com.halcyon.uniplace.ai.FeedbackCache feedbackCache;
    @Autowired private com.halcyon.uniplace.service.DocumentService documentService;

    private int counter;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tables = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", String.class);
        for (String t : tables) {
            jdbc.execute("TRUNCATE TABLE \"" + t + "\" RESTART IDENTITY");
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        matchScores.clear();
        feedbackCache.clear();
        documentService.clearCache();
    }

    protected LocalDate today() {
        return LocalDate.now(clock);
    }

    protected Company company(String name, CompanyStatus status) {
        Company c = new Company(name, "Technology", "hr@" + name.toLowerCase().replace(' ', '-') + ".qa");
        c.setStatus(status);
        return companies.save(c);
    }

    protected Posting openPosting(Company c, String title, List<String> required, int seats) {
        Posting p = new Posting(c, title, "A posting used in tests.");
        p.setRequiredSkills(new ArrayList<>(required));
        p.setNiceToHaveSkills(new ArrayList<>());
        p.setMinGpa(new BigDecimal("2.00"));
        p.setMinCredits(30);
        p.setSeats(seats);
        p.setDeadline(today().plusDays(10));
        p.setStartDate(today().plusDays(30));
        p.setEndDate(today().plusDays(30).plusMonths(3));
        p.setWeeklyHours(40);
        p.setMonthlyAllowance(new BigDecimal("3000"));
        p.setStatus(PostingStatus.OPEN);
        p.setCreatedAt(LocalDateTime.now(clock));
        return postings.save(p);
    }

    protected Student student(String gpa, List<String> skills) {
        counter++;
        Student s = new Student(String.format("2099%04d", counter), "Student " + counter,
                "student" + counter + "@students.test", "BSc Information Systems", new BigDecimal(gpa), 60, 0);
        s.setSkills(new ArrayList<>(skills));
        return students.save(s);
    }

    protected User studentUser(Student s) {
        User u = new User("student" + s.getId(), "{noop}unused", Role.STUDENT, s.getFullName(), s.getEmail());
        u.setStudent(s);
        return users.save(u);
    }

    protected User recruiter(Company c) {
        counter++;
        User u = new User("recruiter" + counter, "{noop}unused", Role.RECRUITER, "Recruiter " + counter,
                "r" + counter + "@test.qa");
        u.setCompany(c);
        return users.save(u);
    }

    protected void validDocuments(Student s) {
        documents.save(new StudentDocument(s, DocumentType.CV, "cv.pdf", "cv-" + s.getId() + ".pdf",
                "application/pdf", 100_000, null, LocalDateTime.now(clock)));
        documents.save(new StudentDocument(s, DocumentType.TRANSCRIPT, "t.pdf", "t-" + s.getId() + ".pdf",
                "application/pdf", 200_000, today().minusDays(30), LocalDateTime.now(clock)));
    }

    protected Application application(Student s, Posting p, ApplicationState state, int score) {
        Application a = new Application(s, p, "A motivation letter that is long enough to be accepted by the "
                + "portal because it has more than one hundred characters in total.", score, LocalDateTime.now(clock));
        a.setState(state);
        return applications.save(a);
    }
}
