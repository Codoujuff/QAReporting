package com.qareporting.security;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freine le « bourrage » de mots de passe : après 5 échecs en 15 minutes sur un même
 * compte, ou 20 depuis une même adresse IP, les connexions sont refusées pendant
 * 15 minutes. Partagé par la page de connexion et par POST /api/login.
 * En mémoire : suffisant pour un serveur unique (un cluster demanderait un cache partagé).
 */
@ApplicationScoped
public class LoginThrottle {

    static final int MAX_PER_ACCOUNT = 5;
    static final int MAX_PER_IP = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    /** Pour les tests. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /** Minutes de blocage restantes (arrondies au-dessus), 0 si la connexion est permise. */
    public long minutesLocked(String email, String ip) {
        Instant now = clock.instant();
        long seconds = Math.max(remaining(accountKey(email), now), remaining(ipKey(ip), now));
        return seconds == 0 ? 0 : (seconds + 59) / 60;
    }

    public void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        count(accountKey(email), MAX_PER_ACCOUNT, now);
        count(ipKey(ip), MAX_PER_IP, now);
    }

    /** Une connexion réussie efface les échecs du compte (pas ceux de l'IP). */
    public void recordSuccess(String email) {
        failures.remove(accountKey(email));
        lockedUntil.remove(accountKey(email));
    }

    private void count(String key, int max, Instant now) {
        if (key == null) {
            return;
        }
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (recent) {
            recent.addLast(now);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) {
                recent.pollFirst();
            }
            if (recent.size() >= max) {
                lockedUntil.put(key, now.plus(LOCK));
                recent.clear();
            }
        }
    }

    private long remaining(String key, Instant now) {
        if (key == null) {
            return 0;
        }
        Instant until = lockedUntil.get(key);
        if (until == null || !until.isAfter(now)) {
            lockedUntil.remove(key);
            return 0;
        }
        return Duration.between(now, until).getSeconds();
    }

    private static String accountKey(String email) {
        return email == null || email.isBlank() ? null : "account:" + email.strip().toLowerCase(Locale.ROOT);
    }

    private static String ipKey(String ip) {
        return ip == null || ip.isBlank() ? null : "ip:" + ip;
    }
}
