package com.qareporting.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "defects")
public class Defect extends Timestamped {

    public enum Severity { critical, high, medium, low }
    public enum Priority { p1, p2, p3, p4 }
    public enum Status { open, in_progress, fixed, retest, closed, reopened }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Verrou optimiste : une modification faite sur une version périmée est refusée au lieu d'écraser celle d'un collègue. */
    @Version
    @Column(name = "lock_version", nullable = false)
    @jakarta.json.bind.annotation.JsonbTransient
    private long lockVersion;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT") // texte long (64 Ko)
    private String description;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private Test test;

    @ManyToOne
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.medium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.p3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.open;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "expected_result", columnDefinition = "TEXT") // texte long (64 Ko)
    private String expectedResult;

    @Column(name = "actual_result", columnDefinition = "TEXT") // texte long (64 Ko)
    private String actualResult;

    @Column(name = "reproduction_steps", columnDefinition = "TEXT") // texte long (64 Ko)
    private String reproductionSteps;

    private String browser;
    private String os;
    private String version;
    private String device;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public String getReproductionSteps() { return reproductionSteps; }
    public void setReproductionSteps(String reproductionSteps) { this.reproductionSteps = reproductionSteps; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public long getLockVersion() { return lockVersion; }
}
