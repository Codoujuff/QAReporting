package com.qareporting.service;

import com.qareporting.entity.PasswordResetToken;
import com.qareporting.entity.User;
import com.qareporting.security.LoginThrottle;
import com.qareporting.security.PasswordHasher;
import com.qareporting.security.PasswordPolicy;
import com.qareporting.security.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

/**
 * « Mot de passe oublié » : lien à usage unique, valable une heure, envoyé par e-mail.
 * La demande répond toujours la même chose, que le compte existe ou non (rien n'est
 * révélé), et elle est limitée comme les connexions (LoginThrottle, compteurs séparés).
 */
@ApplicationScoped
public class PasswordResetService {

    public static final Duration VALIDITY = Duration.ofHours(1);

    public enum ResetStatus { OK, INVALID_LINK, WEAK_PASSWORD }

    public record ResetResult(ResetStatus status, String errorKey) {
    }

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject TokenService tokenService;
    @Inject PasswordHasher passwordHasher;
    @Inject LoginThrottle throttle;
    @Inject MailService mailService;
    @Inject AuditService audit;

    /**
     * @param baseUrl adresse publique de l'application (…/qa-reporting-j2ee), pour le lien
     * @param mailText (sujet ou corps, lien) → texte traduit dans la langue du demandeur
     */
    @Transactional
    public void request(String email, String ip, String baseUrl, BiFunction<String, String, String> mailText) {
        if (email == null || email.isBlank()) {
            return;
        }
        String key = "reset:" + email.strip().toLowerCase();
        if (throttle.minutesLocked(key, "reset:" + ip) > 0) {
            return;
        }
        throttle.recordFailure(key, "reset:" + ip); // chaque demande compte : 5 par quart d'heure au plus

        List<User> found = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :e AND u.active = true", User.class)
                .setParameter("e", email.strip().toLowerCase())
                .getResultList();
        if (found.isEmpty()) {
            return; // même réponse que si le compte existait
        }
        User user = found.get(0);
        em.createQuery("DELETE FROM PasswordResetToken t WHERE t.user = :u AND t.usedAt IS NULL")
                .setParameter("u", user).executeUpdate();

        String plain = tokenService.generatePlainToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenService.hash(plain));
        token.setExpiresAt(LocalDateTime.now().plus(VALIDITY));
        em.persist(token);

        String link = baseUrl + "/reset-password.xhtml?token=" + plain;
        mailService.send(user.getEmail(), mailText.apply("subject", link), mailText.apply("body", link));
    }

    /** Le lien est-il encore utilisable ? (pour afficher le formulaire ou un message d'expiration) */
    public boolean isValid(String plainToken) {
        return find(plainToken) != null;
    }

    @Transactional
    public ResetResult reset(String plainToken, String newPassword) {
        PasswordResetToken token = find(plainToken);
        if (token == null) {
            return new ResetResult(ResetStatus.INVALID_LINK, "err.resetLinkInvalid");
        }
        User user = token.getUser();
        String weak = PasswordPolicy.check(newPassword, user.getEmail());
        if (weak != null) {
            return new ResetResult(ResetStatus.WEAK_PASSWORD, weak);
        }
        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.setMustChangePassword(false);
        token.setUsedAt(LocalDateTime.now());
        // Toutes les sessions d'API ouvertes avec l'ancien mot de passe tombent.
        em.createQuery("DELETE FROM AccessToken t WHERE t.user = :u").setParameter("u", user).executeUpdate();
        throttle.recordSuccess(user.getEmail());
        audit.record("password_reset", user);
        return new ResetResult(ResetStatus.OK, null);
    }

    private PasswordResetToken find(String plainToken) {
        if (plainToken == null || plainToken.isBlank() || plainToken.length() > 200) {
            return null;
        }
        List<PasswordResetToken> found = em.createQuery(
                        "SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :h", PasswordResetToken.class)
                .setParameter("h", tokenService.hash(plainToken))
                .getResultList();
        if (found.isEmpty()) {
            return null;
        }
        PasswordResetToken token = found.get(0);
        return token.isUsable(LocalDateTime.now()) && token.getUser().isActive() ? token : null;
    }
}
