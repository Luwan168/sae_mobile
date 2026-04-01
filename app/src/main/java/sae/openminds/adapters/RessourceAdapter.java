package sae.openminds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.models.Ressource;

// ============================================================
//  app/src/main/java/sae/openminds/adapters/RessourceAdapter.java
// ============================================================
public class RessourceAdapter extends ArrayAdapter<Ressource> {

    public RessourceAdapter(Context context, List<Ressource> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_ressource, parent, false);

        Ressource r = getItem(position);
        if (r == null) return convertView;

        TextView tvTitle = convertView.findViewById(R.id.tvRessourceTitle);
        TextView tvTheme = convertView.findViewById(R.id.tvRessourceTheme);
        TextView tvType  = convertView.findViewById(R.id.tvRessourceType);
        ImageView ivMedia = convertView.findViewById(R.id.ivRessourceMedia);

        tvTitle.setText(r.title);
        tvTheme.setText(r.theme);
        
        String typeLabel = "guide".equals(r.type)
                ? getContext().getString(R.string.lbl_guide)
                : getContext().getString(R.string.lbl_article);
        tvType.setText(typeLabel);

        // Gestion de l'image
        if (r.image_url != null && !r.image_url.isEmpty()) {
            ivMedia.setVisibility(View.VISIBLE);
            String fullUrl = Config.BASE_URL + r.image_url;
            Ion.with(ivMedia)
                    .placeholder(R.drawable.ic_image_placeholder) // Utilisation de l'icône de placeholder du projet
                    .error(R.drawable.ic_image_placeholder)       // Utilisation de l'icône de placeholder en cas d'erreur
                    .load(fullUrl);
        } else {
            ivMedia.setVisibility(View.GONE);
        }

        return convertView;
    }
}
