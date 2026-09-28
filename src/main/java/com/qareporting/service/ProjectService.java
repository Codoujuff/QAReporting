package com.qareporting.service;

import com.qareporting.entity.Project;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectService extends AbstractCrudService<Project, Long> {

    /** Projets sur lesquels la personne peut travailler : ceux de son équipe et ceux dont elle est membre. */
    public java.util.List<Project> visibleProjects(com.qareporting.entity.User viewer) {
        if (Scope.seesEverything(viewer)) {
            return em.createQuery("SELECT p FROM Project p ORDER BY p.name", Project.class).getResultList();
        }
        var q = em.createQuery("SELECT p FROM Project p WHERE " + Scope.projectClause("p", viewer)
                + " ORDER BY p.name", Project.class);
        Scope.bindProjectScope(q, viewer);
        return q.getResultList();
    }

    public boolean isVisible(com.qareporting.entity.User viewer, Project project) {
        return project != null && (Scope.seesEverything(viewer) || Scope.onProject(project, viewer));
    }

    /**
     * Testeurs (QA / QA Lead actifs) à qui l'on peut confier un test ou une anomalie du
     * projet : son équipe et ses membres affectés. Projet sans équipe ni membre : tous.
     */
    public java.util.List<com.qareporting.entity.User> assignableUsers(Project project) {
        var roles = java.util.List.of(com.qareporting.entity.Role.QA, com.qareporting.entity.Role.QA_LEAD);
        boolean open = project == null || (project.getTeam() == null && project.getMembers().isEmpty());
        String jpql = "SELECT DISTINCT u FROM User u WHERE u.active = true AND u.role.name IN :roles"
                + (open ? "" : " AND (u.team = :team OR u IN (SELECT m FROM Project p JOIN p.members m WHERE p = :project))")
                + " ORDER BY u.name";
        var q = em.createQuery(jpql, com.qareporting.entity.User.class).setParameter("roles", roles);
        if (!open) {
            q.setParameter("team", project.getTeam()).setParameter("project", project);
        }
        return q.getResultList();
    }

    public boolean canBeAssignedTo(Project project, com.qareporting.entity.User user) {
        return user != null && assignableUsers(project).stream().anyMatch(u -> u.getId().equals(user.getId()));
    }
    @Override
    protected Class<Project> entityClass() {
        return Project.class;
    }
}
