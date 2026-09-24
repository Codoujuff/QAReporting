package com.qareporting;

import com.qareporting.entity.AppSetting;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import com.qareporting.security.PasswordHasher;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Seeds the four roles, the four environments and one admin account on
 * first boot — the minimum needed to log in at all. The Jakarta EE
 * equivalent of Laravel's RoleSeeder + EnvironmentSeeder + the real admin
 * account created for production. Idempotent: does nothing once roles
 * already exist.
 */
@Singleton
@Startup
public class StartupSeeder {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    PasswordHasher passwordHasher;

    @PostConstruct
    @Transactional
    public void seed() {
        Long roleCount = em.createQuery("SELECT COUNT(r) FROM Role r", Long.class).getSingleResult();
        if (roleCount == 0) {
            role(Role.QA, "Saisie de l'activité, tests, anomalies et reporting personnel.");
            role(Role.QA_LEAD, "Suivi de l'équipe, campagnes et anomalies du périmètre.");
            role(Role.MANAGER, "Vision synthétique de l'avancement et des risques.");
            Role admin = role(Role.ADMIN, "Gestion des utilisateurs, projets, équipes et paramètres.");

            environment("DEV", "Environnement de développement", "dev.qa-reporting-j2ee.local", Environment.Status.available);
            environment("QA", "Environnement de qualification", "qa.qa-reporting-j2ee.local", Environment.Status.available);
            environment("PREPROD", "Environnement de préproduction", "preprod.qa-reporting-j2ee.local", Environment.Status.available);
            environment("PROD", "Environnement de production", "prod.qa-reporting-j2ee.local", Environment.Status.unavailable);

            User initialAdmin = new User();
            initialAdmin.setName("Admin");
            initialAdmin.setInitials("AD");
            initialAdmin.setEmail("admin@qa-reporting-j2ee.local");
            initialAdmin.setPasswordHash(passwordHasher.hash("password"));
            initialAdmin.setRole(admin);
            initialAdmin.setActive(true);
            em.persist(initialAdmin);
        }

        // Gate indépendant du précédent : une base déjà en place (rôles déjà seedés
        // avant l'ajout de AppSetting) doit quand même recevoir ces réglages par défaut.
        Long settingCount = em.createQuery("SELECT COUNT(s) FROM AppSetting s", Long.class).getSingleResult();
        if (settingCount == 0) {
            setting("organisation.nom", "QA Reporting", "Nom de l'organisation affiché dans l'application.");
            setting("organisation.email_support", "support@qa-reporting-j2ee.local", "Adresse de contact pour le support.");
            setting("activite.heure_rappel_defaut", "09:00", "Heure de rappel quotidien proposée par défaut aux nouveaux comptes.");
            setting("notifications.retention_jours", "30", "Durée de conservation des notifications lues, en jours.");
        }
    }

    private Role role(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        em.persist(role);
        return role;
    }

    private void environment(String name, String description, String url, Environment.Status status) {
        Environment env = new Environment();
        env.setName(name);
        env.setDescription(description);
        env.setUrl(url);
        env.setStatus(status);
        em.persist(env);
    }

    private void setting(String key, String value, String description) {
        AppSetting setting = new AppSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        em.persist(setting);
    }
}
