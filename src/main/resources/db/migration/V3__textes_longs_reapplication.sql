-- V3 : réapplique la conversion des textes longs en TEXT (voir V2).
-- Sur la base de développement, V2 avait été enregistrée comme appliquée, puis une ancienne
-- version de l'application (encore en mise à jour automatique Hibernate) avait remis les
-- colonnes en TINYTEXT. Une migration déjà appliquée ne se modifie jamais : on en ajoute
-- une nouvelle. Sur une base neuve, ces instructions ne changent rien.
ALTER TABLE `activities` MODIFY `blocked_reason` TEXT NULL;
ALTER TABLE `activities` MODIFY `comment` TEXT NULL;
ALTER TABLE `app_settings` MODIFY `setting_value` TEXT NULL;
ALTER TABLE `campaigns` MODIFY `description` TEXT NULL;
ALTER TABLE `defects` MODIFY `actual_result` TEXT NULL;
ALTER TABLE `defects` MODIFY `description` TEXT NULL;
ALTER TABLE `defects` MODIFY `expected_result` TEXT NULL;
ALTER TABLE `defects` MODIFY `reproduction_steps` TEXT NULL;
ALTER TABLE `defect_histories` MODIFY `comment` TEXT NULL;
ALTER TABLE `notifications` MODIFY `message` TEXT NULL;
ALTER TABLE `projects` MODIFY `description` TEXT NULL;
ALTER TABLE `tests` MODIFY `description` TEXT NULL;
ALTER TABLE `tests` MODIFY `expected_result` TEXT NULL;
ALTER TABLE `tests` MODIFY `preconditions` TEXT NULL;
ALTER TABLE `test_executions` MODIFY `actual_result` TEXT NULL;
