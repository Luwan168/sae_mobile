package sae.openminds;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import sae.openminds.fragments.AccueilFragment;
import sae.openminds.fragments.ActualitesFragment;
import sae.openminds.fragments.AdminFragment;
import sae.openminds.fragments.BadgesFragment;
import sae.openminds.fragments.CarteFragment;
import sae.openminds.fragments.FormateurFragment;
import sae.openminds.fragments.FormationsFragment;
import sae.openminds.fragments.ParametresFragment;
import sae.openminds.fragments.ProfilFragment;
import sae.openminds.fragments.QuizFragment;
import sae.openminds.fragments.RessourcesFragment;
import sae.openminds.utils.LocaleHelper;

// ============================================================
//  app/src/main/java/sae/openminds/MainActivity.java
//  Point d'entrée principal – gère le Drawer et les fragments
//  Les items du menu s'affichent selon le rôle de l'utilisateur
// ============================================================
public class MainActivity extends AppCompatActivity {

    private NavigationDrawerHelper drawerHelper;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar        toolbar        = findViewById(R.id.toolbar);
        DrawerLayout   drawerLayout   = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Récupérer le rôle depuis SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String role      = prefs.getString(Config.KEY_ROLE,      Config.ROLE_BENEVOLE);
        String firstname = prefs.getString(Config.KEY_FIRSTNAME, "");
        String lastname  = prefs.getString(Config.KEY_LASTNAME,  "");

        drawerHelper = new NavigationDrawerHelper(
                this, drawerLayout, toolbar, navigationView, role, firstname, lastname);

        // Fragment par défaut
        if (savedInstanceState == null) {
            loadFragment(new AccueilFragment(), getString(R.string.menu_home));
            navigationView.setCheckedItem(R.id.nav_accueil);
        }
    }

    public void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        setTitle(title);
    }
}
