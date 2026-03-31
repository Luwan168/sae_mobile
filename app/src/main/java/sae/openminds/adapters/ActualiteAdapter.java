package sae.openminds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sae.openminds.R;
import sae.openminds.models.Actualite;

// ============================================================
//  app/src/main/java/sae/openminds/adapters/ActualiteAdapter.java
// ============================================================
public class ActualiteAdapter extends ArrayAdapter<Actualite> {

    public ActualiteAdapter(Context context, List<Actualite> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_actualite, parent, false);

        Actualite a = getItem(position);
        if (a == null) return convertView;

        ((TextView) convertView.findViewById(R.id.tvActualiteTitle)).setText(a.title);
        ((TextView) convertView.findViewById(R.id.tvActualiteDate)).setText(a.published_at);
        ((TextView) convertView.findViewById(R.id.tvActualiteContent)).setText(a.content);
        return convertView;
    }
}
