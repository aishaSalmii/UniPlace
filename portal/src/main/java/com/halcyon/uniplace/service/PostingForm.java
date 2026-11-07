package com.halcyon.uniplace.service;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fields a recruiter edits on a posting. Skills are comma-separated. */
public class PostingForm {

    private String title;
    private String description;
    private String requiredSkills;
    private String niceToHaveSkills;
    private BigDecimal minGpa;
    private Integer minCredits;
    private Integer seats;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Integer weeklyHours;
    private BigDecimal monthlyAllowance;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getNiceToHaveSkills() { return niceToHaveSkills; }
    public void setNiceToHaveSkills(String niceToHaveSkills) { this.niceToHaveSkills = niceToHaveSkills; }
    public BigDecimal getMinGpa() { return minGpa; }
    public void setMinGpa(BigDecimal minGpa) { this.minGpa = minGpa; }
    public Integer getMinCredits() { return minCredits; }
    public void setMinCredits(Integer minCredits) { this.minCredits = minCredits; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(Integer weeklyHours) { this.weeklyHours = weeklyHours; }
    public BigDecimal getMonthlyAllowance() { return monthlyAllowance; }
    public void setMonthlyAllowance(BigDecimal monthlyAllowance) { this.monthlyAllowance = monthlyAllowance; }
}
