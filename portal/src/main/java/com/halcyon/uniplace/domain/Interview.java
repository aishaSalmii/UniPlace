package com.halcyon.uniplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User recruiter;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime start;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime end;

    @Column(nullable = false, length = 200)
    private String location;

    private boolean cancelled;

    protected Interview() {
    }

    public Interview(Application application, User recruiter, LocalDateTime start, LocalDateTime end, String location) {
        this.application = application;
        this.recruiter = recruiter;
        this.start = start;
        this.end = end;
        this.location = location;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public User getRecruiter() { return recruiter; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
