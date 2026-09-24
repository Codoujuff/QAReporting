package com.qareporting.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Append-only trail of every status change on a Defect — never edited or deleted. */
@Entity
@Table(name = "defect_histories")
public class DefectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "defect_id", nullable = false)
    private Defect defect;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private Defect.Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private Defect.Status newStatus;

    @Lob
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Defect getDefect() { return defect; }
    public void setDefect(Defect defect) { this.defect = defect; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Defect.Status getOldStatus() { return oldStatus; }
    public void setOldStatus(Defect.Status oldStatus) { this.oldStatus = oldStatus; }
    public Defect.Status getNewStatus() { return newStatus; }
    public void setNewStatus(Defect.Status newStatus) { this.newStatus = newStatus; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
