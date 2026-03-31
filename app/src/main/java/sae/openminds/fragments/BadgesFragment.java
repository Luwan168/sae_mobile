package sae.openminds.fragments;

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
import sae.openminds.adapters.BadgeAdapter;
import sae.openminds.models.Badge;
import sae.openminds.models.Enrollment;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/BadgesFragment.java
// ============================================================
public class BadgesFragment extends Fragment {

    private ProgressBar progressBar;
    private ListView    listView;
    private TextView    tvEmpty, tvDone, tvAvgScore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        progressBar  = view.findViewById(R.id.progressBar);
        listView     = view.findViewById(R.id.listViewBadges);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        tvDone       = view.findViewById(R.id.tvDone);
        tvAvgScore   = view.findViewById(R.id.tvAvgScore);

        String token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        fetchProgression(token);
        fetchBadges(token);
        return view;
    }

    private void fetchProgression(String token) {
        Ion.with(this).load("POST", Config.BASE_URL + "getProgression.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JsonObject json  = JsonParser.parseString(result).getAsJsonObject();
                        JsonObject stats = json.getAsJsonObject("stats");
                        tvDone.setText(getString(R.string.lbl_formations_done)
                                + " " + stats.get("done").getAsInt());
                        tvAvgScore.setText(getString(R.string.lbl_avg_score)
                                + " " + stats.get("avg_score").getAsInt() + "%");
                    } catch (Exception ignored) {}
                });
    }

    private void fetchBadges(String token) {
        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getBadges.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) return;

                        Type listType = new TypeToken<List<Badge>>(){}.getType();
                        List<Badge> badges = new Gson().fromJson(json.getAsJsonArray("badges"), listType);

                        if (badges == null || badges.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        listView.setAdapter(new BadgeAdapter(getActivity(), badges));
                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }
}
