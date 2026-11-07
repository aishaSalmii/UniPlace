package com.halcyon.uniplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_documents")
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    @Column(nullable = false, length = 200)
    private String originalName;

    @Column(nullable = false, length = 80)
    private String storedName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    private long sizeBytes;

    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    protected StudentDocument() {
    }

    public StudentDocument(Student student, DocumentType type, String originalName, String storedName,
                           String mimeType, long sizeBytes, LocalDate issueDate, LocalDateTime uploadedAt) {
        this.student = student;
        this.type = type;
        this.originalName = originalName;
        this.storedName = storedName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.issueDate = issueDate;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public DocumentType getType() { return type; }
    public String getOriginalName() { return originalName; }
    public String getStoredName() { return storedName; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
