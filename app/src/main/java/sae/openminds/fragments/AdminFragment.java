package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import sae.openminds.Config;
import sae.openminds.R;

// ============================================================
//  AdminFragment — Admin uniquement
//  Stats globales + validation des tips + modération
// ============================================================
public class AdminFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalFormations, tvTotalEnrollments, tvAvgScore;
    private String   token;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        tvTotalUsers       = view.findViewById(R.id.tvTotalUsers);
        tvTotalFormations  = view.findViewById(R.id.tvTotalFormations);
        tvTotalEnrollments = view.findViewById(R.id.tvTotalEnrollments);
        tvAvgScore         = view.findViewById(R.id.tvGlobalAvgScore);
        Button btnModerer  = view.findViewById(R.id.btnModerBonnesPratiques);
        Button btnValidate = view.findViewById(R.id.btnValidateTips);

        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        fetchStats();
        btnModerer.setOnClickListener(v -> showModerationDialog());
        btnValidate.setOnClickListener(v -> showPendingTipsDialog());
        return view;
    }

    // ── Statistiques ─────────────────────────────────────────
    private void fetchStats() {
        Ion.with(this).load("POST", Config.BASE_URL + "getStats.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!json.getString("status").equals("success")) return;
                        JSONObject stats = json.getJSONObject("stats");
                        tvTotalUsers.setText(getString(R.string.admin_total_users)       + " " + stats.getInt("total_users"));
                        tvTotalFormations.setText(getString(R.string.admin_total_formations)   + " " + stats.getInt("total_formations"));
                        tvTotalEnrollments.setText(getString(R.string.admin_total_enrollments) + " " + stats.getInt("total_enrollments"));
                        tvAvgScore.setText(getString(R.string.admin_avg_score)           + " " + stats.getInt("avg_score") + "%");
                    } catch (Exception ignored) {}
                });
    }

    // ── Validation des tips soumis par les utilisateurs ──────
    private void showPendingTipsDialog() {
        Ion.with(this).load("POST", Config.BASE_URL + "getPendingTips.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray  list = json.getJSONArray("pratiques");

                        if (list.length() == 0) {
                            Toast.makeText(getActivity(), getString(R.string.no_pending_tips), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Afficher un par un
                        showNextTip(list, 0);

                    } catch (Exception ignored) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNextTip(JSONArray list, int index) {
        if (index >= list.length()) {
            Toast.makeText(getActivity(), getString(R.string.no_pending_tips), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject tip    = list.getJSONObject(index);
            int        id     = tip.getInt("id");
            String     content = tip.getString("content");
            String     by     = tip.optString("submitted_by_name", "?");

            String message = content + "\n\n— Soumis par : " + by
                    + "\n\n(" + (index + 1) + "/" + list.length() + ")";

            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.pending_tips_title))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.btn_approve), (dialog, which) -> {
                        validateTip(id, "approve");
                        showNextTip(list, index + 1);
                    })
                    .setNegativeButton(getString(R.string.btn_reject), (dialog, which) -> {
                        validateTip(id, "reject");
                        showNextTip(list, index + 1);
                    })
                    .setNeutralButton(getString(R.string.btn_cancel), null)
                    .show();
        } catch (Exception ignored) {}
    }

    private void validateTip(int id, String action) {
        Ion.with(this).load("POST", Config.BASE_URL + "validateTip.php")
                .setBodyParameter("token",  token)
                .setBodyParameter("id",     String.valueOf(id))
                .setBodyParameter("action", action)
                .asString().setCallback((e, r) -> {});
    }

    // ── Activation / désactivation des tips existants ────────
    private void showModerationDialog() {
        Ion.with(this).load("POST", Config.BASE_URL + "getAllBonnesPratiques.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray  list = json.getJSONArray("pratiques");

                        String[]  labels  = new String[list.length()];
                        boolean[] checked = new boolean[list.length()];
                        int[]     ids     = new int[list.length()];

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject p = list.getJSONObject(i);
                            ids[i]     = p.getInt("id");
                            String c   = p.getString("content");
                            labels[i]  = c.substring(0, Math.min(60, c.length()));
                            checked[i] = p.getInt("active") == 1;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.admin_moderate_title))
                                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) ->
                                        Ion.with(this).load("POST", Config.BASE_URL + "moderatePratique.php")
                                                .setBodyParameter("token",  token)
                                                .setBodyParameter("id",     String.valueOf(ids[which]))
                                                .setBodyParameter("active", isChecked ? "1" : "0")
                                                .asString().setCallback((e2, r) -> {}))
                                .setPositiveButton(getString(R.string.btn_ok), null)
                                .show();
                    } catch (Exception ignored) {}
                });
    }
}
