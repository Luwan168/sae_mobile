-- ============================================================
--  OpenMinds — Mise à jour de la base de données
--  À exécuter dans phpMyAdmin > onglet SQL
--  UNE SEULE FOIS sur votre base existante
-- ============================================================

-- Ajouter les colonnes status et submitted_by à bonne_pratique
ALTER TABLE `bonne_pratique`
  ADD COLUMN `status` ENUM('pending','approved','rejected') NOT NULL DEFAULT 'approved' AFTER `active`,
  ADD COLUMN `submitted_by` INT(11) DEFAULT NULL AFTER `status`,
  ADD FOREIGN KEY fk_bp_user (`submitted_by`) REFERENCES `user`(`id`) ON DELETE SET NULL;

-- Les tips déjà existants sont marqués comme approuvés
UPDATE `bonne_pratique` SET `status` = 'approved', `active` = 1;
