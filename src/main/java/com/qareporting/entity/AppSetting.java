package com.qareporting.entity;

import jakarta.persistence.*;

/**
 * Réglage global de l'application, en clé/valeur (ex : "organisation.nom" ->
 * "QA Reporting"). Gérable depuis l'écran admin "Paramètres" — la clé est
 * un texte libre choisi par l'administrateur (pas un enum fermé), pour rester
 * extensible sans migration à chaque nouveau réglage.
 */
@Entity
@Table(name = "app_settings")
public class AppSetting extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true)
    private String settingKey;

    @Lob
    @Column(name = "setting_value")
    private String settingValue;

    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
