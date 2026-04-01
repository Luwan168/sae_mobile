package sae.openminds.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.BadgeAdapter;
import sae.openminds.adapters.EnrollmentAdapter;
import sae.openminds.models.Badge;
import sae.openminds.models.Enrollment;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/ProfilFragment.java
// ============================================================
public class ProfilFragment extends Fragment {
    private ProgressBar progressBar;
    private ListView listViewBadges;
    private ListView lvBadges; // Changé de RecyclerView à ListView
    private BadgeAdapter badgeAdapter;
    private List<Badge> badgeList = new ArrayList<>();
    private Button btnShowBadges;
    private boolean isVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profil, container, false);
        progressBar  = view.findViewById(R.id.progressBar);
        TextView    tvName      = view.findViewById(R.id.tvName);
        TextView    tvEmail     = view.findViewById(R.id.tvEmail);
        TextView    tvRole      = view.findViewById(R.id.tvRole);
        TextView    tvDone      = view.findViewById(R.id.tvDone);
        TextView    tvAvgScore  = view.findViewById(R.id.tvAvgScore);
        TextView    tvEmpty     = view.findViewById(R.id.tvEmpty);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        ListView    listView    = view.findViewById(R.id.listViewEnrollments);
        lvBadges = view.findViewById(R.id.lv_badges_profil);
        listViewBadges = view.findViewById(R.id.lv_badges_profil);
        badgeAdapter = new BadgeAdapter(getContext(), badgeList);
        lvBadges.setAdapter(badgeAdapter);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String token     = prefs.getString(Config.KEY_TOKEN,     "");
        fetchBadges(token);
        String firstname = prefs.getString(Config.KEY_FIRSTNAME, "");
        String lastname  = prefs.getString(Config.KEY_LASTNAME,  "");
        String email     = prefs.getString(Config.KEY_EMAIL,     "");
        String role      = prefs.getString(Config.KEY_ROLE,      Config.ROLE_BENEVOLE);

        tvName.setText(firstname + " " + lastname);
        tvEmail.setText(email);

        String roleLabel;
        switch (role) {
            case Config.ROLE_ADMIN:     roleLabel = getString(R.string.role_admin);     break;
            case Config.ROLE_FORMATEUR: roleLabel = getString(R.string.role_formateur); break;
            default:                    roleLabel = getString(R.string.role_benevole);  break;
        }
        tvRole.setText(roleLabel);

        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getProgression.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json  = JsonParser.parseString(result).getAsJsonObject();
                        JsonObject stats = json.getAsJsonObject("stats");
                        tvDone.setText(getString(R.string.lbl_formations_done)
                                + " " + stats.get("done").getAsInt());
                        tvAvgScore.setText(getString(R.string.lbl_avg_score)
                                + " " + stats.get("avg_score").getAsInt() + "%");

                        Type listType = new TypeToken<List<Enrollment>>(){}.getType();
                        List<Enrollment> enrollments = new Gson().fromJson(
                                json.getAsJsonArray("enrollments"), listType);
                        if (enrollments == null || enrollments.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            listView.setAdapter(new EnrollmentAdapter(getActivity(), enrollments));
                        }

                        if (json.has("badges")) {
                            Type badgeType = new TypeToken<List<Badge>>(){}.getType();
                            List<Badge> badges = new Gson().fromJson(json.getAsJsonArray("badges"), badgeType);

                            if (badges != null && !badges.isEmpty()) {
                                badgeList.clear();
                                badgeList.addAll(badges);
                                badgeAdapter.notifyDataSetChanged();
                            }
                        }

                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });

        btnShowBadges = view.findViewById(R.id.btn_show_badges);
        lvBadges = view.findViewById(R.id.lv_badges_profil); // Ton ID actuel

        btnShowBadges.setOnClickListener(v -> {
            if (!isVisible) {
                // 1. On lance la récupération des données
                fetchBadges(token);

                // 2. On affiche la liste et on change le texte du bouton
                lvBadges.setVisibility(View.VISIBLE);
                btnShowBadges.setText("Cacher mes badges");
                isVisible = true;
            } else {
                // On cache tout
                lvBadges.setVisibility(View.GONE);
                btnShowBadges.setText("Voir mes badges obtenus");
                isVisible = false;
            }
        });

        return view;
    }


    private void fetchBadges(String token) {

        progressBar.setVisibility(View.VISIBLE);

        Ion.with(this).load("POST", Config.BASE_URL + "getBadges.php")
                .setBodyParameter("token", token)
                .asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) return;

                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) return;

                        Type listType = new TypeToken<List<Badge>>(){}.getType();
                        List<Badge> badges = new Gson().fromJson(json.getAsJsonArray("badges"), listType);

                        if (badges != null && !badges.isEmpty()) {
                            // On utilise notre nouvelle listView spécifique aux badges
                            listViewBadges.setAdapter(new BadgeAdapter(getActivity(), badges));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
    }
}
