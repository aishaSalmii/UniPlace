package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.repository.NotificationRepository;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.AuditService;
import com.halcyon.uniplace.service.ApplicationService;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.CompanyService;
import com.halcyon.uniplace.service.NotificationService;
import com.halcyon.uniplace.service.OfferService;
import com.halcyon.uniplace.service.ReportService;
import com.halcyon.uniplace.service.StudentService;
import com.halcyon.uniplace.service.TestDataService;
import com.halcyon.uniplace.service.UserService;
import org.springframework.data.domain.PageImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
public class CoordinatorController {

    private final StudentService students;
    private final CompanyService companies;
    private final UserService users;
    private final ApplicationService applications;
    private final OfferService offers;
    private final NotificationService notifications;
    private final AuditService audit;
    private final ReportService reports;
    private final TestDataService testData;
    private final CurrentUser currentUser;
    private final NotificationRepository notificationRepository;

    public CoordinatorController(StudentService students, CompanyService companies, UserService users,
                                 ApplicationService applications, OfferService offers,
                                 NotificationService notifications, AuditService audit, ReportService reports,
                                 TestDataService testData, CurrentUser currentUser,
                                 NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
        this.students = students;
        this.companies = companies;
        this.users = users;
        this.applications = applications;
        this.offers = offers;
        this.notifications = notifications;
        this.audit = audit;
        this.reports = reports;
        this.testData = testData;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------- students

    @GetMapping("/coordinator/students")
    public String students(Model model) {
        model.addAttribute("students", students.all());
        return "coordinator/students";
    }

    @GetMapping("/coordinator/students/{id}")
    public String student(@PathVariable Long id, Model model) {
        model.addAttribute("student", students.get(id));
        return "coordinator/student";
    }

    @PostMapping("/coordinator/students/{id}")
    public String saveStudent(@PathVariable Long id, @RequestParam BigDecimal gpa, @RequestParam int creditsCompleted,
                              @RequestParam int priorInternships, @RequestParam(required = false) String program,
                              RedirectAttributes ra) {
        try {
            students.updateAcademicRecord(id, gpa, creditsCompleted, priorInternships, program);
            ra.addFlashAttribute("success", "Academic record saved.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/coordinator/students/" + id;
    }

    @PostMapping("/coordinator/students")
    public String createStudent(@RequestParam String studentNumber, @RequestParam String fullName,
                                @RequestParam String email, @RequestParam String program, @RequestParam BigDecimal gpa,
                                @RequestParam int creditsCompleted, @RequestParam(defaultValue = "0") int priorInternships,
                                @RequestParam String username, @RequestParam String password, RedirectAttributes ra) {
        try {
            Student s = createStudentAndUser(studentNumber, fullName, email, program, gpa, creditsCompleted,
                    priorInternships, username, password);
            ra.addFlashAttribute("success", "Student " + s.getFullName() + " created.");
            return "redirect:/coordinator/students/" + s.getId();
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/coordinator/students";
        }
    }

    private Student createStudentAndUser(String number, String fullName, String email, String program, BigDecimal gpa,
                                 int credits, int prior, String username, String password) {
        UserService.checkPassword(password);
        users.assertUsernameAvailable(username);
        Student s = students.create(number, fullName, email, program, gpa, credits, prior);
        users.create(username, password, Role.STUDENT, fullName, email, s, null);
        return s;
    }

    // --------------------------------------------------------------- companies

    @GetMapping("/coordinator/companies")
    public String companies(Model model) {
        model.addAttribute("companies", companies.all());
        return "coordinator/companies";
    }

    @PostMapping("/coordinator/companies/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        companies.approve(id);
        ra.addFlashAttribute("success", "Company approved.");
        return "redirect:/coordinator/companies";
    }

    @PostMapping("/coordinator/companies/{id}/suspend")
    public String suspend(@PathVariable Long id, RedirectAttributes ra) {
        try {
            companies.suspend(id);
            ra.addFlashAttribute("success", "Company suspended.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/coordinator/companies";
    }

    // ------------------------------------------------------------------- users

    @GetMapping("/coordinator/users")
    @Transactional(readOnly = true)
    public String users(Model model) {
        model.addAttribute("users", users.all());
        model.addAttribute("companies", companies.all());
        return "coordinator/users";
    }

    @PostMapping("/coordinator/users")
    public String createUser(@RequestParam String username, @RequestParam String password, @RequestParam Role role,
                             @RequestParam String displayName, @RequestParam(required = false) String email,
                             @RequestParam(required = false) Long companyId, RedirectAttributes ra) {
        try {
            if (role == Role.STUDENT) {
                throw new BusinessRuleException("Create students from the Students page.");
            }
            users.create(username, password, role, displayName, email, null,
                    companyId == null || role != Role.RECRUITER ? null : companies.get(companyId));
            ra.addFlashAttribute("success", "User " + username.trim().toLowerCase() + " created.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/coordinator/users";
    }

    @PostMapping("/coordinator/users/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes ra) {
        users.unlock(id);
        ra.addFlashAttribute("success", "Account unlocked.");
        return "redirect:/coordinator/users";
    }

    // ------------------------------------------------------------ applications

    @GetMapping("/coordinator/applications")
    @Transactional(readOnly = true)
    public String applications(@RequestParam(required = false) ApplicationState state,
                               @RequestParam(required = false) Long postingId,
                               @RequestParam(required = false) Long companyId,
                               @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", applications.pageForCoordinator(state, postingId, companyId, page));
        model.addAttribute("state", state);
        model.addAttribute("postingId", postingId);
        model.addAttribute("companyId", companyId);
        model.addAttribute("states", ApplicationState.values());
        model.addAttribute("companies", companies.all());
        return "coordinator/applications";
    }

    @PostMapping("/coordinator/applications/{id}/agreement")
    public String agreement(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedOn,
                            RedirectAttributes ra) {
        try {
            offers.recordAgreement(id, receivedOn);
            ra.addFlashAttribute("success", "Agreement date recorded and allowance recalculated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/coordinator/offers/{id}/recalculate")
    public String recalculate(@PathVariable Long id, @RequestParam Long applicationId, RedirectAttributes ra) {
        try {
            offers.recalculate(id);
            ra.addFlashAttribute("success", "Allowance statement recalculated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/applications/" + applicationId;
    }

    // ------------------------------------------------------- audit and outbox

    @GetMapping("/coordinator/audit")
    public String audit(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", audit.page(page));
        return "coordinator/audit";
    }

    /** NOT-03: the whole Outbox. */
    @GetMapping("/coordinator/outbox")
    @Transactional(readOnly = true)
    public String outbox(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", new PageImpl<>(notificationRepository.findAllByOrderByCreatedAtDescIdDesc()));
        model.addAttribute("all", true);
        return "notifications";
    }

    // ----------------------------------------------------------------- reports

    @GetMapping("/coordinator/reports")
    public String reports(Model model) {
        model.addAttribute("tables", reports.all());
        return "coordinator/reports";
    }

    @GetMapping("/coordinator/reports/{name}.csv")
    public ResponseEntity<byte[]> export(@PathVariable String name) {
        byte[] body = ReportService.toCsv(reports.byName(name)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"uniplace-" + name + ".csv\"")
                .body(body);
    }

    // ------------------------------------------------------------------- tools

    @GetMapping("/coordinator/tools")
    public String tools() {
        return "coordinator/tools";
    }

    @PostMapping("/coordinator/tools/generate")
    public String generate(@RequestParam(defaultValue = "1000") int count, RedirectAttributes ra) {
        try {
            TestDataService.Result r = testData.generate(count);
            ra.addFlashAttribute("success", "Generated " + r.applications() + " applications, " + r.students()
                    + " students, " + r.postings() + " postings and " + r.notifications() + " notifications.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/coordinator/tools";
    }
}
