package com.qareporting.service;

import com.qareporting.entity.AppSetting;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;

@ApplicationScoped
public class AppSettingService extends AbstractCrudService<AppSetting, Long> {

    @Override
    protected Class<AppSetting> entityClass() {
        return AppSetting.class;
    }

    public List<AppSetting> findAllOrdered() {
        return em.createQuery("SELECT s FROM AppSetting s ORDER BY s.settingKey", AppSetting.class)
                .getResultList();
    }

    public AppSetting findByKey(String key) {
        try {
            return em.createQuery("SELECT s FROM AppSetting s WHERE s.settingKey = :key", AppSetting.class)
                    .setParameter("key", key)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
