package sae.openminds.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sae.openminds.R;
import sae.openminds.models.Enrollment;

// ============================================================
//  app/src/main/java/sae/openminds/adapters/EnrollmentAdapter.java
// ============================================================
public class EnrollmentAdapter extends ArrayAdapter<Enrollment> {

    public EnrollmentAdapter(Context context, List<Enrollment> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_enrollment, parent, false);

        Enrollment e = getItem(position);
        if (e == null) return convertView;

        TextView tvTitle  = convertView.findViewById(R.id.tvEnrollTitle);
        TextView tvStatus = convertView.findViewById(R.id.tvEnrollStatus);
        TextView tvScore  = convertView.findViewById(R.id.tvEnrollScore);

        tvTitle.setText(e.title);
        tvStatus.setText(e.status);

        // Couleur selon le statut
        switch (e.status) {
            case "termine":
                tvStatus.setTextColor(getContext().getColor(R.color.status_done));
                tvScore.setVisibility(View.VISIBLE);
                tvScore.setText(e.score + "%");
                break;
            case "abandonne":
                tvStatus.setTextColor(getContext().getColor(R.color.status_abandoned));
                tvScore.setVisibility(View.GONE);
                break;
            default:
                tvStatus.setTextColor(getContext().getColor(R.color.status_ongoing));
                tvScore.setVisibility(View.GONE);
        }
        return convertView;
    }
}
