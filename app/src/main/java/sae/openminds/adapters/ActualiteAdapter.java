package sae.openminds.adapters;

// ============================================================
//  app/src/main/java/sae/openminds/adapters/ActualiteAdapter.java
//  Affiche logo OpenMinds OU image distante selon image_url
// ============================================================

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sae.openminds.R;
import sae.openminds.models.Actualite;

public class ActualiteAdapter extends ArrayAdapter<Actualite> {

    private static final SimpleDateFormat SDF_IN  =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRENCH);
    private static final SimpleDateFormat SDF_OUT =
            new SimpleDateFormat("d MMM yyyy", Locale.FRENCH);

    public ActualiteAdapter(Context context, List<Actualite> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_actualite, parent, false);
        }

        Actualite a = getItem(position);
        if (a == null) return convertView;

        TextView   tvTitle   = convertView.findViewById(R.id.tvActualiteTitle);
        TextView   tvContent = convertView.findViewById(R.id.tvActualiteContent);
        TextView   tvDate    = convertView.findViewById(R.id.tvActualiteDate);
        TextView   tvAuthor  = convertView.findViewById(R.id.tvActualiteAuthor);
        FrameLayout vignette = convertView.findViewById(R.id.vignetteLogo);
        ImageView  ivImage   = convertView.findViewById(R.id.ivActualiteImage);

        // Textes
        tvTitle.setText(a.title != null ? a.title : "");
        tvContent.setText(a.content != null ? a.content : "");
        tvDate.setText(formatDate(a.published_at));

        // Auteur
        if (a.author != null && !a.author.trim().isEmpty()
                && !a.author.trim().equalsIgnoreCase("null null")) {
            tvAuthor.setText(a.author.trim());
            tvAuthor.setVisibility(View.VISIBLE);
        } else {
            tvAuthor.setVisibility(View.GONE);
        }

        // Image ou logo
        if (a.image_url != null && !a.image_url.trim().isEmpty()) {
            // On a une image → masquer le logo, afficher l'ImageView
            vignette.setVisibility(View.GONE);
            ivImage.setVisibility(View.VISIBLE);
            // Ion gère le téléchargement + cache automatiquement
            Ion.with(getContext())
                    .load(a.image_url.trim())
                    .intoImageView(ivImage);
        } else {
            // Pas d'image → afficher le logo OpenMinds
            vignette.setVisibility(View.VISIBLE);
            ivImage.setVisibility(View.GONE);
            // Réinitialiser l'ImageView pour éviter le recyclage de vues
            ivImage.setImageDrawable(null);
        }

        return convertView;
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            Date d = SDF_IN.parse(raw);
            return d != null ? SDF_OUT.format(d) : raw;
        } catch (ParseException e) {
            return raw;
        }
    }
}