package com.qareporting.service;

import com.qareporting.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TeamService extends AbstractCrudService<Team, Long> {
    @Override
    protected Class<Team> entityClass() {
        return Team.class;
    }
}
