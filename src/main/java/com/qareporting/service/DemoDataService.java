package com.qareporting.service;

import com.qareporting.entity.Activity;
import com.qareporting.entity.Campaign;
import com.qareporting.entity.Defect;
import com.qareporting.entity.DefectHistory;
import com.qareporting.entity.Environment;
import com.qareporting.entity.Project;
import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.Test;
import com.qareporting.entity.TestExecution;
import com.qareporting.entity.User;
import com.qareporting.security.CurrentUser;
import com.qareporting.security.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jeu de données de démonstration (écran admin « Paramètres généraux ») : une équipe
 * Paiement de cinq personnes, deux projets, trois campagnes à des stades différents,
 * une quinzaine de tests avec leur historique d'exécution, sept anomalies couvrant tout
 * le cycle de vie et deux semaines d'activité. Tout passe par les services métier, pour
 * que l'historique, les notifications et le journal d'audit soient cohérents — chaque
 * action est attribuée à la personne qui l'aurait faite. Dates relatives au jour du
 * chargement, pour que la démo « tombe juste » quel que soit le jour de la présentation.
 * Idempotent : ne fait rien si le projet « Boutique en ligne » existe déjà.
 */
@ApplicationScoped
public class DemoDataService {

    public static final String PASSWORD = "demo2026";
    public static final String DOMAIN = "@demo.qa-reporting.local";
    private static final String MARKER_PROJECT = "Boutique en ligne";

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject TestService testService;
    @Inject DefectService defectService;
    @Inject CampaignService campaignService;
    @Inject ActivityService activityService;
    @Inject PasswordHasher passwordHasher;
    @Inject CurrentUser currentUser;

    private final LocalDate today = LocalDate.now();

    public boolean isLoaded() {
        return em.createQuery("SELECT COUNT(p) FROM Project p WHERE p.name = :n", Long.class)
                .setParameter("n", MARKER_PROJECT).getSingleResult() > 0;
    }

    @Transactional
    public boolean load() {
        if (isLoaded()) {
            return false;
        }
        User caller = currentUser.get();
        try {
            build();
        } finally {
            currentUser.set(caller);
        }
        return true;
    }

    private void build() {
        // ---------------- équipe et personnes ----------------
        Team team = new Team();
        team.setName("Équipe Paiement");
        team.setDescription("Tests du tunnel d'achat : panier, paiement, remboursements.");
        em.persist(team);

        User fatou = user("Fatou Sarr", "fatou.sarr", Role.QA_LEAD, team);
        User awa = user("Awa Ndiaye", "awa.ndiaye", Role.QA, team);
        User moussa = user("Moussa Fall", "moussa.fall", Role.QA, team);
        User khadija = user("Khadija Ba", "khadija.ba", Role.QA, team);
        user("Ibrahima Diallo", "ibrahima.diallo", Role.MANAGER, null);
        awa.setReminderTime(LocalTime.of(17, 30));
        team.setLead(fatou);

        Environment qaEnv = environment("QA", "Environnement de qualification");
        Environment preprod = environment("PREPROD", "Environnement de préproduction");

        Project shop = project(MARKER_PROJECT, "Site e-commerce : catalogue, panier et paiement.", team);
        Project mobile = project("Application mobile", "Application Android / iOS de la boutique.", team);
        // Affectations : un testeur peut être sur plusieurs projets.
        shop.getMembers().addAll(java.util.List.of(fatou, awa, moussa, khadija));
        mobile.getMembers().addAll(java.util.List.of(awa, khadija));

        // ---------------- campagnes ----------------
        currentUser.set(fatou);
        Campaign previous = campaign("Régression v2.2", shop, "2.2.0", preprod, fatou,
                today.minusDays(35), today.minusDays(21));
        Campaign current = campaign("Recette v2.3 – Paiement", shop, "2.3.0", qaEnv, fatou,
                today.minusDays(14), today.plusDays(4));
        Campaign upcoming = campaign("Recette mobile v1.0", mobile, "1.0.0", qaEnv, fatou,
                today.plusDays(3), today.plusDays(17));
        campaignService.changeStatus(previous, Campaign.Status.in_progress);
        campaignService.changeStatus(current, Campaign.Status.in_progress);

        // ---------------- campagne précédente (terminée) ----------------
        Test login = test(previous, awa, "Connexion par e-mail et mot de passe", "Un compte client actif existe.",
                List.of("Ouvrir la page de connexion", "Saisir l'e-mail et le mot de passe", "Valider"),
                "Le client arrive sur son espace, son prénom est affiché.");
        Test forgot = test(previous, moussa, "Mot de passe oublié", "Le compte a une adresse e-mail valide.",
                List.of("Cliquer sur « Mot de passe oublié »", "Saisir l'e-mail", "Ouvrir le lien reçu", "Choisir un nouveau mot de passe"),
                "Le nouveau mot de passe permet de se connecter.");
        Test search = test(previous, khadija, "Recherche d'un produit par son nom", "Le catalogue contient « Thé à la menthe ».",
                List.of("Saisir « the menthe » dans la recherche", "Valider"),
                "Le produit « Thé à la menthe » apparaît en premier résultat.");
        run(login, Test.Status.passed, null, awa, preprod, 26);
        run(forgot, Test.Status.passed, null, moussa, preprod, 25);
        run(search, Test.Status.failed, "Aucun résultat : la recherche ne tient pas compte des accents.", khadija, preprod, 25);
        Defect accents = defect(search, khadija, "Recherche : les accents ne sont pas ignorés",
                "« the menthe » ne trouve pas « Thé à la menthe ».", Defect.Severity.medium, Defect.Priority.p3, 25);
        assign(accents, khadija, fatou);
        currentUser.set(khadija);
        defectService.markFixed(accents, khadija, "Correctif livré en 2.2.1 (recherche normalisée).");
        run(search, Test.Status.passed, "Le produit est trouvé.", khadija, preprod, 22);
        defectService.retest(accents, true, khadija, "Vérifié sur PREPROD, build 2.2.1.");
        backdateHistory(accents, 25, 23, 22);
        currentUser.set(fatou);
        campaignService.changeStatus(previous, Campaign.Status.completed);

        // ---------------- campagne en cours ----------------
        Test visa = test(current, awa, "Paiement par carte Visa valide", "Panier de 2 articles, carte de test Visa valide.",
                List.of("Ouvrir le panier", "Cliquer sur « Payer »", "Saisir la carte de test", "Valider le paiement"),
                "La commande est confirmée et un e-mail de confirmation est envoyé.");
        Test expired = test(current, awa, "Paiement refusé : carte expirée", "Carte de test expirée (12/2023).",
                List.of("Ouvrir le panier", "Payer avec la carte expirée", "Valider"),
                "Un message clair indique que la carte est expirée ; le panier est conservé.");
        Test secure = test(current, moussa, "3-D Secure : validation par code SMS", "Carte de test soumise à 3-D Secure.",
                List.of("Payer avec la carte 3-D Secure", "Saisir le code reçu par SMS", "Valider"),
                "Le paiement est accepté après saisie du bon code.");
        Test refund = test(current, moussa, "Remboursement partiel depuis le back-office", "Commande payée de 45 000 FCFA.",
                List.of("Ouvrir la commande dans le back-office", "Cliquer sur « Rembourser »", "Saisir 15 000 FCFA", "Confirmer"),
                "Le client est remboursé de 15 000 FCFA et reçoit un e-mail.");
        Test promo = test(current, khadija, "Code promo cumulé avec les soldes", "Article soldé à -20 %, code SOLDES10.",
                List.of("Ajouter l'article soldé au panier", "Saisir le code SOLDES10", "Aller au paiement"),
                "La réduction du code s'applique une seule fois, sur le prix soldé.");
        Test cart = test(current, khadija, "Panier conservé après reconnexion", "Client connecté avec 3 articles au panier.",
                List.of("Se déconnecter", "Se reconnecter"), "Les 3 articles sont toujours dans le panier.");
        Test orange = test(current, awa, "Paiement Orange Money", "Compte Orange Money de test approvisionné.",
                List.of("Choisir Orange Money", "Saisir le numéro", "Valider sur le téléphone"),
                "Le paiement est confirmé en moins de 30 secondes.");
        Test invoice = test(current, moussa, "Facture PDF envoyée par e-mail", "Commande confirmée.",
                List.of("Ouvrir l'e-mail de confirmation", "Télécharger la facture"),
                "La facture PDF contient le détail, la TVA et l'adresse de livraison.");
        test(current, khadija, "Frais de livraison par zone", "Zones Dakar et régions configurées.",
                List.of("Saisir une adresse à Dakar", "Saisir une adresse à Thiès", "Comparer les frais"),
                "Les frais correspondent à la grille de chaque zone.");
        test(current, awa, "Paiement Wave", "Compte Wave de test.",
                List.of("Choisir Wave", "Scanner le QR code", "Valider"), "Le paiement est confirmé.");
        Test label = test(current, moussa, "Bouton « Payer » sur petit écran", "Navigateur réglé en 360 px de large.",
                List.of("Ouvrir le panier sur mobile"), "Le libellé du bouton est entièrement lisible.");

        run(visa, Test.Status.passed, null, awa, qaEnv, 9);
        run(expired, Test.Status.failed, "Écran blanc après validation, aucun message.", awa, qaEnv, 8);
        run(secure, Test.Status.failed, "Le code SMS n'arrive pas.", moussa, qaEnv, 9);
        run(secure, Test.Status.passed, "Code reçu après correction de la passerelle SMS.", moussa, qaEnv, 6);
        run(refund, Test.Status.blocked, "Back-office indisponible sur l'environnement QA.", moussa, qaEnv, 5);
        run(promo, Test.Status.failed, "La réduction est appliquée deux fois : 32 % au lieu de 28 %.", khadija, qaEnv, 7);
        run(cart, Test.Status.passed, null, khadija, qaEnv, 6);
        run(orange, Test.Status.failed, "Au-delà de 30 s, la page reste bloquée sur « En attente ».", awa, qaEnv, 7);
        run(invoice, Test.Status.passed, null, moussa, qaEnv, 4);
        run(label, Test.Status.failed, "Le libellé est coupé : « Pay… ».", moussa, qaEnv, 6);

        // ---------------- anomalies : une par étape du cycle de vie ----------------
        Defect whiteScreen = defect(expired, awa, "Carte expirée : écran blanc au lieu du message d'erreur",
                "Après validation d'une carte expirée, la page reste blanche.", Defect.Severity.high, Defect.Priority.p2, 8);
        assign(whiteScreen, awa, fatou);                                              // ouverte
        backdateHistory(whiteScreen, 8, 8);

        Defect doublePromo = defect(promo, khadija, "Code promo appliqué deux fois pendant les soldes",
                "Le code SOLDES10 s'ajoute à la remise des soldes au lieu de s'appliquer au prix soldé.",
                Defect.Severity.critical, Defect.Priority.p1, 7);
        assign(doublePromo, khadija, fatou);
        currentUser.set(khadija);
        defectService.startProgress(doublePromo, khadija, "Pris en charge par l'équipe back-end (ticket PAY-231).");
        backdateHistory(doublePromo, 7, 7, 6);                                        // en cours

        Defect timeout = defect(orange, awa, "Orange Money : délai d'attente dépassé non géré",
                "Passé 30 secondes sans réponse de l'opérateur, aucun message ni nouvel essai.",
                Defect.Severity.high, Defect.Priority.p2, 7);
        assign(timeout, awa, fatou);
        currentUser.set(awa);
        defectService.markFixed(timeout, awa, "Correctif déployé sur QA (build 2.3.0-rc4).");
        backdateHistory(timeout, 7, 7, 2);                                            // corrigée, à revérifier

        Defect truncated = defect(label, moussa, "Libellé « Payer » tronqué sur petit écran",
                "Sous 380 px de large, le bouton affiche « Pay… ».", Defect.Severity.low, Defect.Priority.p4, 6);
        assign(truncated, moussa, fatou);
        currentUser.set(moussa);
        defectService.markFixed(truncated, moussa, "CSS corrigé.");
        defectService.startRetest(truncated, moussa, null);
        backdateHistory(truncated, 6, 6, 3, 1);                                       // revérification en cours

        Defect refundButton = defect(refund, moussa, "Remboursement : bouton inactif sans explication",
                "Le bouton « Rembourser » est grisé sans message quand le montant dépasse le reste à rembourser.",
                Defect.Severity.medium, Defect.Priority.p3, 5);
        assign(refundButton, moussa, fatou);
        currentUser.set(moussa);
        defectService.markFixed(refundButton, moussa, "Message ajouté.");
        defectService.retest(refundButton, false, moussa, "Le message n'apparaît qu'en anglais.");
        backdateHistory(refundButton, 5, 5, 3, 2);                                    // rouverte

        Defect duplicate = defect(expired, moussa, "Carte expirée : page blanche",
                "Même symptôme que l'anomalie sur la carte expirée.", Defect.Severity.low, Defect.Priority.p4, 4);
        currentUser.set(fatou);
        defectService.close(duplicate, fatou, "Doublon de « Carte expirée : écran blanc au lieu du message d'erreur ».");
        backdateHistory(duplicate, 4, 4);                                             // fermée sans revérification

        // ---------------- activité : deux semaines de déclarations ----------------
        Random random = new Random(2026);
        List<LocalDate> days = workingDays(10);
        for (User tester : List.of(awa, moussa, khadija)) {
            for (LocalDate day : days) {
                if (day.equals(today) && tester == khadija) {
                    continue; // une testeuse n'a pas encore saisi aujourd'hui : le rappel a un sens
                }
                Activity a = activity(tester, current, qaEnv, day, random);
                if (day.isBefore(today.minusDays(2))) {
                    currentUser.set(fatou);
                    activityService.validateByLead(a, fatou);
                }
            }
        }

        // La campagne mobile reste « planifiée » : ses tests ne peuvent pas encore être exécutés.
        test(upcoming, awa, "Installation sur Android 12", "Téléphone Android 12 sans l'application.",
                List.of("Installer l'APK de test", "Ouvrir l'application"), "L'écran d'accueil s'affiche en moins de 3 secondes.");
        test(upcoming, khadija, "Notification push de commande", "Application installée, notifications autorisées.",
                List.of("Passer une commande depuis le site"), "Une notification arrive sur le téléphone.");
        currentUser.set(null);
    }

    // =============================== briques ===============================

    private User user(String name, String login, String roleName, Team team) {
        User u = new User();
        u.setName(name);
        String[] parts = name.split(" ");
        u.setInitials(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
        u.setEmail(login + DOMAIN);
        u.setPasswordHash(passwordHasher.hash(PASSWORD));
        u.setRole(em.createQuery("SELECT r FROM Role r WHERE r.name = :n", Role.class)
                .setParameter("n", roleName).getSingleResult());
        u.setTeam(team);
        u.setActive(true);
        em.persist(u);
        return u;
    }

    private Environment environment(String name, String description) {
        List<Environment> found = em.createQuery("SELECT e FROM Environment e WHERE e.name = :n", Environment.class)
                .setParameter("n", name).getResultList();
        if (!found.isEmpty()) {
            return found.get(0);
        }
        Environment env = new Environment();
        env.setName(name);
        env.setDescription(description);
        env.setStatus(Environment.Status.available);
        em.persist(env);
        return env;
    }

    private Project project(String name, String description, Team team) {
        Project p = new Project();
        p.setName(name);
        p.setDescription(description);
        p.setStatus(Project.Status.active);
        p.setTeam(team);
        em.persist(p);
        return p;
    }

    private Campaign campaign(String name, Project project, String version, Environment env, User owner,
                              LocalDate start, LocalDate end) {
        Campaign c = new Campaign();
        c.setName(name);
        c.setProject(project);
        c.setVersion(version);
        c.setEnvironment(env);
        c.setResponsible(owner);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setStatus(Campaign.Status.planned);
        return campaignService.create(c);
    }

    private Test test(Campaign campaign, User assignee, String title, String preconditions,
                      List<String> steps, String expected) {
        Test t = new Test();
        t.setTitle(title);
        t.setPreconditions(preconditions);
        t.setSteps(new ArrayList<>(steps));
        t.setExpectedResult(expected);
        t.setProject(campaign.getProject());
        t.setCampaign(campaign);
        t.setEnvironment(campaign.getEnvironment());
        t.setAssignedTo(assignee);
        return testService.create(t);
    }

    /** Exécution datée d'il y a {@code daysAgo} jours, en milieu de matinée. */
    private void run(Test test, Test.Status status, String actual, User tester, Environment env, int daysAgo) {
        currentUser.set(tester);
        TestExecution e = testService.execute(test, status, actual, tester, env, "15 min");
        em.flush();
        em.createQuery("UPDATE TestExecution e SET e.executedAt = :t WHERE e.id = :id")
                .setParameter("t", at(daysAgo, 10)).setParameter("id", e.getId()).executeUpdate();
    }

    private Defect defect(Test source, User reporter, String title, String description,
                          Defect.Severity severity, Defect.Priority priority, int daysAgo) {
        currentUser.set(reporter);
        Defect d = new Defect();
        d.setTitle(title);
        d.setDescription(description);
        d.setProject(source.getProject());
        d.setCampaign(source.getCampaign());
        d.setTest(source);
        d.setEnvironment(source.getEnvironment());
        d.setSeverity(severity);
        d.setPriority(priority);
        d.setExpectedResult(source.getExpectedResult());
        d.setReproductionSteps(String.join("\n", source.getSteps()));
        defectService.createWithHistory(d, reporter);
        em.flush();
        em.createQuery("UPDATE Defect d SET d.createdAt = :t, d.updatedAt = :t WHERE d.id = :id")
                .setParameter("t", at(daysAgo, 11)).setParameter("id", d.getId()).executeUpdate();
        return d;
    }

    private void assign(Defect defect, User assignee, User lead) {
        currentUser.set(lead);
        defectService.assign(defect, assignee, lead);
    }

    /** Date les lignes d'historique de l'anomalie, dans l'ordre, aux jours indiqués (il y a N jours). */
    private void backdateHistory(Defect defect, int... daysAgo) {
        em.flush();
        List<DefectHistory> rows = em.createQuery(
                        "SELECT h FROM DefectHistory h WHERE h.defect = :d ORDER BY h.id", DefectHistory.class)
                .setParameter("d", defect).getResultList();
        for (int i = 0; i < rows.size(); i++) {
            int ago = daysAgo[Math.min(i, daysAgo.length - 1)];
            em.createQuery("UPDATE DefectHistory h SET h.createdAt = :t WHERE h.id = :id")
                    .setParameter("t", at(ago, 11 + i)).setParameter("id", rows.get(i).getId()).executeUpdate();
        }
    }

    private Activity activity(User tester, Campaign campaign, Environment env, LocalDate day, Random random) {
        int executed = 6 + random.nextInt(9);
        int failed = random.nextInt(3);
        int blocked = random.nextInt(5) == 0 ? 1 : 0;
        int notRun = random.nextInt(2);
        int passed = Math.max(0, executed - failed - blocked - notRun);
        executed = passed + failed + blocked + notRun;
        Activity a = new Activity();
        a.setUser(tester);
        a.setProject(campaign.getProject());
        a.setCampaign(campaign);
        a.setEnvironment(env);
        a.setActivityType(random.nextInt(4) == 0 ? "Analyse d'anomalies" : "Exécution de tests");
        a.setTestsExecuted(executed);
        a.setPassed(passed);
        a.setFailed(failed);
        a.setBlocked(blocked);
        a.setNotRun(notRun);
        a.setDefectsCount(failed > 0 ? random.nextInt(failed + 1) : 0);
        a.setBlockedReason(blocked > 0 ? "Environnement QA indisponible une partie de la journée." : null);
        a.setDuration("7 h");
        a.setActivityDate(day);
        currentUser.set(tester);
        return activityService.createValidated(a);
    }

    /** Les {@code count} derniers jours ouvrés, aujourd'hui compris s'il est ouvré. */
    private List<LocalDate> workingDays(int count) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = today; days.size() < count; d = d.minusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(0, d);
            }
        }
        return days;
    }

    private LocalDateTime at(int daysAgo, int hour) {
        return today.minusDays(daysAgo).atTime(Math.min(hour, 18), 0);
    }
}
