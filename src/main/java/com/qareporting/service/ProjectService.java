package com.qareporting.service;

import com.qareporting.entity.Project;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectService extends AbstractCrudService<Project, Long> {
    @Override
    protected Class<Project> entityClass() {
        return Project.class;
    }
}
