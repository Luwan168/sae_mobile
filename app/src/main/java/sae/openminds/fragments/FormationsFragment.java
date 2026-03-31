package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.FormationAdapter;
import sae.openminds.models.Formation;

// ============================================================
//  FormationsFragment — formations en présentiel uniquement
//  Règles métier :
//    - Places limitées : inscription bloquée si complet
//    - Max 3 formations actives par bénévole
//    - Ne peut pas s'inscrire à sa propre formation
// ============================================================
public class FormationsFragment extends Fragment {

    private ProgressBar     progressBar;
    private ListView        listView;
    private TextView        tvEmpty;
    private String          token;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_formations, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        listView    = view.findViewById(R.id.listViewFormations);
        tvEmpty     = view.findViewById(R.id.tvEmpty);
        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");
        fetchFormations();
        return view;
    }

    private void fetchFormations() {
        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getFormations.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) {
                            tvEmpty.setVisibility(View.VISIBLE); return;
                        }
                        Type listType = new TypeToken<List<Formation>>(){}.getType();
                        List<Formation> formations = new Gson().fromJson(
                                json.getAsJsonArray("formations"), listType);
                        if (formations == null || formations.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE); return;
                        }
                        listView.setAdapter(new FormationAdapter(getActivity(), formations));
                        listView.setOnItemClickListener((parent, v, pos, id) ->
                                showFormationDialog(formations.get(pos)));
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showFormationDialog(Formation f) {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.lbl_theme)).append(" ").append(f.theme).append("\n");
        if (f.location != null && !f.location.isEmpty()) {
            msg.append(getString(R.string.lbl_location)).append(" ").append(f.location).append("\n");
        }
        // Affichage des places
        if (f.max_places > 0) {
            if (f.is_full) {
                msg.append(getString(R.string.lbl_places_full)).append("\n");
            } else {
                msg.append(getString(R.string.lbl_places_left, f.places_left, f.max_places)).append("\n");
            }
        }
        if (f.description != null && !f.description.isEmpty()) {
            msg.append("\n").append(f.description);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(f.title)
                .setMessage(msg.toString())
                .setNegativeButton(getString(R.string.btn_cancel), null);

        // Bouton inscription masqué si formation complète
        if (!f.is_full) {
            builder.setPositiveButton(getString(R.string.btn_enroll),
                    (dialog, which) -> enrollFormation(f));
        }
        builder.show();
    }

    private void enrollFormation(Formation f) {
        Ion.with(this).load("POST", Config.BASE_URL + "enrollFormation.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(f.id))
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json   = JsonParser.parseString(result).getAsJsonObject();
                        String     status = json.get("status").getAsString();
                        String     msg;
                        switch (status) {
                            case "success":
                                msg = getString(R.string.enroll_success);
                                fetchFormations(); // rafraîchir les places
                                break;
                            case "already_enrolled":
                                msg = getString(R.string.already_enrolled); break;
                            case "own_formation":
                                msg = getString(R.string.err_own_formation); break;
                            case "formation_full":
                                msg = getString(R.string.err_formation_full); break;
                            case "too_many_active":
                                int max = json.has("max") ? json.get("max").getAsInt() : 3;
                                msg = getString(R.string.err_too_many_active, max); break;
                            default:
                                msg = getString(R.string.err_server); break;
                        }
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
