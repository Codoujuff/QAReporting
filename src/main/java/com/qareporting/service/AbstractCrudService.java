package com.qareporting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Shared find/create/update/delete plumbing for the simple admin-managed
 * entities (Team, Environment, Project, User...) — the Jakarta EE
 * counterpart of Laravel's apiResource() generating the same five actions
 * for a model.
 */
public abstract class AbstractCrudService<T, ID> {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    protected EntityManager em;

    protected abstract Class<T> entityClass();

    public List<T> findAll() {
        return em.createQuery("SELECT e FROM " + entityClass().getSimpleName() + " e", entityClass())
                .getResultList();
    }

    public T find(ID id) {
        return em.find(entityClass(), id);
    }

    @Transactional
    public T create(T entity) {
        em.persist(entity);
        return entity;
    }

    @Transactional
    public T update(T entity) {
        return em.merge(entity);
    }

    @Transactional
    public boolean delete(ID id) {
        T entity = find(id);
        if (entity == null) {
            return false;
        }
        em.remove(entity);
        return true;
    }
}
