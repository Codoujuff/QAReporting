package com.qareporting.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "tests")
public class Test extends Timestamped {

    public enum Status { passed, failed, blocked, not_run }

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

    @Column(columnDefinition = "TEXT") // texte long (64 Ko)
    private String preconditions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "longtext") // JSON : MariaDB le stocke en LONGTEXT (JSON n'en est qu'un alias)
    private List<String> steps;

    @Column(name = "expected_result", columnDefinition = "TEXT") // texte long (64 Ko)
    private String expectedResult;

    @Column(nullable = false)
    private String type = "Test fonctionnel";

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "environment_id")
    private Environment environment;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.not_run;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPreconditions() { return preconditions; }
    public void setPreconditions(String preconditions) { this.preconditions = preconditions; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public long getLockVersion() { return lockVersion; }
}
