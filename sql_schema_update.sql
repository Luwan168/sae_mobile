-- ============================================================
--  OpenMinds — Mise à jour du schéma
--  À exécuter UNE SEULE FOIS dans phpMyAdmin
-- ============================================================

-- 1. Ajouter adresse et ville sur la table user
ALTER TABLE `user`
  ADD COLUMN `address` VARCHAR(255) DEFAULT NULL AFTER `phone`,
  ADD COLUMN `city`    VARCHAR(100) DEFAULT NULL AFTER `address`;

-- 2. Ajouter status et submitted_by sur bonne_pratique (si pas déjà fait)
ALTER TABLE `bonne_pratique`
  ADD COLUMN `status`       ENUM('pending','approved','rejected') NOT NULL DEFAULT 'approved' AFTER `active`,
  ADD COLUMN `submitted_by` INT(11) DEFAULT NULL AFTER `status`,
  ADD FOREIGN KEY fk_bp_user (`submitted_by`) REFERENCES `user`(`id`) ON DELETE SET NULL;

UPDATE `bonne_pratique` SET `status` = 'approved', `active` = 1 WHERE `status` IS NULL OR `status` = '';
