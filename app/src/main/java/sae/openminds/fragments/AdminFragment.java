package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    private void showModerationDialog() {
        Ion.with(this).load("POST", Config.BASE_URL + "getAllBonnesPratiques.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray  list = json.getJSONArray("pratiques");

                        List<JSONObject> tipsList = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) tipsList.add(list.getJSONObject(i));

                        ListView lv = new ListView(getActivity());
                        TipsModerationAdapter adapter = new TipsModerationAdapter(getActivity(), tipsList);
                        lv.setAdapter(adapter);

                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.admin_moderate_title))
                                .setView(lv)
                                .setPositiveButton(getString(R.string.btn_ok), null)
                                .show();
                    } catch (Exception ignored) {}
                });
    }

    private class TipsModerationAdapter extends BaseAdapter {
        private final Context          context;
        private final List<JSONObject> items;

        public TipsModerationAdapter(Context context, List<JSONObject> items) {
            this.context = context;
            this.items   = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_tip_moderation, parent, false);
            }
            try {
                JSONObject tip = items.get(position);
                int    id      = tip.getInt("id");
                String content = tip.getString("content");
                boolean active = tip.getInt("active") == 1;

                TextView  tv      = convertView.findViewById(R.id.tvTipContent);
                CheckBox  cb      = convertView.findViewById(R.id.cbActive);
                ImageButton btnDel = convertView.findViewById(R.id.btnDeleteTip);

                tv.setText(content);
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(active);

                cb.setOnCheckedChangeListener((btn, isChecked) -> 
                    Ion.with(AdminFragment.this).load("POST", Config.BASE_URL + "moderatePratique.php")
                            .setBodyParameter("token",  token)
                            .setBodyParameter("id",     String.valueOf(id))
                            .setBodyParameter("active", isChecked ? "1" : "0")
                            .setBodyParameter("action", "moderate")
                            .asString().setCallback((e, r) -> {
                                if (e == null) try { tip.put("active", isChecked ? 1 : 0); } catch(Exception ignored){}
                            }));

                btnDel.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.dialog_delete_title))
                            .setMessage(getString(R.string.dialog_delete_msg))
                            .setPositiveButton(getString(R.string.btn_confirm_delete), (dialog, which) -> {
                                Ion.with(AdminFragment.this).load("POST", Config.BASE_URL + "moderatePratique.php")
                                        .setBodyParameter("token",  token)
                                        .setBodyParameter("id",     String.valueOf(id))
                                        .setBodyParameter("action", "delete")
                                        .asString().setCallback((e, r) -> {
                                            if (e == null) {
                                                Toast.makeText(context, getString(R.string.tip_deleted), Toast.LENGTH_SHORT).show();
                                                items.remove(position);
                                                notifyDataSetChanged();
                                            }
                                        });
                            })
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show();
                });

            } catch (Exception ignored) {}
            return convertView;
        }
    }
}
