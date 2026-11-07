package com.halcyon.uniplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Application application;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal monthlyAllowance;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private int weeklyHours;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime respondBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    private LocalDateTime respondedAt;

    private boolean reminderSent;

    @Column(precision = 10, scale = 2)
    private BigDecimal allowanceTotal;

    @Column(length = 4000)
    private String allowanceLines;

    protected Offer() {
    }

    public Offer(Application application, BigDecimal monthlyAllowance, LocalDate startDate, LocalDate endDate,
                 int weeklyHours, LocalDateTime issuedAt, LocalDateTime respondBy) {
        this.application = application;
        this.monthlyAllowance = monthlyAllowance;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weeklyHours = weeklyHours;
        this.issuedAt = issuedAt;
        this.respondBy = respondBy;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public BigDecimal getMonthlyAllowance() { return monthlyAllowance; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getWeeklyHours() { return weeklyHours; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getRespondBy() { return respondBy; }
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public BigDecimal getAllowanceTotal() { return allowanceTotal; }
    public void setAllowanceTotal(BigDecimal allowanceTotal) { this.allowanceTotal = allowanceTotal; }
    public String getAllowanceLines() { return allowanceLines; }
    public void setAllowanceLines(String allowanceLines) { this.allowanceLines = allowanceLines; }
}
