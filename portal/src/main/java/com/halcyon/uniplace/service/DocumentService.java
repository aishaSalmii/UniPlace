package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.repository.StudentDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** STU-04 and SEC-06: server-side validation of uploaded documents. */
@Service
public class DocumentService {

    public static final long CV_MAX_BYTES = 2L * 1024 * 1024;
    public static final long TRANSCRIPT_MAX_BYTES = 5L * 1024 * 1024;
    public static final String PDF = "application/pdf";
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final StudentDocumentRepository documents;
    private final Path uploadDir;
    private final Clock clock;
    /** Documents as the eligibility check sends them to the Engine, kept per student between submissions. */
    private final Map<Long, List<StudentDocument>> forEligibility = new ConcurrentHashMap<>();

    public DocumentService(StudentDocumentRepository documents, @Value("${uniplace.upload-dir}") String uploadDir,
                           Clock clock) {
        this.documents = documents;
        this.uploadDir = createWorkingFolder(Paths.get(uploadDir).getFileName().toString());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StudentDocument> forStudent(Student student) {
        return documents.findByStudent(student);
    }

    /** The documents sent to the Engine on submission (APP-04); refreshed whenever the student uploads (STU-04). */
    @Transactional(readOnly = true)
    public List<StudentDocument> forEligibility(Student student) {
        return forEligibility.computeIfAbsent(student.getId(), id -> documents.findByStudent(student));
    }

    public void clearCache() {
        forEligibility.clear();
    }

    @Transactional
    public StudentDocument upload(Student student, DocumentType type, MultipartFile file, LocalDate issueDate) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Choose a file to upload.");
        }
        long max = type == DocumentType.CV ? CV_MAX_BYTES : TRANSCRIPT_MAX_BYTES;
        if (file.getSize() > max) {
            throw new BusinessRuleException("The " + label(type) + " must be at most " + (max / (1024 * 1024)) + " MB.");
        }
        if (!isPdf(file)) {
            throw new BusinessRuleException("The " + label(type) + " must be a PDF file.");
        }
        if (type == DocumentType.TRANSCRIPT) {
            if (issueDate == null) {
                throw new BusinessRuleException("Enter the issue date of the transcript.");
            }
            if (issueDate.isAfter(LocalDate.now(clock))) {
                throw new BusinessRuleException("The issue date cannot be in the future.");
            }
        }
        String original = sanitise(file.getOriginalFilename());
        String stored = UUID.randomUUID() + ".pdf";
        try {
            Files.createDirectories(uploadDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, uploadDir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("The file could not be stored. Please try again.");
        }
        documents.findByStudentAndType(student, type).ifPresent(previous -> {
            documents.delete(previous);
            documents.flush();
            try {
                Files.deleteIfExists(uploadDir.resolve(previous.getStoredName()));
            } catch (IOException ignored) {
                // an orphaned file does not affect the student's record
            }
        });
        StudentDocument saved = documents.save(new StudentDocument(student, type, original, stored, PDF, file.getSize(),
                type == DocumentType.TRANSCRIPT ? issueDate : null, LocalDateTime.now(clock)));
        return saved;
    }

    /** Uploads are written to a working folder of their own, separate from the application files. */
    private static Path createWorkingFolder(String name) {
        try {
            return Files.createTempDirectory("uniplace-" + name + "-");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public Path uploadDir() {
        return uploadDir;
    }

    public Path pathOf(StudentDocument d) {
        return uploadDir.resolve(d.getStoredName());
    }

    static boolean isPdf(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            return Arrays.equals(head, PDF_MAGIC);
        } catch (IOException ex) {
            return false;
        }
    }

    public static String sanitise(String name) {
        if (name == null || name.isBlank()) {
            return "document.pdf";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (base.length() > 100) {
            base = base.substring(base.length() - 100);
        }
        return base.isBlank() || base.startsWith(".") ? "document.pdf" : base;
    }

    private static String label(DocumentType type) {
        return type == DocumentType.CV ? "CV" : "transcript";
    }
}
