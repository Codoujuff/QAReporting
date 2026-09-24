package com.qareporting.web.notification;

import com.qareporting.entity.Notification;
import com.qareporting.entity.User;
import com.qareporting.service.NotificationService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Consomme le NotificationService existant (déjà alimenté par
 * DefectService.assign() et CampaignService sur changement de statut) —
 * aucun ajout backend nécessaire ici, uniquement l'écran JSF.
 */
@Named
@ViewScoped
public class NotificationBean implements Serializable {

    @Inject
    private NotificationService notificationService;

    @Inject
    private SessionAuth sessionAuth;

    private List<Notification> notifications;
    private long unreadCount;

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        User viewer = sessionAuth.getCurrentUser();
        notifications = notificationService.listFor(viewer);
        unreadCount = notifications.stream().filter(n -> n.getReadAt() == null).count();
    }

    public boolean isUnread(Notification notification) {
        return notification.getReadAt() == null;
    }

    /** "Il y a 10 min" / "Hier" / "Il y a 3 jours" / date absolue au-delà d'une semaine. */
    public String relativeTime(Notification notification) {
        LocalDateTime createdAt = notification.getCreatedAt();
        if (createdAt == null) {
            return "";
        }
        Duration elapsed = Duration.between(createdAt, LocalDateTime.now());
        long minutes = elapsed.toMinutes();
        if (minutes < 1) {
            return "À l'instant";
        }
        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }
        long hours = elapsed.toHours();
        if (hours < 24) {
            return "Il y a " + hours + " h";
        }
        long days = elapsed.toDays();
        if (days == 1) {
            return "Hier";
        }
        if (days < 7) {
            return "Il y a " + days + " jours";
        }
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String markRead(Long id) {
        notificationService.markRead(id, sessionAuth.getCurrentUser());
        return "notifications.xhtml?faces-redirect=true";
    }

    public String markAllRead() {
        notificationService.markAllRead(sessionAuth.getCurrentUser());
        return "notifications.xhtml?faces-redirect=true";
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
