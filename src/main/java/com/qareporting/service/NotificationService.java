package com.qareporting.service;

import com.qareporting.entity.Notification;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Creates Notification rows — the Jakarta EE equivalent of the Laravel
 * version's auto-notify-team-on-campaign-status-change and
 * notify-on-defect-assignment behaviour.
 */
@ApplicationScoped
public class NotificationService {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    public List<Notification> listFor(User user) {
        return em.createQuery(
                        "SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC",
                        Notification.class)
                .setParameter("user", user)
                .getResultList();
    }

    @Transactional
    public boolean markRead(Long notificationId, User user) {
        Notification notification = em.find(Notification.class, notificationId);
        if (notification == null || !notification.getUser().getId().equals(user.getId())) {
            return false;
        }
        notification.setReadAt(LocalDateTime.now());
        em.merge(notification);
        return true;
    }

    @Transactional
    public void markAllRead(User user) {
        em.createQuery("UPDATE Notification n SET n.readAt = :now WHERE n.user = :user AND n.readAt IS NULL")
                .setParameter("now", LocalDateTime.now())
                .setParameter("user", user)
                .executeUpdate();
    }

    @Transactional
    public void notify(User user, String type, String title, String message) {
        if (!isEnabledFor(user, type)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        em.persist(notification);
    }

    /** Respecte les préférences de notification de chaque utilisateur (écran Paramètres). */
    private boolean isEnabledFor(User user, String type) {
        return switch (type) {
            case "defect_assigned" -> user.isNotifyAssigned();
            case "defect_fixed" -> user.isNotifyFixed();
            default -> !type.startsWith("campaign_") || user.isNotifyCampaign();
        };
    }

    @Transactional
    public void notifyTeam(Team team, String type, String title, String message) {
        if (team == null) {
            return;
        }
        List<User> members = em.createQuery(
                        "SELECT u FROM User u WHERE u.team = :team AND u.active = true", User.class)
                .setParameter("team", team)
                .getResultList();
        for (User member : members) {
            notify(member, type, title, message);
        }
    }
}
