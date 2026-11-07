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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "postings")
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "posting_required_skills", joinColumns = @JoinColumn(name = "posting_id"))
    @OrderColumn(name = "position")
    @Column(name = "skill", length = 60)
    private List<String> requiredSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "posting_nice_skills", joinColumns = @JoinColumn(name = "posting_id"))
    @OrderColumn(name = "position")
    @Column(name = "skill", length = 60)
    private List<String> niceToHaveSkills = new ArrayList<>();

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal minGpa;

    private int minCredits;

    private int seats;

    private int seatsTaken;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private int weeklyHours;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal monthlyAllowance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostingStatus status = PostingStatus.DRAFT;

    private LocalDateTime createdAt;

    protected Posting() {
    }

    public Posting(Company company, String title, String description) {
        this.company = company;
        this.title = title;
        this.description = description;
    }

    public int seatsLeft() {
        return Math.max(0, seats - seatsTaken);
    }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public List<String> getNiceToHaveSkills() { return niceToHaveSkills; }
    public void setNiceToHaveSkills(List<String> niceToHaveSkills) { this.niceToHaveSkills = niceToHaveSkills; }
    public BigDecimal getMinGpa() { return minGpa; }
    public void setMinGpa(BigDecimal minGpa) { this.minGpa = minGpa; }
    public int getMinCredits() { return minCredits; }
    public void setMinCredits(int minCredits) { this.minCredits = minCredits; }
    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }
    public int getSeatsTaken() { return seatsTaken; }
    public void setSeatsTaken(int seatsTaken) { this.seatsTaken = seatsTaken; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; }
    public BigDecimal getMonthlyAllowance() { return monthlyAllowance; }
    public void setMonthlyAllowance(BigDecimal monthlyAllowance) { this.monthlyAllowance = monthlyAllowance; }
    public PostingStatus getStatus() { return status; }
    public void setStatus(PostingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
