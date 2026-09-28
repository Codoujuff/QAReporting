-- V2 : les textes longs (descriptions, commentaires, étapes...) étaient en TINYTEXT,
-- limité à 255 octets : une description un peu longue faisait échouer l'enregistrement.
-- On les passe en TEXT (64 Ko), sans rien changer d'autre.
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
