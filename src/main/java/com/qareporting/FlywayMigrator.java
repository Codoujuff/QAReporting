package com.qareporting;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Migrations de schéma versionnées (src/main/resources/db/migration/V&lt;n&gt;__description.sql),
 * appliquées au démarrage, avant tout accès aux données (StartupSeeder en dépend).
 * Hibernate ne modifie plus la base (hbm2ddl.auto = none) : chaque évolution du schéma
 * est un script SQL relu, versionné avec le code, appliqué une seule fois, dans l'ordre.
 *
 * Base existante sans historique Flyway (créée par Hibernate avant les migrations) :
 * elle est « baselinée » en version 1, et seules les migrations suivantes s'y appliquent.
 * Puis on vérifie que les tables correspondent aux entités : une entité modifiée sans
 * migration fait échouer le démarrage avec un message clair, au lieu d'erreurs en cours d'usage.
 *
 * Transactions gérées à la main (BEAN) : Flyway valide lui-même chaque migration, ce qu'une
 * transaction JTA du conteneur interdirait.
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class FlywayMigrator {

    private static final Logger LOG = Logger.getLogger(FlywayMigrator.class.getName());

    @Resource(lookup = "java:/QaReportingJ2eeDS")
    DataSource dataSource;

    @PersistenceUnit(unitName = "qaReportingJ2eePU")
    EntityManagerFactory emf;

    @PostConstruct
    void migrate() {
        MigrateResult result = Flyway.configure(FlywayMigrator.class.getClassLoader())
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("schema existant (cree par Hibernate)")
                .load()
                .migrate();
        LOG.info(() -> "Migrations de schéma : " + result.migrationsExecuted + " appliquée(s), version actuelle "
                + (result.targetSchemaVersion != null ? result.targetSchemaVersion : result.initialSchemaVersion));

        try {
            emf.unwrap(org.hibernate.SessionFactory.class).getSchemaManager().validateMappedObjects();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Le schéma de la base ne correspond pas aux entités Java. Ajoutez une migration "
                    + "db/migration/V<n>__....sql pour la modification faite. Détail : " + e.getMessage(), e);
        }
    }
}
