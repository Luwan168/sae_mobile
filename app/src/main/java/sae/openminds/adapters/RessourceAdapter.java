package sae.openminds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

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

        ((TextView) convertView.findViewById(R.id.tvRessourceTitle)).setText(r.title);
        ((TextView) convertView.findViewById(R.id.tvRessourceTheme)).setText(r.theme);
        String typeLabel = "guide".equals(r.type)
                ? getContext().getString(R.string.lbl_guide)
                : getContext().getString(R.string.lbl_article);
        ((TextView) convertView.findViewById(R.id.tvRessourceType)).setText(typeLabel);
        return convertView;
    }
}
