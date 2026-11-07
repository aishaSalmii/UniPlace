package com.halcyon.uniplace.web;

import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Role;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.ApplicationRepository;
import com.halcyon.uniplace.repository.StudentDocumentRepository;
import com.halcyon.uniplace.security.CurrentUser;
import com.halcyon.uniplace.service.AccessGuard;
import com.halcyon.uniplace.service.BusinessRuleException;
import com.halcyon.uniplace.service.DocumentService;
import com.halcyon.uniplace.service.ForbiddenException;
import com.halcyon.uniplace.service.NotFoundException;
import com.halcyon.uniplace.service.StudentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Objects;

@Controller
public class StudentController {

    private final StudentService students;
    private final DocumentService documents;
    private final StudentDocumentRepository documentRepository;
    private final ApplicationRepository applications;
    private final CurrentUser currentUser;
    private final AccessGuard guard;

    public StudentController(StudentService students, DocumentService documents,
                             StudentDocumentRepository documentRepository, ApplicationRepository applications,
                             CurrentUser currentUser, AccessGuard guard) {
        this.students = students;
        this.documents = documents;
        this.documentRepository = documentRepository;
        this.applications = applications;
        this.currentUser = currentUser;
        this.guard = guard;
    }

    @GetMapping("/student/profile")
    @Transactional(readOnly = true)
    public String profile(Model model) {
        User user = currentUser.get();
        model.addAttribute("student", user.getStudent());
        model.addAttribute("skills", String.join(", ", user.getStudent().getSkills()));
        model.addAttribute("documents", documents.forStudent(user.getStudent()));
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String saveProfile(@RequestParam String fullName, @RequestParam String email,
                              @RequestParam(defaultValue = "") String skills, RedirectAttributes ra) {
        User user = currentUser.get();
        try {
            students.updateProfile(user.getStudent().getId(), fullName, email, skills);
            ra.addFlashAttribute("success", "Profile updated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/student/documents")
    public String upload(@RequestParam DocumentType type, @RequestParam(required = false) MultipartFile file,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
                         RedirectAttributes ra) {
        User user = currentUser.get();
        try {
            documents.upload(user.getStudent(), type, file, issueDate);
            ra.addFlashAttribute("success", (type == DocumentType.CV ? "CV" : "Transcript") + " uploaded.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/profile";
    }

    @GetMapping("/documents/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        User user = currentUser.get();
        StudentDocument d = documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document"));
        boolean allowed = user.is(Role.COORDINATOR)
                || (user.is(Role.STUDENT) && user.getStudent() != null
                    && Objects.equals(user.getStudent().getId(), d.getStudent().getId()))
                || user.is(Role.RECRUITER);
        if (!allowed) {
            throw new ForbiddenException();
        }
        FileSystemResource file = new FileSystemResource(documents.pathOf(d));
        if (!file.exists()) {
            throw new NotFoundException("Document");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.getOriginalName()).build().toString())
                .body(file);
    }
}
