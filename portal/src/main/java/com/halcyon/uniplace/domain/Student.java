package com.halcyon.uniplace.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String studentNumber;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 80)
    private String program;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal gpa;

    private int creditsCompleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Standing standing;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_skills", joinColumns = @JoinColumn(name = "student_id"))
    @OrderColumn(name = "position")
    @Column(name = "skill", length = 60)
    private List<String> skills = new ArrayList<>();

    private int priorInternships;

    protected Student() {
    }

    public Student(String studentNumber, String fullName, String email, String program, BigDecimal gpa,
                   int creditsCompleted, int priorInternships) {
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.email = email;
        this.program = program;
        this.gpa = gpa;
        this.creditsCompleted = creditsCompleted;
        this.priorInternships = priorInternships;
        this.standing = Standing.fromGpa(gpa);
    }

    public Long getId() { return id; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }
    public int getCreditsCompleted() { return creditsCompleted; }
    public void setCreditsCompleted(int creditsCompleted) { this.creditsCompleted = creditsCompleted; }
    public Standing getStanding() { return standing; }
    public void setStanding(Standing standing) { this.standing = standing; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public int getPriorInternships() { return priorInternships; }
    public void setPriorInternships(int priorInternships) { this.priorInternships = priorInternships; }
}
