package sae.openminds.fragments;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/ActualitesFragment.java
// ============================================================

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.ActualiteAdapter;
import sae.openminds.models.Actualite;

public class ActualitesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actualites, container, false);

        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        ListView    listView    = view.findViewById(R.id.listViewActualites);
        TextView    tvEmpty     = view.findViewById(R.id.tvEmpty);

        String token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getActualites.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        Type listType = new TypeToken<List<Actualite>>(){}.getType();
                        List<Actualite> items = new Gson()
                                .fromJson(json.getAsJsonArray("actualites"), listType);
                        if (items == null || items.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        listView.setAdapter(new ActualiteAdapter(getActivity(), items));

                        // ── Clic sur un item → dialog contenu complet ──
                        listView.setOnItemClickListener((parent, v, position, id) -> {
                            Actualite a = (Actualite) parent.getItemAtPosition(position);
                            if (a != null) showDetailDialog(a);
                        });

                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
        return view;
    }

    // ── Dialog détail de l'actualité ─────────────────────────

    private void showDetailDialog(Actualite a) {
        // ScrollView pour les longs textes
        ScrollView scrollView = new ScrollView(getActivity());
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);
        scrollView.addView(layout);

        // Date + auteur
        String meta = formatDate(a.published_at);
        if (a.author != null && !a.author.trim().isEmpty()
                && !a.author.trim().equalsIgnoreCase("null null")) {
            meta += "  •  " + a.author.trim();
        }
        if (!meta.isEmpty()) {
            TextView tvMeta = new TextView(getActivity());
            tvMeta.setText(meta);
            tvMeta.setTextSize(12f);
            tvMeta.setTextColor(getResources().getColor(R.color.text_secondary, null));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 16;
            tvMeta.setLayoutParams(lp);
            layout.addView(tvMeta);
        }

        // Contenu complet (sans maxLines)
        TextView tvContent = new TextView(getActivity());
        tvContent.setText(a.content != null ? a.content : "");
        tvContent.setTextSize(15f);
        tvContent.setTextColor(getResources().getColor(R.color.text_primary, null));
        tvContent.setLineSpacing(0, 1.4f);
        layout.addView(tvContent);

        // Image (si présente)
        if (a.image_url != null && !a.image_url.trim().isEmpty()) {
            ImageView iv = new ImageView(getActivity());
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lpImg.topMargin = 24;
            iv.setLayoutParams(lpImg);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            layout.addView(iv);
            Ion.with(getContext()).load(a.image_url.trim()).intoImageView(iv);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(a.title)
                .setView(scrollView)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show();
    }

    // ── Formatage de la date ──────────────────────────────────

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            SimpleDateFormat sdfIn  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRENCH);
            SimpleDateFormat sdfOut = new SimpleDateFormat("d MMM yyyy", Locale.FRENCH);
            Date d = sdfIn.parse(raw);
            return d != null ? sdfOut.format(d) : raw;
        } catch (ParseException e) {
            return raw;
        }
    }
}