package com.qareporting.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "environments")
public class Environment extends Timestamped {

    public enum Status { available, unavailable }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.available;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
