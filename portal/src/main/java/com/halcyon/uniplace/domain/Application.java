package com.halcyon.uniplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications", indexes = {
        @Index(name = "idx_app_student", columnList = "student_id"),
        @Index(name = "idx_app_posting", columnList = "posting_id"),
        @Index(name = "idx_app_state", columnList = "state")
})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Posting posting;

    @Column(nullable = false, length = 2000)
    private String letter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationState state = ApplicationState.SUBMITTED;

    private int matchScore;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;

    @Column(length = 1000)
    private String overrideJustification;

    private boolean rescheduleRequested;

    private LocalDate agreementReceivedOn;

    protected Application() {
    }

    public Application(Student student, Posting posting, String letter, int matchScore, LocalDateTime submittedAt) {
        this.student = student;
        this.posting = posting;
        this.letter = letter;
        this.matchScore = matchScore;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Posting getPosting() { return posting; }
    public String getLetter() { return letter; }
    public ApplicationState getState() { return state; }
    public void setState(ApplicationState state) { this.state = state; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getOverrideJustification() { return overrideJustification; }
    public void setOverrideJustification(String overrideJustification) { this.overrideJustification = overrideJustification; }
    public boolean isRescheduleRequested() { return rescheduleRequested; }
    public void setRescheduleRequested(boolean rescheduleRequested) { this.rescheduleRequested = rescheduleRequested; }
    public LocalDate getAgreementReceivedOn() { return agreementReceivedOn; }
    public void setAgreementReceivedOn(LocalDate agreementReceivedOn) { this.agreementReceivedOn = agreementReceivedOn; }
}
