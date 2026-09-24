package com.qareporting.service;

import com.qareporting.entity.Environment;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnvironmentService extends AbstractCrudService<Environment, Long> {
    @Override
    protected Class<Environment> entityClass() {
        return Environment.class;
    }
}
