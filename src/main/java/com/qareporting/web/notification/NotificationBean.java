package com.qareporting.web.notification;

import com.qareporting.entity.Notification;
import com.qareporting.web.i18n.I18n;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /*
     * Les notifications sont stockées en base sous forme de texte français (partagé avec
     * l'API REST). Pour les types générés par l'application, on retrouve le sujet
     * (titre de l'anomalie, nom de la campagne) dans le texte et on reconstruit
     * titre et message dans la langue de l'utilisateur ; sinon, on affiche le texte stocké.
     */
    private static final Pattern DEFECT_ASSIGNED = Pattern.compile("(?s)(.+) vous a été assignée\\.");
    private static final Pattern STATUS_CHANGED = Pattern.compile("(?s)(.+) est passée à \"[^\"]*\"\\.");

    public String title(Notification n) {
        String label = n.getType() == null ? null : I18n.find("notif.type." + n.getType() + ".title");
        return label != null ? label : n.getTitle();
    }

    public String message(Notification n) {
        String type = n.getType();
        String stored = n.getMessage();
        if (type == null || stored == null) {
            return stored;
        }
        Matcher m = ("defect_assigned".equals(type) ? DEFECT_ASSIGNED : STATUS_CHANGED).matcher(stored);
        String key = "notif.type." + type + ".message";
        if (m.matches() && I18n.find(key) != null) {
            return I18n.t(key, m.group(1));
        }
        return stored;
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
            return I18n.t("time.minutesAgo", minutes);
        }
        long hours = elapsed.toHours();
        if (hours < 24) {
            return I18n.t("time.hoursAgo", hours);
        }
        long days = elapsed.toDays();
        if (days == 1) {
            return I18n.t("time.yesterday");
        }
        if (days < 7) {
            return I18n.t("time.daysAgo", days);
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
