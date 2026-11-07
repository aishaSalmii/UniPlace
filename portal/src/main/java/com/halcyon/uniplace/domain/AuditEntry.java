package com.halcyon.uniplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String username;

    @Column(nullable = false)
    private LocalDateTime at;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 200)
    private String target;

    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;

    protected AuditEntry() {
    }

    public AuditEntry(String username, LocalDateTime at, String action, String target, String oldValue, String newValue) {
        this.username = username;
        this.at = at;
        this.action = action;
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public LocalDateTime getAt() { return at; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}
