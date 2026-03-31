# OpenMinds — Guide d'installation

## Structure du projet

```
openminds_final/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/sae/openminds/
│   │   ├── Config.java                  ← MODIFIER : BASE_URL après déploiement
│   │   ├── SplashActivity.java
│   │   ├── LoginActivity.java
│   │   ├── RegisterActivity.java
│   │   ├── MainActivity.java
│   │   ├── NavigationDrawerHelper.java  ← Gère le filtrage par rôle
│   │   ├── adapters/                    ← 5 adapteurs ListView
│   │   ├── fragments/                   ← 11 fragments
│   │   ├── models/                      ← 6 modèles POJO
│   │   └── utils/                       ← LocaleHelper, NotificationReceiver, BootReceiver
│   └── res/
│       ├── layout/                      ← 21 fichiers XML
│       ├── menu/drawer_menu.xml
│       ├── drawable/                    ← 13 icônes vectorielles
│       ├── values/strings.xml           ← Anglais (défaut)
│       ├── values-fr/strings.xml        ← Français
│       └── values-es/strings.xml        ← Espagnol
├── php/                                 ← 19 scripts PHP → à déposer dans /www/openminds_server/
└── openminds_db.sql                     ← À importer dans phpMyAdmin AlwaysData
```

---

## Étape 1 — Base de données (phpMyAdmin AlwaysData)

1. Connectez-vous à https://admin.alwaysdata.com
2. Allez dans **Bases de données → MySQL → phpMyAdmin**
3. Sélectionnez **openminds_bd**
4. Onglet **Importer** → choisir `openminds_db.sql` → Exécuter
5. La base contient 3 comptes de test (mot de passe : `Admin1234`) :
   - `admin@openminds.fr`     → rôle admin
   - `formateur@openminds.fr` → rôle formateur
   - `benevole@openminds.fr`  → rôle bénévole

---

## Étape 2 — Déploiement PHP (AlwaysData)

1. Allez dans **Accès distant → Gestionnaire de fichiers**
2. Naviguez dans `/www/`
3. Créez un dossier `openminds_server`
4. Déposez tous les fichiers du dossier `php/` dans `/www/openminds_server/`
5. **Modifiez `config.php`** avec vos identifiants AlwaysData :
   ```php
   $db_host = "mysql-openminds.alwaysdata.net";
   $db_uid  = "votre_identifiant";   // ← à remplacer
   $db_pass = "votre_mot_de_passe";  // ← à remplacer
   $db_name = "openminds_bd";
   ```
6. Testez dans le navigateur :
   `https://openminds.alwaysdata.net/openminds_server/getBonnesPratiques.php`
   → doit retourner `{"status":"success","pratique":{...}}`

---

## Étape 3 — Application Android (Android Studio)

1. Ouvrez Android Studio → **Open** → sélectionnez ce dossier
2. Modifiez `Config.java` :
   ```java
   public static final String BASE_URL =
       "https://openminds.alwaysdata.net/openminds_server/";
   ```
3. **Sync Gradle** (le bouton éléphant en haut à droite)
4. Lancez l'émulateur ou connectez un appareil → **Run**

---

## Gestion des rôles

Le menu Drawer s'adapte automatiquement selon le rôle :

| Élément menu        | Bénévole | Formateur | Admin |
|---------------------|:--------:|:---------:|:-----:|
| Accueil             | ✓        | ✓         | ✓     |
| Formations          | ✓        | ✓         | ✓     |
| Entraînement (Quiz) | ✓        | ✓         | ✓     |
| Badges              | ✓        | ✓         | ✓     |
| Actualités          | ✓        | ✓         | ✓     |
| Ressources          | ✓        | ✓         | ✓     |
| Formations proches  | ✓        | ✓         | ✓     |
| Mon profil          | ✓        | ✓         | ✓     |
| Espace formateur    |          | ✓         | ✓     |
| Administration      |          |           | ✓     |
| Paramètres          | ✓        | ✓         | ✓     |

---

## Internationalisation

La langue par défaut est **l'anglais**. Pour changer :
- Dans l'app : Paramètres → Langue → Français / Español / English
- Les fichiers de traduction sont dans :
  - `res/values/strings.xml` (anglais)
  - `res/values-fr/strings.xml` (français)
  - `res/values-es/strings.xml` (espagnol)

Pour ajouter une nouvelle langue, créez `res/values-XX/strings.xml`
et ajoutez `"XX"` dans `ParametresFragment.java`.
