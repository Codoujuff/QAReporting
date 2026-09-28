package com.qareporting.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One immutable row per execution of a Test — history is append-only, a
 * result is never overwritten (same rule as the Laravel version).
 */
@Entity
@Table(name = "test_executions")
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Test.Status status;

    @Column(name = "actual_result", columnDefinition = "TEXT") // texte long (64 Ko)
    private String actualResult;

    @ManyToOne
    @JoinColumn(name = "executed_by")
    private User executedBy;

    @ManyToOne
    @JoinColumn(name = "environment_id")
    private Environment environment;

    private String duration;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public Test.Status getStatus() { return status; }
    public void setStatus(Test.Status status) { this.status = status; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public User getExecutedBy() { return executedBy; }
    public void setExecutedBy(User executedBy) { this.executedBy = executedBy; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
