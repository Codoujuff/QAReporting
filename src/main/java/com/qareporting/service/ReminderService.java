package com.qareporting.service;

import com.qareporting.entity.Role;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Rappel quotidien de saisie d'activité (écran Paramètres → « Rappel de saisie ») : à
 * l'heure choisie, un testeur qui n'a encore rien déclaré aujourd'hui reçoit une
 * notification — une seule par jour. Appelé chaque minute par ReminderScheduler.
 */
@ApplicationScoped
public class ReminderService {

    public static final String TYPE = "activity_reminder";

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    ActivityService activityService;

    @Inject
    NotificationService notificationService;

    /** L'heure du rappel est-elle atteinte ? (fonction pure, testée unitairement) */
    public static boolean isDue(LocalTime reminderTime, LocalTime now) {
        return reminderTime != null && !now.isBefore(reminderTime);
    }

    /** Envoie les rappels dus ; renvoie le nombre de notifications créées. */
    @Transactional
    public int sendDueReminders(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<User> candidates = em.createQuery(
                        "SELECT u FROM User u WHERE u.active = true AND u.reminderTime IS NOT NULL "
                                + "AND u.role.name IN :roles", User.class)
                .setParameter("roles", List.of(Role.QA, Role.QA_LEAD))
                .getResultList();
        int sent = 0;
        for (User user : candidates) {
            if (!isDue(user.getReminderTime(), now.toLocalTime())
                    || activityService.hasActivityOn(user, today)
                    || alreadyRemindedOn(user, today)) {
                continue;
            }
            notificationService.notify(user, TYPE, "Rappel de saisie",
                    "Pensez à déclarer votre activité de test du jour.");
            sent++;
        }
        return sent;
    }

    private boolean alreadyRemindedOn(User user, LocalDate day) {
        return em.createQuery("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.type = :type "
                        + "AND n.createdAt >= :start", Long.class)
                .setParameter("user", user)
                .setParameter("type", TYPE)
                .setParameter("start", day.atStartOfDay())
                .getSingleResult() > 0;
    }
}
