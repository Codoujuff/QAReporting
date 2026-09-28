package com.qareporting.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project extends Timestamped {

    public enum Status { active, inactive, archived }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Verrou optimiste : une modification faite sur une version périmée est refusée au lieu d'écraser celle d'un collègue. */
    @Version
    @Column(name = "lock_version", nullable = false)
    @jakarta.json.bind.annotation.JsonbTransient
    private long lockVersion;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT") // texte long (64 Ko)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.active;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    /**
     * Testeurs affectés au projet, quelle que soit leur équipe : un QA peut travailler sur
     * plusieurs projets. L'équipe du projet (team) reste celle de son QA Lead.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "project_members",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @jakarta.json.bind.annotation.JsonbTransient
    private java.util.Set<User> members = new java.util.HashSet<>();

    /** Membres demandés par un appel API (PUT/POST /api/projects), appliqués par la ressource. */
    @Transient
    private java.util.List<Long> requestedMemberIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public java.util.Set<User> getMembers() { return members; }
    public void setMembers(java.util.Set<User> members) { this.members = members; }

    /** Pour l'API : identifiants des membres (lecture), membres demandés (écriture). */
    public java.util.List<Long> getMemberIds() {
        return members.stream().map(User::getId).sorted().toList();
    }
    public void setMemberIds(java.util.List<Long> ids) { this.requestedMemberIds = ids; }
    @jakarta.json.bind.annotation.JsonbTransient
    public java.util.List<Long> getRequestedMemberIds() { return requestedMemberIds; }

    public boolean hasMember(User user) {
        return user != null && members.stream().anyMatch(m -> m.getId().equals(user.getId()));
    }
    public long getLockVersion() { return lockVersion; }
}
