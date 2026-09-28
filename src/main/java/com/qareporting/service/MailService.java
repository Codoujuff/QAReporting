package com.qareporting.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Envoi d'e-mails par SMTP, configuré par variables d'environnement :
 * QA_SMTP_HOST, QA_SMTP_PORT (587), QA_SMTP_USER, QA_SMTP_PASSWORD, QA_SMTP_FROM,
 * QA_SMTP_TLS (true). Sans QA_SMTP_HOST (poste de développement, démonstration), rien
 * n'est envoyé : l'e-mail est écrit dans le journal du serveur, pour pouvoir tester.
 */
@ApplicationScoped
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    public boolean isConfigured() {
        return env("QA_SMTP_HOST", null) != null;
    }

    /** Envoie un e-mail texte ; renvoie false (et journalise) en cas d'échec, sans lever d'exception. */
    public boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            LOG.info(() -> "\n===== E-mail NON envoyé (QA_SMTP_HOST non défini) =====\nÀ : " + to
                    + "\nSujet : " + subject + "\n\n" + body + "\n=====================================================");
            return true;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", env("QA_SMTP_HOST", null));
        props.put("mail.smtp.port", env("QA_SMTP_PORT", "587"));
        props.put("mail.smtp.starttls.enable", env("QA_SMTP_TLS", "true"));
        props.put("mail.smtp.starttls.required", env("QA_SMTP_TLS", "true"));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        String user = env("QA_SMTP_USER", null);
        Authenticator auth = null;
        if (user != null) {
            props.put("mail.smtp.auth", "true");
            String password = env("QA_SMTP_PASSWORD", "");
            auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            };
        }
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(props, auth));
            message.setFrom(new InternetAddress(env("QA_SMTP_FROM", user != null ? user : "no-reply@qa-reporting.local")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(body, StandardCharsets.UTF_8.name());
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            LOG.warning("Échec de l'envoi de l'e-mail à " + to + " : " + e.getMessage());
            return false;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
