package com.qareporting.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /**
     * Sans ça, la sérialisation JSON-B boucle : Team.lead (User) -> User.team (Team) ->
     * Team.lead -> ... Le lead reste consultable via son id (getLeadId()) ; l'objet User
     * complet, lui, se retrouve déjà exposé partout où un utilisateur apparaît
     * (listes, assignedTo des tests/anomalies), donc c'est ce lien-ci qu'on coupe.
     */
    @JsonbTransient
    @ManyToOne
    @JoinColumn(name = "lead_id")
    private User lead;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getLead() { return lead; }
    public void setLead(User lead) { this.lead = lead; }

    /** Seul ce qui reste exposé en JSON une fois "lead" rendu @JsonbTransient. */
    public Long getLeadId() { return lead != null ? lead.getId() : null; }
}
