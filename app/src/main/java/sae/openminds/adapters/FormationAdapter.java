package sae.openminds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sae.openminds.R;
import sae.openminds.models.Formation;

// ============================================================
//  FormationAdapter — sans type, avec gestion des places
// ============================================================
public class FormationAdapter extends ArrayAdapter<Formation> {

    public FormationAdapter(Context context, List<Formation> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_formation, parent, false);
        }
        Formation f = getItem(position);
        if (f == null) return convertView;

        TextView tvTitle  = convertView.findViewById(R.id.tvFormationTitle);
        TextView tvTheme  = convertView.findViewById(R.id.tvFormationTheme);
        TextView tvPlaces = convertView.findViewById(R.id.tvPlaces);
        TextView tvLocation = convertView.findViewById(R.id.tvFormationLocation);

        tvTitle.setText(f.title);
        tvTheme.setText(f.theme);

        // Lieu
        if (f.location != null && !f.location.isEmpty()) {
            tvLocation.setText(f.location);
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }

        // Badge places / Statut
        tvPlaces.setVisibility(View.VISIBLE);
        if (f.is_completed) {
            tvPlaces.setText(getContext().getString(R.string.btn_end_formation)); // Or "Terminée" if available
            tvPlaces.setBackgroundColor(getContext().getColor(R.color.text_secondary));
            tvPlaces.setTextColor(getContext().getColor(R.color.white));
        } else if (f.max_places > 0) {
            if (f.is_full) {
                tvPlaces.setText(getContext().getString(R.string.lbl_places_full));
                tvPlaces.setBackgroundColor(getContext().getColor(R.color.status_abandoned));
                tvPlaces.setTextColor(getContext().getColor(R.color.white));
            } else {
                tvPlaces.setText(getContext().getString(
                        R.string.lbl_places_left, f.places_left, f.max_places));
                tvPlaces.setBackgroundColor(getContext().getColor(R.color.green_accent));
                tvPlaces.setTextColor(getContext().getColor(R.color.green_primary_dark));
            }
        } else {
            tvPlaces.setVisibility(View.GONE);
        }

        // Griser si complet ou terminé
        convertView.setAlpha((f.is_full || f.is_completed) ? 0.45f : 1f);

        return convertView;
    }
}
