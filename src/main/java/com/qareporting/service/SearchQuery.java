package com.qareporting.service;

import com.qareporting.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Petite aide pour les listes recherchées et paginées EN BASE : on assemble les conditions
 * (périmètre du rôle, statut, texte recherché), puis on lance un COUNT et une requête
 * limitée à la page affichée. La recherche utilise LIKE : l'interclassement de la base
 * (utf8mb4_unicode_ci) ignore déjà les accents et la casse.
 */
public final class SearchQuery {

    private final String from;
    private final StringBuilder where = new StringBuilder(" WHERE 1=1");
    private final Map<String, Object> params = new LinkedHashMap<>();

    /** @param from « FROM Defect d LEFT JOIN d.assignedTo a » : alias et jointures utilisés par les conditions */
    SearchQuery(String from) {
        this.from = from;
    }

    SearchQuery and(String condition) {
        where.append(" AND ").append(condition);
        return this;
    }

    SearchQuery param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    /** Condition de périmètre, avec les paramètres :scopeTeam / :scopeUser qu'elle utilise. */
    SearchQuery scope(String condition, User viewer) {
        and(condition);
        if (condition.contains(":scopeTeam")) {
            param("scopeTeam", viewer.getTeam());
        }
        if (condition.contains(":scopeUser")) {
            param("scopeUser", viewer);
        }
        return this;
    }

    /** Texte recherché dans plusieurs champs ; « ANO-12 », « t12 » ou « 12 » cherchent aussi l'identifiant. */
    SearchQuery text(String q, String idField, String idPrefix, String... fields) {
        if (q == null || q.isBlank()) {
            return this;
        }
        String needle = q.strip();
        StringBuilder c = new StringBuilder("(");
        for (int i = 0; i < fields.length; i++) {
            c.append(i == 0 ? "" : " OR ").append(fields[i]).append(" LIKE :q ESCAPE '!'");
        }
        String digits = needle.toLowerCase().startsWith(idPrefix.toLowerCase()) ? needle.substring(idPrefix.length()) : needle;
        if (idField != null && digits.matches("\\d{1,18}")) {
            c.append(" OR ").append(idField).append(" = :qid");
            param("qid", Long.parseLong(digits));
        }
        c.append(")");
        and(c.toString());
        String escaped = needle.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return param("q", "%" + escaped + "%");
    }

    long count(EntityManager em, String alias) {
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(DISTINCT " + alias + ") " + from + where, Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    <T> List<T> page(EntityManager em, String alias, Class<T> type, String orderBy, int first, int size) {
        TypedQuery<T> query = em.createQuery("SELECT DISTINCT " + alias + " " + from + where + " ORDER BY " + orderBy, type);
        params.forEach(query::setParameter);
        return query.setFirstResult(first).setMaxResults(size).getResultList();
    }
}
