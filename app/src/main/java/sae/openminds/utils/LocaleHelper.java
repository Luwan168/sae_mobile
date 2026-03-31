package sae.openminds.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

import sae.openminds.Config;

// ============================================================
//  app/src/main/java/sae/openminds/utils/LocaleHelper.java
//  Gestion du changement de langue à chaud
// ============================================================
public class LocaleHelper {

    public static Context applyLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(Config.KEY_LANGUAGE, "en");
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static void saveLanguage(Context context, String languageCode) {
        context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(Config.KEY_LANGUAGE, languageCode).apply();
    }
}
