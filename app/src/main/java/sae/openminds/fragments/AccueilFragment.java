package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.TipWorker;

// ============================================================
//  AccueilFragment — Page d'accueil enrichie
// ============================================================
public class AccueilFragment extends Fragment {

    private TextView tvWelcome, tvRole, tvTip, tvFormationsCount, tvBadgesCount, tvProgressStat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accueil, container, false);

        tvWelcome       = view.findViewById(R.id.tvWelcome);
        tvRole          = view.findViewById(R.id.tvRole);
        tvTip           = view.findViewById(R.id.tvTip);
        tvFormationsCount = view.findViewById(R.id.tvFormationsCount);
        tvBadgesCount   = view.findViewById(R.id.tvBadgesCount);
        tvProgressStat  = view.findViewById(R.id.tvProgressStat);
        Button btnSubmitTip = view.findViewById(R.id.btnSubmitTip);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String token     = prefs.getString(Config.KEY_TOKEN, "");
        String firstname = prefs.getString(Config.KEY_FIRSTNAME, "");
        String role      = prefs.getString(Config.KEY_ROLE, Config.ROLE_BENEVOLE);

        tvWelcome.setText(getString(R.string.welcome_message, firstname));

        String roleLabel;
        switch (role) {
            case Config.ROLE_ADMIN:     roleLabel = getString(R.string.role_admin);     break;
            case Config.ROLE_FORMATEUR: roleLabel = getString(R.string.role_formateur); break;
            default:                    roleLabel = getString(R.string.role_benevole);  break;
        }
        tvRole.setText(roleLabel);

        btnSubmitTip.setOnClickListener(v -> showSubmitTipDialog(token));
        fetchTip();
        fetchStats(token);
        scheduleTipNotifications();
        return view;
    }

    private void fetchTip() {
        Ion.with(this).load("GET", Config.BASE_URL + "getBonnesPratiques.php")
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.getString("status").equals("success"))
                            tvTip.setText(json.getJSONObject("pratique").getString("content"));
                    } catch (Exception ignored) {}
                });
    }

    private void fetchStats(String token) {
        Ion.with(this).load("POST", Config.BASE_URL + "getProgression.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json  = new JSONObject(result);
                        JSONObject stats = json.getJSONObject("stats");
                        int total    = stats.getInt("total");
                        int done     = stats.getInt("done");
                        int avgScore = stats.getInt("avg_score");

                        tvFormationsCount.setText(getString(R.string.stat_formations, total));
                        tvBadgesCount.setText(getString(R.string.stat_done, done));
                        tvProgressStat.setText(done > 0
                                ? getString(R.string.stat_avg, avgScore)
                                : getString(R.string.stat_start));
                    } catch (Exception ignored) {}
                });
    }

    private void showSubmitTipDialog(String token) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        EditText etContent = new EditText(getActivity());
        etContent.setHint(getString(R.string.hint_tip_content));
        etContent.setMinLines(2);
        layout.addView(etContent);

        EditText etTheme = new EditText(getActivity());
        etTheme.setHint(getString(R.string.hint_tip_theme));
        layout.addView(etTheme);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.submit_tip_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_submit_tip), (dialog, which) -> {
                    String content = etContent.getText().toString().trim();
                    String theme   = etTheme.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Ion.with(this).load("POST", Config.BASE_URL + "submitTip.php")
                            .setBodyParameter("token",   token)
                            .setBodyParameter("content", content)
                            .setBodyParameter("theme",   theme.isEmpty() ? "general" : theme)
                            .asString()
                            .setCallback((e, result) -> {
                                if (e != null || result == null) return;
                                try {
                                    String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    Toast.makeText(getActivity(),
                                            status.equals("success") ? getString(R.string.tip_submitted) : getString(R.string.err_server),
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception ignored) {}
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void scheduleTipNotifications() {
        // Pour un test toutes les 2 minutes (OneTimeWorkRequest récurrente)
        PeriodicWorkRequest tipRequest = new PeriodicWorkRequest.Builder(TipWorker.class,
                15, TimeUnit.MINUTES) // Minimum légal 15 min
                .addTag("TIP_WORKER")
                .build();


        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "UniqueTipWork",
                ExistingPeriodicWorkPolicy.KEEP, // TRÈS IMPORTANT : ne redémarre pas le cycle si déjà actif
                tipRequest
        );
    }
}
