package com.qareporting.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "activities")
public class Activity extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Verrou optimiste : une modification faite sur une version périmée est refusée au lieu d'écraser celle d'un collègue. */
    @Version
    @Column(name = "lock_version", nullable = false)
    @jakarta.json.bind.annotation.JsonbTransient
    private long lockVersion;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "tests_executed", nullable = false)
    private int testsExecuted = 0;

    @Column(nullable = false)
    private int passed = 0;

    @Column(nullable = false)
    private int failed = 0;

    @Column(nullable = false)
    private int blocked = 0;

    @Column(name = "not_run", nullable = false)
    private int notRun = 0;

    @Column(name = "defects_count", nullable = false)
    private int defectsCount = 0;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "blocked_reason", columnDefinition = "TEXT") // texte long (64 Ko)
    private String blockedReason;

    @Column(columnDefinition = "TEXT") // texte long (64 Ko)
    private String comment;

    private String duration;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    /** Validation par le QA Lead de l'équipe (ou l'admin) : une déclaration validée n'est plus modifiable. */
    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private java.time.LocalDateTime validatedAt;

    /** Business rule (server-enforced, same as the Laravel ActivityRequest validator):
     *  passed + failed + blocked + not_run must always equal tests_executed. */
    public boolean isCounterSumValid() {
        return testsExecuted == passed + failed + blocked + notRun;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public int getTestsExecuted() { return testsExecuted; }
    public void setTestsExecuted(int testsExecuted) { this.testsExecuted = testsExecuted; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public int getBlocked() { return blocked; }
    public void setBlocked(int blocked) { this.blocked = blocked; }
    public int getNotRun() { return notRun; }
    public void setNotRun(int notRun) { this.notRun = notRun; }
    public int getDefectsCount() { return defectsCount; }
    public void setDefectsCount(int defectsCount) { this.defectsCount = defectsCount; }
    public boolean isBlocked() { return isBlocked; }
    public void setIsBlocked(boolean blocked) { isBlocked = blocked; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
    public User getValidatedBy() { return validatedBy; }
    public void setValidatedBy(User validatedBy) { this.validatedBy = validatedBy; }
    public java.time.LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(java.time.LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
    public boolean isValidated() { return validatedAt != null; }
    public long getLockVersion() { return lockVersion; }
}
