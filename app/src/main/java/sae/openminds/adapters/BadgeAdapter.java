package sae.openminds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sae.openminds.R;
import sae.openminds.models.Badge;

// ============================================================
//  app/src/main/java/sae/openminds/adapters/BadgeAdapter.java
// ============================================================
public class BadgeAdapter extends ArrayAdapter<Badge> {

    public BadgeAdapter(Context context, List<Badge> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_badge, parent, false);

        Badge b = getItem(position);
        if (b == null) return convertView;

        ((TextView) convertView.findViewById(R.id.tvBadgeName)).setText(b.name);
        ((TextView) convertView.findViewById(R.id.tvBadgeFormation)).setText(b.formation_title);
        ((TextView) convertView.findViewById(R.id.tvBadgeDate)).setText(
                getContext().getString(R.string.obtained_on, b.obtained_at));
        return convertView;
    }
}
