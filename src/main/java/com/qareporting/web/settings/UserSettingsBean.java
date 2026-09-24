package com.qareporting.web.settings;

import com.qareporting.entity.User;
import com.qareporting.security.PasswordHasher;
import com.qareporting.service.UserService;
import com.qareporting.web.auth.SessionAuth;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Écran "Paramètres" : modifier son nom, ses préférences de notification, son
 * rappel quotidien (User.reminderTime) et son mot de passe. L'édition du nom et
 * le changement de mot de passe vivaient auparavant dans "Mon profil" — ce
 * dernier est maintenant une page de lecture seule (statistiques + activité
 * récente), l'édition du compte vit ici, comme dans la maquette.
 */
@Named
@ViewScoped
public class UserSettingsBean implements Serializable {

    private static final LocalTime DEFAULT_REMINDER_TIME = LocalTime.of(9, 0);

    @Inject
    private SessionAuth sessionAuth;

    @Inject
    private UserService userService;

    @Inject
    private PasswordHasher passwordHasher;

    private User user;
    private boolean reminderEnabled;
    private LocalTime reminderTime;
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirmation;

    @PostConstruct
    public void init() {
        user = sessionAuth.getCurrentUser();
        reminderTime = user.getReminderTime();
        reminderEnabled = reminderTime != null;
        if (reminderTime == null) {
            reminderTime = DEFAULT_REMINDER_TIME;
        }
    }

    public String save() {
        if (user.getName() == null || user.getName().isBlank()) {
            addError("Le nom est obligatoire.");
            return null;
        }

        boolean changingPassword = newPassword != null && !newPassword.isBlank();
        if (changingPassword) {
            if (currentPassword == null || currentPassword.isBlank()
                    || !passwordHasher.matches(currentPassword, user.getPasswordHash())) {
                addError("Mot de passe actuel incorrect.");
                return null;
            }
            if (!newPassword.equals(newPasswordConfirmation)) {
                addError("La confirmation ne correspond pas au nouveau mot de passe.");
                return null;
            }
            user.setPasswordHash(passwordHasher.hash(newPassword));
        }

        user.setReminderTime(reminderEnabled ? reminderTime : null);
        userService.update(user);
        sessionAuth.syncDisplayName(user);

        currentPassword = null;
        newPassword = null;
        newPasswordConfirmation = null;
        return "settings.xhtml?faces-redirect=true";
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public User getUser() {
        return user;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirmation() {
        return newPasswordConfirmation;
    }

    public void setNewPasswordConfirmation(String newPasswordConfirmation) {
        this.newPasswordConfirmation = newPasswordConfirmation;
    }
}
