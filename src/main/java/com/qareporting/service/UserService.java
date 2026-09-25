package com.qareporting.service;

import com.qareporting.entity.Role;
import com.qareporting.entity.Team;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;

@ApplicationScoped
public class UserService extends AbstractCrudService<User, Long> {

    @Override
    protected Class<User> entityClass() {
        return User.class;
    }

    public Role findRole(Long id) {
        return em.find(Role.class, id);
    }

    public Team findTeam(Long id) {
        return em.find(Team.class, id);
    }

    public User findByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Role picker for the admin CRUD screens — there are only ever four rows, no dedicated service needed. */
    public List<Role> listRoles() {
        return em.createQuery("SELECT r FROM Role r ORDER BY r.name", Role.class).getResultList();
    }

    /** Members of a team, for the QA Lead's team dashboard. */
    /** Active / désactive un compte ; la désactivation révoque aussi tous ses jetons d'API. */
    @jakarta.transaction.Transactional
    public void setActive(Long userId, boolean active) {
        User user = find(userId);
        if (user == null) {
            return;
        }
        user.setActive(active);
        update(user);
        if (!active) {
            em.createQuery("DELETE FROM AccessToken t WHERE t.user = :user").setParameter("user", user).executeUpdate();
        }
    }

    public List<User> findByTeam(Team team) {
        return em.createQuery("SELECT u FROM User u WHERE u.team = :team ORDER BY u.name", User.class)
                .setParameter("team", team)
                .getResultList();
    }
}
