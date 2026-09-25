package com.qareporting.dto;

/**
 * Separate from the User entity on purpose: passwordHash is @JsonbTransient
 * on the entity (never leaked back in a response), which means it can't be
 * used as the incoming field for a plaintext password either — JSON-B
 * excludes a @JsonbTransient field from both directions. A create/update
 * request needs its own shape with a plain "password" field instead.
 */
public class UserRequest {
    private String name;
    private String initials;
    private String email;
    private String password;
    /** Obligatoire quand on change son propre mot de passe. */
    private String currentPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    private Long roleId;
    private Long teamId;
    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
