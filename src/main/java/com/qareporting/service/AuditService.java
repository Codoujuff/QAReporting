package com.qareporting.service;

import com.qareporting.entity.AuditLog;
import com.qareporting.entity.User;
import com.qareporting.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Journal d'audit (cahier des charges §7.2) : chaque écriture passant par les services
 * ajoute une ligne AuditLog dans la même transaction — si l'action échoue, sa trace
 * disparaît avec elle. L'auteur est l'utilisateur de la requête courante (CurrentUser,
 * alimenté par le filtre REST comme par le filtre des pages JSF) ; hors requête
 * (données de départ au démarrage), l'action est enregistrée sans auteur.
 */
@ApplicationScoped
public class AuditService {

    public static final String CREATED = "created";
    public static final String UPDATED = "updated";
    public static final String DELETED = "deleted";

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    @Inject
    CurrentUser currentUser;

    @Transactional
    public void record(String action, Object entity) {
        record(action, entity, null);
    }

    @Transactional
    public void record(String action, Object entity, Map<String, Object> changes) {
        AuditLog log = new AuditLog();
        log.setUser(actor());
        log.setAction(action);
        log.setModelType(Hibernate.getClass(entity).getSimpleName());
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        log.setModelId(id instanceof Long l ? l : null);
        log.setChanges(changes == null || changes.isEmpty() ? null : changes);
        em.persist(log);
    }

    /** Raccourci pour un changement d'état : {"from": ..., "to": ...}. */
    public static Map<String, Object> transition(Object from, Object to) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("from", from == null ? null : from.toString());
        changes.put("to", to == null ? null : to.toString());
        return changes;
    }

    public List<AuditLog> latest(int max) {
        return em.createQuery("SELECT a FROM AuditLog a LEFT JOIN FETCH a.user ORDER BY a.createdAt DESC, a.id DESC",
                        AuditLog.class)
                .setMaxResults(max)
                .getResultList();
    }

    private User actor() {
        try {
            User user = currentUser.get();
            return user == null || user.getId() == null ? null : em.getReference(User.class, user.getId());
        } catch (ContextNotActiveException noRequest) {
            return null;
        }
    }
}
