package com.qareporting.security;

import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * Vérification des identifiants, commune à la page de connexion et à POST /api/login.
 * L'ordre compte pour ne rien révéler à un attaquant :
 * 1. compte ou adresse IP bloqués après trop d'échecs → refus sans rien vérifier ;
 * 2. mot de passe vérifié même si l'adresse e-mail n'existe pas (hachage factice), pour
 *    que le temps de réponse ne trahisse pas l'existence du compte ;
 * 3. « compte désactivé » n'est dit qu'à qui a donné le bon mot de passe.
 */
@ApplicationScoped
public class LoginService {

    public enum Status { OK, INVALID, LOCKED, DISABLED }

    public record Result(Status status, User user, long minutesLocked) {
        public boolean ok() {
            return status == Status.OK;
        }
    }

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    LoginThrottle throttle;

    private volatile String dummyHash;

    public Result authenticate(String email, String password, String ip) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return new Result(Status.INVALID, null, 0);
        }
        long locked = throttle.minutesLocked(email, ip);
        if (locked > 0) {
            return new Result(Status.LOCKED, null, locked);
        }

        List<User> found = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                .setParameter("email", email.strip().toLowerCase())
                .getResultList();
        User user = found.isEmpty() ? null : found.get(0);
        boolean passwordOk = passwordHasher.matches(password, user != null ? user.getPasswordHash() : dummyHash());

        if (user == null || !passwordOk) {
            throttle.recordFailure(email, ip);
            return new Result(Status.INVALID, null, 0);
        }
        if (!user.isActive()) {
            return new Result(Status.DISABLED, null, 0);
        }
        throttle.recordSuccess(email);
        return new Result(Status.OK, user, 0);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordHasher.hash("mot-de-passe-factice-pour-egaliser-le-temps");
        }
        return dummyHash;
    }
}
