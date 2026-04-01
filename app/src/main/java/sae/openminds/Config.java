package sae.openminds;

// ============================================================
//  app/src/main/java/sae/openminds/Config.java
//  Modifier BASE_URL après déploiement sur AlwaysData
// ============================================================
public class Config {
    public static final String BASE_URL = "https://openminds.alwaysdata.net/openminds_server/";

    // Rôles utilisateur
    public static final String ROLE_BENEVOLE  = "benevole";
    public static final String ROLE_FORMATEUR = "formateur";
    public static final String ROLE_ADMIN     = "admin";

    // Clés SharedPreferences
    public static final String PREFS_NAME  = "OpenMindsPrefs";
    public static final String KEY_TOKEN     = "token";
    public static final String KEY_ROLE      = "role";
    public static final String KEY_FIRSTNAME = "firstname";
    public static final String KEY_LASTNAME  = "lastname";
    public static final String KEY_EMAIL     = "email";
    public static final String KEY_LANGUAGE  = "language";
    public static final String KEY_NOTIFS    = "notifications_enabled";
    public static final String[] THEMES = {"environment", "inclusion", "health", "citizenship"};
    public static final int      THEME_DEFAULT_INDEX = 0;
}
