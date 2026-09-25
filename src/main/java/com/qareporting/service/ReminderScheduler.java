package com.qareporting.service;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Déclencheur du rappel quotidien : un timer EJB non persistant, chaque minute.
 * Une erreur est journalisée sans jamais arrêter le timer.
 */
@Singleton
@Startup
public class ReminderScheduler {

    private static final Logger LOG = Logger.getLogger(ReminderScheduler.class.getName());

    @Inject
    ReminderService reminderService;

    @Schedule(hour = "*", minute = "*", persistent = false)
    public void tick() {
        try {
            int sent = reminderService.sendDueReminders(LocalDateTime.now());
            if (sent > 0) {
                LOG.info(() -> "Rappels de saisie envoyés : " + sent);
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Échec de l'envoi des rappels de saisie", e);
        }
    }
}
