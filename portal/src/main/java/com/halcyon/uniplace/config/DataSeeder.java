package com.halcyon.uniplace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Interview;
import com.halcyon.uniplace.domain.Notification;
import com.halcyon.uniplace.domain.Offer;
import com.halcyon.uniplace.domain.OfferStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.PostingStatus;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.AllowanceResult;
import com.halcyon.uniplace.engine.EngineRequestMapper;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.CompanyRepository;
import com.halcyon.uniplace.repository.InterviewRepository;
import com.halcyon.uniplace.repository.NotificationRepository;
import com.halcyon.uniplace.repository.OfferRepository;
import com.halcyon.uniplace.repository.PostingRepository;
import com.halcyon.uniplace.repository.StudentDocumentRepository;
import com.halcyon.uniplace.repository.StudentRepository;
import com.halcyon.uniplace.repository.UserRepository;
import com.halcyon.uniplace.service.DocumentService;
import com.halcyon.uniplace.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Loads the demonstration data into an empty database (REL-04). */
@Component
@ConditionalOnProperty(name = "uniplace.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEFAULT_PASSWORD = "Uniplace!2026";

    private static final String[] FIRST = {"Fatima", "Hamad", "Mariam", "Abdulla", "Aisha", "Yousef", "Noor",
            "Salem", "Reem", "Ali", "Hessa", "Faisal", "Dana", "Jassim", "Lulwa", "Nasser", "Sara", "Tamim", "Maha",
            "Rashid", "Layla", "Saad", "Hind", "Majed", "Shaikha", "Ibrahim", "Alanoud", "Mohammed", "Ruqaya",
            "Hassan", "Jawaher", "Ahmed", "Dalal", "Khalifa", "Asma", "Mubarak", "Latifa"};
    private static final String[] LAST = {"Al-Kuwari", "Al-Thani", "Al-Marri", "Al-Sulaiti", "Al-Naimi",
            "Al-Mannai", "Al-Emadi", "Al-Kaabi", "Al-Hajri", "Al-Mohannadi", "Al-Obaidli", "Al-Jaber", "Al-Ansari",
            "Al-Muftah", "Al-Dosari", "Al-Khater"};
    private static final String[] PROGRAMS = {"BSc Information Systems", "BSc Software Engineering",
            "BSc Data Science", "BSc Business Management", "BEng Mechanical Engineering"};
    private static final String[][] SKILLS_BY_PROGRAM = {
            {"SQL", "Java", "Excel", "Communication", "Power BI", "Git"},
            {"Java", "Spring", "JavaScript", "Git", "SQL", "Docker", "Selenium", "Playwright"},
            {"Python", "SQL", "Statistics", "Excel", "Power BI", "HL7"},
            {"Excel", "Communication", "Arabic", "Writing", "Power BI"},
            {"AutoCAD", "Excel", "Safety", "Python", "Writing"}};

    private final UserRepository users;
    private final StudentRepository students;
    private final StudentDocumentRepository documents;
    private final CompanyRepository companies;
    private final PostingRepository postings;
    private final ApplicationRepository applications;
    private final InterviewRepository interviews;
    private final OfferRepository offers;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;
    private final EngineClient engine;
    private final EngineRequestMapper mapper;
    private final SettingsService settings;
    private final ObjectMapper json;
    private final Clock clock;
    private final Path uploadDir;

    private final Random random = new Random(2026);
    private String passwordHash;
    private boolean engineReachable = true;

    public DataSeeder(UserRepository users, StudentRepository students, StudentDocumentRepository documents,
                      CompanyRepository companies, PostingRepository postings, ApplicationRepository applications,
                      InterviewRepository interviews, OfferRepository offers, NotificationRepository notifications,
                      PasswordEncoder encoder, EngineClient engine, EngineRequestMapper mapper,
                      SettingsService settings, ObjectMapper json, Clock clock,
                      DocumentService documentService) {
        this.users = users;
        this.students = students;
        this.documents = documents;
        this.companies = companies;
        this.postings = postings;
        this.applications = applications;
        this.interviews = interviews;
        this.offers = offers;
        this.notifications = notifications;
        this.encoder = encoder;
        this.engine = engine;
        this.mapper = mapper;
        this.settings = settings;
        this.json = json;
        this.clock = clock;
        this.uploadDir = documentService.uploadDir();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        log.info("Empty database: loading demonstration data");
        passwordHash = encoder.encode(DEFAULT_PASSWORD);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        user("coordinator", Role.COORDINATOR, "Placement Office", "placement@uniplace.local", null, null);

        Company qtech = company("QTech Solutions", "Technology", "careers@qtech.qa", CompanyStatus.APPROVED);
        Company gulf = company("Gulf Bank", "Banking", "talent@gulfbank.qa", CompanyStatus.APPROVED);
        Company energy = company("Doha Energy Partners", "Energy", "interns@dohaenergy.qa", CompanyStatus.APPROVED);
        Company health = company("Lusail Health Group", "Healthcare", "hr@lusailhealth.qa", CompanyStatus.APPROVED);
        Company pearl = company("Pearl Logistics", "Logistics", "jobs@pearllogistics.qa", CompanyStatus.APPROVED);
        Company bloom = company("Desert Bloom Media", "Media", "hello@desertbloom.qa", CompanyStatus.PENDING);

        Map<Company, User> recruiterOf = new HashMap<>();
        recruiterOf.put(qtech, user("recruiter.qtech", Role.RECRUITER, "Hana Al-Sada", "hana@qtech.qa", null, qtech));
        recruiterOf.put(gulf, user("recruiter.gulfbank", Role.RECRUITER, "Omar Rahman", "omar.r@gulfbank.qa", null, gulf));
        recruiterOf.put(energy, user("recruiter.dohaenergy", Role.RECRUITER, "Lina Haddad", "lina@dohaenergy.qa", null, energy));
        recruiterOf.put(health, user("recruiter.lusail", Role.RECRUITER, "Karim Nassar", "karim@lusailhealth.qa", null, health));
        recruiterOf.put(pearl, user("recruiter.pearl", Role.RECRUITER, "Mona Saleh", "mona@pearllogistics.qa", null, pearl));
        recruiterOf.put(bloom, user("recruiter.desertbloom", Role.RECRUITER, "Zaid Farouk", "zaid@desertbloom.qa", null, bloom));
        user("recruiter.newco", Role.RECRUITER, "Rana Khalil", "rana@newco.qa", null, null);

        Posting p1 = posting(qtech, "Backend Developer Intern", List.of("Java", "SQL", "Spring"), List.of("Git", "Docker"),
                "2.00", 45, 2, today.plusDays(14), 40, "3500", PostingStatus.OPEN);
        Posting p2 = posting(qtech, "QA Automation Intern", List.of("Java", "Selenium"), List.of("Playwright", "Git"),
                "2.00", 45, 1, today, 30, "3000", PostingStatus.OPEN);
        posting(qtech, "Data Analyst Intern", List.of("SQL", "Python"), List.of("Power BI"),
                "2.00", 45, 2, today.plusDays(21), 40, "3200", PostingStatus.DRAFT);
        Posting p4 = posting(gulf, "Risk Analytics Intern", List.of("Python", "SQL", "Statistics"), List.of("Excel"),
                "2.50", 60, 3, today.plusDays(10), 40, "4500", PostingStatus.OPEN);
        Posting p5 = posting(gulf, "Customer Experience Intern", List.of("Communication", "Excel"), List.of("Arabic"),
                "1.50", 30, 4, today.plusDays(20), 20, "2000", PostingStatus.OPEN);
        Posting p6 = posting(gulf, "IT Security Intern", List.of("Networking", "Linux"), List.of("Python"),
                "2.00", 60, 1, today.minusDays(3), 40, "4000", PostingStatus.CLOSED);
        Posting p7 = posting(energy, "Field Operations Intern", List.of("Excel", "Safety"), List.of("AutoCAD"),
                "1.50", 40, 2, today.plusDays(7), 40, "5000", PostingStatus.OPEN);
        Posting p8 = posting(energy, "Sustainability Reporting Intern", List.of("Excel", "Writing"), List.of("Python"),
                "2.00", 45, 1, today.plusDays(5), 30, "3200", PostingStatus.OPEN);
        Posting p9 = posting(energy, "Process Engineering Intern", List.of("AutoCAD", "Safety"), List.of(),
                "2.50", 70, 2, today.minusDays(20), 40, "4200", PostingStatus.CLOSED);
        Posting p10 = posting(health, "Health Informatics Intern", List.of("SQL", "HL7"), List.of("Python", "Java"),
                "2.00", 45, 2, today.plusDays(12), 35, "3800", PostingStatus.OPEN);
        Posting p11 = posting(health, "Hospital Administration Intern", List.of("Excel", "Communication"), List.of(),
                "2.00", 30, 1, today.minusDays(2), 40, "3000", PostingStatus.FILLED);
        Posting p12 = posting(pearl, "Supply Chain Intern", List.of("Excel", "SQL"), List.of("Power BI"),
                "1.50", 30, 3, today.plusDays(9), 40, "2800", PostingStatus.OPEN);
        Posting p13 = posting(pearl, "Software Support Intern", List.of("JavaScript", "SQL"), List.of("Java"),
                "2.00", 45, 2, today.plusDays(15), 25, "2500", PostingStatus.OPEN);
        posting(bloom, "Content Creator Intern", List.of("Writing", "Arabic"), List.of(),
                "2.00", 30, 1, today.plusDays(18), 20, "1800", PostingStatus.DRAFT);

        // Named students used throughout the documentation.
        Student amal = student("20231001", "Amal Al-Kuwari", PROGRAMS[1], "3.40", 75, 1,
                List.of("Java", "SQL", "Git", "Spring", "Python"), today.minusDays(40), true);
        Student omar = student("20231002", "Omar Al-Sulaiti", PROGRAMS[3], "1.85", 60, 1,
                List.of("Excel", "SQL", "Communication"), today.minusDays(60), true);
        Student khalid = student("20231003", "Khalid Al-Marri", PROGRAMS[4], "1.70", 50, 2,
                List.of("Excel", "Safety", "AutoCAD"), today.minusDays(30), true);
        Student noor = student("20231004", "Noor Al-Emadi", PROGRAMS[2], "3.10", 80, 0,
                List.of("Python", "SQL", "Statistics", "HL7"), today.minusDays(179), true);
        Student yousef = student("20231005", "Yousef Al-Naimi", PROGRAMS[0], "2.90", 70, 0,
                List.of("SQL", "Excel", "JavaScript"), today.minusDays(20), false);

        List<Student> all = new ArrayList<>(List.of(amal, omar, khalid, noor, yousef));
        Set<String> names = new HashSet<>();
        for (Student s : all) {
            names.add(s.getFullName());
        }
        int number = 20231006;
        while (all.size() < 40) {
            String first = FIRST[random.nextInt(FIRST.length)];
            String full = first + " " + LAST[random.nextInt(LAST.length)];
            if (!names.add(full)) {
                continue;
            }
            int programIndex = random.nextInt(PROGRAMS.length);
            String gpa = BigDecimal.valueOf(165 + random.nextInt(230)).movePointLeft(2).toPlainString();
            List<String> skills = pickSkills(SKILLS_BY_PROGRAM[programIndex]);
            all.add(student(String.valueOf(number++), full, PROGRAMS[programIndex], gpa, 30 + random.nextInt(100),
                    random.nextInt(10) < 7 ? 0 : 1, skills, today.minusDays(5 + random.nextInt(150)), true));
        }

        // Fixed scenario applications.
        LocalDateTime t = now.minusDays(12);
        apply(amal, p10, ApplicationState.SUBMITTED, t.plusHours(1), recruiterOf);
        apply(amal, p1, ApplicationState.SHORTLISTED, t.plusHours(2), recruiterOf);
        Application amalOffer = apply(amal, p13, ApplicationState.OFFERED, t.plusHours(3), recruiterOf);
        apply(amal, p4, ApplicationState.WITHDRAWN, t.plusHours(4), recruiterOf);
        apply(omar, p5, ApplicationState.SUBMITTED, t.plusHours(5), recruiterOf);
        apply(omar, p6, ApplicationState.REJECTED, t.minusDays(10), recruiterOf);
        Application p8first = apply(all.get(10), p8, ApplicationState.OFFERED, t.plusHours(6), recruiterOf);
        Application p8second = apply(all.get(11), p8, ApplicationState.OFFERED, t.plusHours(7), recruiterOf);
        apply(all.get(12), p8, ApplicationState.SUBMITTED, t.plusHours(8), recruiterOf);
        Application placed = apply(all.get(13), p11, ApplicationState.ACCEPTED, t.minusDays(15), recruiterOf);
        apply(all.get(14), p11, ApplicationState.REJECTED, t.minusDays(14), recruiterOf);

        offer(amalOffer, now.minusHours(20));
        offer(p8first, now.minusHours(50));
        offer(p8second, now.minusHours(10));
        Offer accepted = offer(placed, now.minusDays(6));
        accepted.setStatus(OfferStatus.ACCEPTED);
        accepted.setRespondedAt(now.minusDays(5));
        computeAllowance(accepted);
        p11.setSeatsTaken(1);

        // Background population.
        List<Posting> targets = List.of(p1, p2, p4, p5, p6, p7, p9, p10, p12, p13);
        ApplicationState[] states = {ApplicationState.SUBMITTED, ApplicationState.SUBMITTED, ApplicationState.SUBMITTED,
                ApplicationState.SUBMITTED, ApplicationState.SHORTLISTED, ApplicationState.SHORTLISTED,
                ApplicationState.INTERVIEW, ApplicationState.REJECTED, ApplicationState.REJECTED,
                ApplicationState.WITHDRAWN};
        for (int i = 5; i < all.size(); i++) {
            Student s = all.get(i);
            if (s == placed.getStudent()) {
                continue;
            }
            int wanted = 2 + random.nextInt(3);
            List<Posting> shuffled = new ArrayList<>(targets);
            java.util.Collections.shuffle(shuffled, random);
            for (Posting p : shuffled) {
                if (wanted == 0) {
                    break;
                }
                if (hasApplication(s, p)) {
                    continue;
                }
                ApplicationState state = p.getStatus() == PostingStatus.CLOSED
                        ? (random.nextBoolean() ? ApplicationState.REJECTED : ApplicationState.WITHDRAWN)
                        : states[random.nextInt(states.length)];
                if (state.isActive() && activeCount(s) >= limitFor(s)) {
                    state = ApplicationState.WITHDRAWN;
                }
                apply(s, p, state, now.minusDays(1 + random.nextInt(25)).minusMinutes(random.nextInt(600)), recruiterOf);
                wanted--;
            }
        }
        // Further scenarios, added after the generated population so that the population stays the same.
        user("recruiter2.qtech", Role.RECRUITER, "Faisal Al-Thani", "faisal@qtech.qa", null, qtech);
        apply(noor, p10, ApplicationState.INTERVIEW, t.plusHours(9), recruiterOf);
        apply(noor, p1, ApplicationState.SHORTLISTED, t.plusHours(10), recruiterOf);
        apply(noor, p2, ApplicationState.SHORTLISTED, t.plusHours(11), recruiterOf);
        Student amna = student("20231041", "Amna Al-Kubaisi", PROGRAMS[2], "3.55", 90, 0,
                List.of("Python", "SQL", "Statistics", "Excel"), today.minusDays(25), true);
        Application riskPlaced = apply(amna, p4, ApplicationState.ACCEPTED, t.minusDays(1), recruiterOf);
        Offer riskAccepted = offer(riskPlaced, now.minusDays(4));
        riskAccepted.setStatus(OfferStatus.ACCEPTED);
        riskAccepted.setRespondedAt(now.minusDays(3));
        computeAllowance(riskAccepted);
        p4.setSeatsTaken(1);

        log.info("Demonstration data loaded: {} users, {} applications", users.count(), applications.count());
    }

    // ------------------------------------------------------------------ helpers

    private final Map<Long, List<Application>> byStudent = new HashMap<>();
    private final Map<Long, LocalDateTime> nextSlot = new HashMap<>();

    private boolean hasApplication(Student s, Posting p) {
        return byStudent.getOrDefault(s.getId(), List.of()).stream().anyMatch(a -> a.getPosting() == p);
    }

    private long activeCount(Student s) {
        return byStudent.getOrDefault(s.getId(), List.of()).stream().filter(a -> a.getState().isActive()).count();
    }

    private int limitFor(Student s) {
        return s.getStanding() == com.halcyon.uniplace.domain.Standing.PROBATION
                ? settings.maxActiveProbation() : settings.maxActiveGood();
    }

    private User user(String username, Role role, String name, String email, Student student, Company company) {
        User u = new User(username, passwordHash, role, name, email);
        u.setStudent(student);
        u.setCompany(company);
        return users.save(u);
    }

    private Company company(String name, String sector, String email, CompanyStatus status) {
        Company c = new Company(name, sector, email);
        c.setStatus(status);
        return companies.save(c);
    }

    private Posting posting(Company c, String title, List<String> required, List<String> nice, String minGpa,
                            int minCredits, int seats, LocalDate deadline, int hours, String allowance,
                            PostingStatus status) {
        Posting p = new Posting(c, title, describe(title, c, required));
        p.setRequiredSkills(new ArrayList<>(required));
        p.setNiceToHaveSkills(new ArrayList<>(nice));
        p.setMinGpa(new BigDecimal(minGpa));
        p.setMinCredits(minCredits);
        p.setSeats(seats);
        p.setDeadline(deadline);
        p.setStartDate(deadline.plusDays(21));
        p.setEndDate(deadline.plusDays(21).plusMonths(3).minusDays(1));
        p.setWeeklyHours(hours);
        p.setMonthlyAllowance(new BigDecimal(allowance));
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now(clock).minusDays(30));
        return postings.save(p);
    }

    private static String describe(String title, Company c, List<String> required) {
        return c.getName() + " is looking for a motivated " + title.toLowerCase(Locale.ROOT)
                + " to join its team in Doha. You will work with experienced staff on real projects and receive "
                + "weekly mentoring. Essential skills: " + String.join(", ", required) + ".";
    }

    private Student student(String number, String fullName, String program, String gpa, int credits, int prior,
                            List<String> skills, LocalDate transcriptIssued, boolean withCv) {
        String first = fullName.split(" ")[0].toLowerCase(Locale.ROOT);
        String email = first + "." + number.substring(4) + "@students.uniplace.edu.qa";
        Student s = new Student(number, fullName, email, program, new BigDecimal(gpa), credits, prior);
        s.setSkills(new ArrayList<>(skills));
        s = students.save(s);
        String username = "student." + first;
        int n = 2;
        while (users.existsByUsername(username)) {
            username = "student." + first + n++;
        }
        user(username, Role.STUDENT, fullName, email, s, null);
        if (withCv) {
            document(s, DocumentType.CV, "CV_" + first + ".pdf", null);
        }
        document(s, DocumentType.TRANSCRIPT, "Transcript_" + first + ".pdf", transcriptIssued);
        return s;
    }

    private void document(Student s, DocumentType type, String name, LocalDate issued) {
        byte[] pdf = ("%PDF-1.4\n% UniPlace demonstration document for " + s.getFullName() + "\n%%EOF\n")
                .getBytes(StandardCharsets.UTF_8);
        String stored = UUID.randomUUID() + ".pdf";
        try {
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(stored), pdf);
        } catch (IOException ex) {
            log.warn("Could not write demonstration document {}", stored);
        }
        documents.save(new StudentDocument(s, type, name, stored, "application/pdf", 180_000 + random.nextInt(400_000),
                issued, LocalDateTime.now(clock).minusDays(3)));
    }

    private List<String> pickSkills(String[] pool) {
        List<String> list = new ArrayList<>(List.of(pool));
        java.util.Collections.shuffle(list, random);
        return new ArrayList<>(list.subList(0, 2 + random.nextInt(Math.min(4, list.size() - 1))));
    }

    private Application apply(Student s, Posting p, ApplicationState state, LocalDateTime when,
                              Map<Company, User> recruiterOf) {
        Application a = new Application(s, p, letterFor(s, p), score(s, p), when);
        a.setState(state);
        a.setUpdatedAt(when.plusDays(state == ApplicationState.SUBMITTED ? 0 : 2));
        a = applications.save(a);
        byStudent.computeIfAbsent(s.getId(), k -> new ArrayList<>()).add(a);
        users.findByStudent(s).ifPresent(u -> notifications.save(new Notification(u, "Application submitted: "
                + p.getTitle(), "Your application to " + p.getTitle() + " at " + p.getCompany().getName()
                + " was submitted.", when)));
        if (state == ApplicationState.INTERVIEW) {
            User recruiter = recruiterOf.get(p.getCompany());
            LocalDateTime start = nextSlot(recruiter);
            interviews.save(new Interview(a, recruiter, start, start.plusMinutes(45), "Meeting room 2, "
                    + p.getCompany().getName() + " office"));
        }
        return a;
    }

    private LocalDateTime nextSlot(User recruiter) {
        LocalDateTime current = nextSlot.get(recruiter.getId());
        if (current == null) {
            LocalDate d = LocalDate.now(clock).plusDays(2);
            current = d.atTime(LocalTime.of(9, 0));
        } else {
            current = current.plusHours(1);
            if (current.getHour() >= 16) {
                current = current.toLocalDate().plusDays(1).atTime(9, 0);
            }
        }
        while (current.getDayOfWeek() == DayOfWeek.FRIDAY || current.getDayOfWeek() == DayOfWeek.SATURDAY) {
            current = current.plusDays(1);
        }
        nextSlot.put(recruiter.getId(), current);
        return current;
    }

    private Offer offer(Application a, LocalDateTime issuedAt) {
        Posting p = a.getPosting();
        return offers.save(new Offer(a, p.getMonthlyAllowance(), p.getStartDate(), p.getEndDate(), p.getWeeklyHours(),
                issuedAt, issuedAt.plusHours(settings.offerResponseHours())));
    }

    private int score(Student s, Posting p) {
        if (engineReachable) {
            try {
                return engine.match(mapper.toMatchRequest(s.getSkills(), p)).score();
            } catch (RuntimeException ex) {
                engineReachable = false;
                log.warn("Engine not reachable while loading demonstration data; using estimated scores");
            }
        }
        long matched = p.getRequiredSkills().stream().filter(r -> s.getSkills().stream().anyMatch(r::equalsIgnoreCase)).count();
        return (int) Math.round(70.0 * matched / p.getRequiredSkills().size());
    }

    private void computeAllowance(Offer o) {
        if (!engineReachable) {
            return;
        }
        try {
            AllowanceResult r = engine.allowance(mapper.toAllowanceRequest(o, 0));
            o.setAllowanceTotal(r.total());
            o.setAllowanceLines(json.writeValueAsString(r.lines()));
        } catch (Exception ex) {
            log.warn("Allowance for the demonstration placement not computed");
        }
    }

    private String letterFor(Student s, Posting p) {
        String[] openings = {
                "I am writing to apply for the " + p.getTitle() + " position at " + p.getCompany().getName() + ".",
                "Please consider my application for the " + p.getTitle() + " internship.",
                "I would like to join " + p.getCompany().getName() + " as a " + p.getTitle() + "."};
        return openings[random.nextInt(openings.length)] + " I am a " + s.getProgram() + " student with "
                + s.getCreditsCompleted() + " credits completed. During my studies I worked with "
                + String.join(", ", s.getSkills()) + " in course projects and team assignments. I am motivated to "
                + "learn from experienced colleagues and to contribute to real projects from the first week.";
    }
}
