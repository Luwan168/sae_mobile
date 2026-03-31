-- ============================================================
--  OpenMinds — Données de test complètes
--  À exécuter dans phpMyAdmin > onglet SQL
--  APRÈS avoir exécuté sql_updates.sql
-- ============================================================

-- Ajouter le champ adresse/ville sur user
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `address` VARCHAR(255) DEFAULT NULL AFTER `phone`,
  ADD COLUMN IF NOT EXISTS `city`    VARCHAR(100) DEFAULT NULL AFTER `address`;

-- Mettre à jour les comptes de test avec des villes
UPDATE `user` SET `city` = 'Paris',  `address` = '1 rue de la Paix, 75001 Paris' WHERE email = 'admin@openminds.fr';
UPDATE `user` SET `city` = 'Lyon',   `address` = '5 place Bellecour, 69002 Lyon' WHERE email = 'formateur@openminds.fr';
UPDATE `user` SET `city` = 'Paris',  `address` = '10 avenue des Champs, 75008 Paris' WHERE email = 'benevole@openminds.fr';

-- ── Formations supplémentaires ───────────────────────────────
INSERT INTO `formation` (`title`, `description`, `theme`, `type`, `location`, `created_by`) VALUES
('Gestion du stress bénévole',   'Techniques pour gérer le stress dans les missions associatives.', 'bien-etre',    'en_ligne',  NULL,   1),
('Premiers secours citoyens',    'Formation aux gestes de premiers secours adaptée aux bénévoles.', 'sante',        'presentiel','Lyon',  1),
('Communication non violente',   'Apprenez les bases de la CNV pour mieux échanger.', 'communication','en_ligne',  NULL,   1),
('Éco-gestes au quotidien',      'Réduire son empreinte carbone dans la vie associative.',            'environnement','presentiel','Paris', 1);

-- ── Quiz — Formation 1 : Introduction à l\'environnement ─────
INSERT INTO `quiz` (`formation_id`, `question`, `choices`, `correct_answer`) VALUES
(1, 'Quel gaz est principalement responsable du réchauffement climatique ?',
   '["Oxygène","CO2 (dioxyde de carbone)","Azote","Vapeur d\'eau"]',
   'CO2 (dioxyde de carbone)'),
(1, 'Combien de piliers compte le développement durable ?',
   '["2","3","4","5"]',
   '3'),
(1, 'Quelle énergie est renouvelable ?',
   '["Charbon","Pétrole","Énergie solaire","Gaz naturel"]',
   'Énergie solaire');

-- ── Quiz — Formation 2 : Inclusion et diversité ──────────────
INSERT INTO `quiz` (`formation_id`, `question`, `choices`, `correct_answer`) VALUES
(2, 'Que signifie l\'inclusion en milieu associatif ?',
   '["Exclure les minorités","Intégrer toutes les personnes quelles que soient leurs différences","Favoriser une seule culture","Limiter la diversité"]',
   'Intégrer toutes les personnes quelles que soient leurs différences'),
(2, 'Quelle pratique favorise l\'inclusion ?',
   '["Utiliser un langage neutre","Ignorer les différences culturelles","Séparer les groupes","Favoriser les anciens bénévoles"]',
   'Utiliser un langage neutre');

-- ── Quiz — Formation 3 : Égalité des chances ─────────────────
INSERT INTO `quiz` (`formation_id`, `question`, `choices`, `correct_answer`) VALUES
(3, 'Qu\'est-ce que l\'égalité des chances ?',
   '["Donner les mêmes ressources à tous","Offrir à chacun les moyens d\'atteindre son potentiel","Traiter tout le monde identiquement","Ignorer les inégalités"]',
   'Offrir à chacun les moyens d\'atteindre son potentiel'),
(3, 'Quelle mesure favorise l\'égalité des chances ?',
   '["Les quotas","La méritocratie pure","L\'accès équitable à la formation","La sélection par origine"]',
   'L\'accès équitable à la formation');

-- ── Quiz — Formation 4 : Gestion du stress ───────────────────
INSERT INTO `quiz` (`formation_id`, `question`, `choices`, `correct_answer`) VALUES
(4, 'Quelle technique aide à réduire le stress immédiatement ?',
   '["La respiration profonde","Travailler plus","Ignorer le problème","Prendre des médicaments"]',
   'La respiration profonde'),
(4, 'Qu\'est-ce que le burnout bénévole ?',
   '["Un surplus d\'énergie","Un épuisement physique et mental lié à l\'engagement","Une promotion","Une récompense"]',
   'Un épuisement physique et mental lié à l\'engagement');

-- ── Badges supplémentaires ────────────────────────────────────
INSERT INTO `badge` (`name`, `formation_id`, `image_url`) VALUES
('Expert bien-être',     4, NULL),
('Secouriste citoyen',   5, NULL),
('Communicant bienveillant', 6, NULL),
('Zéro carbone',         7, NULL);

-- ── Inscriptions de test ──────────────────────────────────────
-- Bénévole inscrit à plusieurs formations
INSERT IGNORE INTO `enrollment` (`user_id`, `formation_id`, `status`, `score`) VALUES
(3, 1, 'termine',  85),
(3, 2, 'termine',  72),
(3, 3, 'en_cours', NULL),
(3, 4, 'en_cours', NULL);

-- Badge obtenu par le bénévole (formations terminées avec score >= 70)
INSERT IGNORE INTO `user_badge` (`user_id`, `badge_id`) VALUES
(3, 1),
(3, 2);

-- ── Ressources supplémentaires ───────────────────────────────
INSERT INTO `ressource` (`title`, `content`, `theme`, `type`, `created_by`) VALUES
('Guide complet du développement durable',
 'Ce guide approfondi couvre tous les aspects du développement durable : environnement, social et économique. Découvrez comment appliquer ces principes dans votre vie associative au quotidien.',
 'environnement', 'guide', 1),
('Article : Gérer les conflits en association',
 'Les conflits au sein des associations sont inévitables. Cet article vous donne des clés pour les identifier, les désamorcer et en faire une opportunité de renforcement du groupe.',
 'communication', 'article', 1),
('Guide de la communication inclusive',
 'Vocabulaire, tournures de phrases et exemples concrets pour adopter une communication vraiment inclusive dans toutes vos communications associatives.',
 'inclusion', 'guide', 1);

-- ── Actualités supplémentaires ───────────────────────────────
INSERT INTO `actualite` (`title`, `content`, `created_by`) VALUES
('OpenMinds dépasse 100 inscrits !',
 'Nous sommes ravis d\'annoncer que notre plateforme a dépassé 100 bénévoles inscrits. Merci à toute la communauté pour son engagement !',
 1),
('Nouvelle session présentielle à Lyon',
 'Une nouvelle session de la formation "Premiers secours citoyens" est disponible à Lyon le mois prochain. Inscrivez-vous vite, les places sont limitées !',
 1);

-- ── Bonnes pratiques supplémentaires (approuvées) ────────────
INSERT INTO `bonne_pratique` (`content`, `theme`, `active`, `status`, `submitted_by`) VALUES
('Apportez votre propre gourde lors des événements associatifs.', 'environnement', 1, 'approved', NULL),
('Organisez des covoiturages pour vos déplacements en groupe.', 'environnement', 1, 'approved', NULL),
('Prenez le temps de remercier vos collègues bénévoles régulièrement.', 'inclusion', 1, 'approved', NULL);

-- ── Tip en attente de validation (pour tester la modération) ─
INSERT INTO `bonne_pratique` (`content`, `theme`, `active`, `status`, `submitted_by`) VALUES
('Pensez à trier vos déchets même lors des événements associatifs rapides.', 'environnement', 0, 'pending', 3);

