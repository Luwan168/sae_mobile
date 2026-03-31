package sae.openminds;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import sae.openminds.fragments.AboutFragment;
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

// ============================================================
//  app/src/main/java/sae/openminds/NavigationDrawerHelper.java
//
//  RÔLES ET ACCÈS :
//  ┌──────────────────────────┬──────────┬───────────┬───────┐
//  │ Item                     │ Bénévole │ Formateur │ Admin │
//  ├──────────────────────────┼──────────┼───────────┼───────┤
//  │ Accueil                  │    ✓     │     ✓     │   ✓   │
//  │ Formations               │    ✓     │     ✓     │   ✓   │
//  │ Entraînement (Quiz)      │    ✓     │     ✓     │   ✓   │
//  │ Badges                   │    ✓     │     ✓     │   ✓   │
//  │ Actualités               │    ✓     │     ✓     │   ✓   │
//  │ Ressources               │    ✓     │     ✓     │   ✓   │
//  │ Carte                    │    ✓     │     ✓     │   ✓   │
//  │ Mon profil               │    ✓     │     ✓     │   ✓   │
//  │ Espace formateur         │          │     ✓     │   ✓   │
//  │ Administration           │          │           │   ✓   │
//  │ Paramètres               │    ✓     │     ✓     │   ✓   │
//  │ À propos                 │    ✓     │     ✓     │   ✓   │
//  └──────────────────────────┴──────────┴───────────┴───────┘
// ============================================================
public class NavigationDrawerHelper implements NavigationView.OnNavigationItemSelectedListener {

    private final AppCompatActivity activity;
    private final DrawerLayout      drawerLayout;

    public NavigationDrawerHelper(AppCompatActivity activity,
                                  DrawerLayout drawerLayout,
                                  Toolbar toolbar,
                                  NavigationView navigationView,
                                  String role,
                                  String firstname,
                                  String lastname) {
        this.activity     = activity;
        this.drawerLayout = drawerLayout;

        setupDrawer(toolbar, navigationView);
        filterMenuByRole(navigationView, role);
        setupHeader(navigationView, firstname, lastname, role);
    }

    private void setupDrawer(Toolbar toolbar, NavigationView navigationView) {
        activity.setSupportActionBar(toolbar);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Fermer le drawer avec le bouton retour
        OnBackPressedCallback callback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, callback);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(View v)  { callback.setEnabled(true);  }
            @Override public void onDrawerClosed(View v)  { callback.setEnabled(false); }
        });
    }

    /**
     * Masque les items du menu selon le rôle de l'utilisateur.
     */
    private void filterMenuByRole(NavigationView navigationView, String role) {
        boolean isFormateur = role.equals(Config.ROLE_FORMATEUR) || role.equals(Config.ROLE_ADMIN);
        boolean isAdmin     = role.equals(Config.ROLE_ADMIN);

        navigationView.getMenu().findItem(R.id.nav_espace_formateur).setVisible(isFormateur);
        navigationView.getMenu().findItem(R.id.nav_administration).setVisible(isAdmin);
    }

    /**
     * Affiche le prénom, nom et rôle dans le header du Drawer.
     */
    private void setupHeader(NavigationView navigationView, String firstname, String lastname, String role) {
        View header = navigationView.getHeaderView(0);
        if (header == null) return;

        TextView tvName = header.findViewById(R.id.tvHeaderName);
        TextView tvRole = header.findViewById(R.id.tvHeaderRole);

        if (tvName != null) tvName.setText(firstname + " " + lastname);
        if (tvRole != null) {
            String roleLabel;
            switch (role) {
                case Config.ROLE_ADMIN:     roleLabel = activity.getString(R.string.role_admin);     break;
                case Config.ROLE_FORMATEUR: roleLabel = activity.getString(R.string.role_formateur); break;
                default:                    roleLabel = activity.getString(R.string.role_benevole);  break;
            }
            tvRole.setText(roleLabel);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int    id       = item.getItemId();
        String title    = item.getTitle() != null ? item.getTitle().toString() : "";
        MainActivity ma = (MainActivity) activity;

        if (id == R.id.nav_accueil) {
            ma.loadFragment(new AccueilFragment(),    activity.getString(R.string.menu_home));
        } else if (id == R.id.nav_formations) {
            ma.loadFragment(new FormationsFragment(), activity.getString(R.string.menu_formations));
        } else if (id == R.id.nav_entrainement) {
            ma.loadFragment(new QuizFragment(),       activity.getString(R.string.menu_entrainement));
        } else if (id == R.id.nav_badges) {
            ma.loadFragment(new BadgesFragment(),     activity.getString(R.string.menu_badges));
        } else if (id == R.id.nav_actualites) {
            ma.loadFragment(new ActualitesFragment(), activity.getString(R.string.menu_actualites));
        } else if (id == R.id.nav_mes_ressources) {
            ma.loadFragment(new RessourcesFragment(), activity.getString(R.string.menu_ressources));
        } else if (id == R.id.nav_carte) {
            ma.loadFragment(new CarteFragment(),      activity.getString(R.string.menu_carte));
        } else if (id == R.id.nav_mon_profil) {
            ma.loadFragment(new ProfilFragment(),     activity.getString(R.string.menu_profil));
        } else if (id == R.id.nav_espace_formateur) {
            ma.loadFragment(new FormateurFragment(),  activity.getString(R.string.menu_formateur));
        } else if (id == R.id.nav_administration) {
            ma.loadFragment(new AdminFragment(),      activity.getString(R.string.menu_admin));
        } else if (id == R.id.nav_parametres) {
            ma.loadFragment(new ParametresFragment(), activity.getString(R.string.menu_settings));
        } else if (id == R.id.nav_about) {
            ma.loadFragment(new AboutFragment(),      activity.getString(R.string.menu_about));
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        // Supprimer le token et le rôle
        activity.getSharedPreferences(Config.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().clear().apply();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
