-- ============================================================
--  OpenMinds — Script de création de la base de données
--  À importer via phpMyAdmin sur AlwaysData
--  Base : openminds_bd
--
--  COMPTES DE TEST (mot de passe : password) :
--    admin@openminds.fr
--    formateur@openminds.fr
--    benevole@openminds.fr
--
--  Pour changer vers Admin1234 : déposez genhash.php sur le
--  serveur et appelez-le UNE SEULE FOIS dans le navigateur,
--  puis supprimez-le.
-- ============================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";
SET NAMES utf8mb4;

-- ── TABLE : user ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user` (
  `id`        INT(11)      NOT NULL AUTO_INCREMENT,
  `firstname` VARCHAR(80)  NOT NULL,
  `lastname`  VARCHAR(80)  NOT NULL,
  `email`     VARCHAR(150) NOT NULL UNIQUE,
  `password`  VARCHAR(255) NOT NULL,
  `phone`     VARCHAR(20)  DEFAULT NULL,
  `token`     VARCHAR(100) DEFAULT NULL,
  `role`      ENUM('benevole','formateur','admin') NOT NULL DEFAULT 'benevole',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : formation ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS `formation` (
  `id`          INT(11)      NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(150) NOT NULL,
  `description` TEXT         DEFAULT NULL,
  `theme`       VARCHAR(80)  DEFAULT NULL,
  `type`        ENUM('presentiel','en_ligne') NOT NULL DEFAULT 'en_ligne',
  `location`    VARCHAR(150) DEFAULT NULL,
  `created_by`  INT(11)      DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : enrollment ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS `enrollment` (
  `id`           INT(11)  NOT NULL AUTO_INCREMENT,
  `user_id`      INT(11)  NOT NULL,
  `formation_id` INT(11)  NOT NULL,
  `status`       ENUM('en_cours','termine','abandonne') NOT NULL DEFAULT 'en_cours',
  `score`        INT(3)   DEFAULT NULL,
  `enrolled_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_enrollment` (`user_id`, `formation_id`),
  FOREIGN KEY (`user_id`)      REFERENCES `user`(`id`)      ON DELETE CASCADE,
  FOREIGN KEY (`formation_id`) REFERENCES `formation`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : quiz ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `quiz` (
  `id`             INT(11) NOT NULL AUTO_INCREMENT,
  `formation_id`   INT(11) NOT NULL,
  `question`       TEXT    NOT NULL,
  `choices`        JSON    NOT NULL,
  `correct_answer` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`formation_id`) REFERENCES `formation`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : badge ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `badge` (
  `id`           INT(11)      NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(100) NOT NULL,
  `formation_id` INT(11)      DEFAULT NULL,
  `image_url`    VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`formation_id`) REFERENCES `formation`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : user_badge ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user_badge` (
  `id`          INT(11)  NOT NULL AUTO_INCREMENT,
  `user_id`     INT(11)  NOT NULL,
  `badge_id`    INT(11)  NOT NULL,
  `obtained_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_badge` (`user_id`, `badge_id`),
  FOREIGN KEY (`user_id`)  REFERENCES `user`(`id`)  ON DELETE CASCADE,
  FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : actualite ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS `actualite` (
  `id`           INT(11)      NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(200) NOT NULL,
  `content`      TEXT         NOT NULL,
  `published_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`   INT(11)      DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : ressource ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS `ressource` (
  `id`         INT(11)      NOT NULL AUTO_INCREMENT,
  `title`      VARCHAR(200) NOT NULL,
  `content`    TEXT         NOT NULL,
  `theme`      VARCHAR(80)  DEFAULT NULL,
  `type`       ENUM('article','guide') NOT NULL DEFAULT 'article',
  `created_by` INT(11)      DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── TABLE : bonne_pratique ──────────────────────────────────
CREATE TABLE IF NOT EXISTS `bonne_pratique` (
  `id`         INT(11)    NOT NULL AUTO_INCREMENT,
  `content`    TEXT       NOT NULL,
  `theme`      VARCHAR(80) DEFAULT NULL,
  `active`     TINYINT(1) NOT NULL DEFAULT 1,
  `created_by` INT(11)    DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  DONNÉES DE TEST
--  Mot de passe : "password" pour les 3 comptes
--  Hash bcrypt valide : $2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- ============================================================

INSERT INTO `user` (`firstname`,`lastname`,`email`,`password`,`role`) VALUES
('Admin','OpenMinds','admin@openminds.fr','$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','admin'),
('Marie','Dupont','formateur@openminds.fr','$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','formateur'),
('Lucas','Martin','benevole@openminds.fr','$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi','benevole');

INSERT INTO `formation` (`title`,`description`,`theme`,`type`,`location`,`created_by`) VALUES
('Introduction a l environnement','Decouvrez les bases de l ecologie et du developpement durable.','environnement','en_ligne',NULL,1),
('Inclusion et diversite','Comprendre et promouvoir l inclusion dans les associations.','inclusion','presentiel','Paris',1),
('Egalite des chances','Les fondamentaux de l egalite dans la vie associative.','egalite','en_ligne',NULL,1);

INSERT INTO `quiz` (`formation_id`,`question`,`choices`,`correct_answer`) VALUES
(1,'Qu est-ce que le developpement durable ?','["Croissance economique illimitee","Repondre aux besoins presents sans compromettre l avenir","Utiliser toutes les ressources naturelles","Industrialiser le monde"]','Repondre aux besoins presents sans compromettre l avenir'),
(1,'Combien y a-t-il de piliers du developpement durable ?','["2","3","4","5"]','3'),
(2,'Que signifie l inclusion ?','["Exclure les minorites","Integrer toutes les personnes quelles que soient leurs differences","Favoriser une seule culture","Limiter la diversite"]','Integrer toutes les personnes quelles que soient leurs differences');

INSERT INTO `badge` (`name`,`formation_id`,`image_url`) VALUES
('Eco-citoyen',1,NULL),
('Ambassadeur inclusion',2,NULL),
('Champion egalite',3,NULL);

INSERT INTO `actualite` (`title`,`content`,`created_by`) VALUES
('Lancement d OpenMinds','Nous sommes ravis de lancer notre plateforme de formation citoyenne !',1),
('Nouvelle formation disponible','La formation Egalite des chances est maintenant disponible en ligne.',1);

INSERT INTO `ressource` (`title`,`content`,`theme`,`type`,`created_by`) VALUES
('Guide du benevole eco-responsable','Ce guide vous accompagne dans vos pratiques eco-responsables au quotidien. Privilegiez les transports en commun, reduisez vos dechets et sensibilisez votre entourage.','environnement','guide',1),
('Article : l inclusion en pratique','Comment mettre en place une politique d inclusion concrete dans votre association. Des conseils pratiques et des exemples concrets.','inclusion','article',1);

INSERT INTO `bonne_pratique` (`content`,`theme`,`active`,`created_by`) VALUES
('Eteignez les lumieres en quittant une piece.','environnement',1,1),
('Privilegiez les transports en commun pour vos deplacements associatifs.','environnement',1,1),
('Accueillez chaque nouveau benevole avec bienveillance.','inclusion',1,1),
('Utilisez un langage inclusif dans toutes vos communications.','inclusion',1,1),
('Recyclez vos dechets lors de vos evenements associatifs.','environnement',1,1),
('Proposez des alternatives vegetariennes lors de vos evenements.','environnement',1,1);

