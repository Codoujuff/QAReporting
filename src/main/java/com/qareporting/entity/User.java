package com.qareporting.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "users")
public class User extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String initials;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonbTransient
    @Column(name = "password", nullable = false)
    private String passwordHash;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    /**
     * @ColumnDefault donne un vrai DEFAULT SQL — sans lui, un ALTER TABLE sur une base
     * existante (comme ici) initialise les lignes déjà présentes à false, et ignore
     * complètement l'initialiseur Java "= true" qui ne s'applique qu'aux nouveaux objets.
     */
    @ColumnDefault("1")
    @Column(name = "notify_assigned", nullable = false)
    private boolean notifyAssigned = true;

    @ColumnDefault("1")
    @Column(name = "notify_fixed", nullable = false)
    private boolean notifyFixed = true;

    @ColumnDefault("1")
    @Column(name = "notify_campaign", nullable = false)
    private boolean notifyCampaign = true;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }
    public boolean isNotifyAssigned() { return notifyAssigned; }
    public void setNotifyAssigned(boolean notifyAssigned) { this.notifyAssigned = notifyAssigned; }
    public boolean isNotifyFixed() { return notifyFixed; }
    public void setNotifyFixed(boolean notifyFixed) { this.notifyFixed = notifyFixed; }
    public boolean isNotifyCampaign() { return notifyCampaign; }
    public void setNotifyCampaign(boolean notifyCampaign) { this.notifyCampaign = notifyCampaign; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }
}
