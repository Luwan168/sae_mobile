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
//  app/src/main/java/sae/openminds/adapters/FormationAdapter.java
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
        TextView tvType   = convertView.findViewById(R.id.tvFormationType);

        tvTitle.setText(f.title);
        tvTheme.setText(f.theme);

        String typeLabel = "presentiel".equals(f.type)
                ? getContext().getString(R.string.lbl_type_presential)
                : getContext().getString(R.string.lbl_type_online);
        tvType.setText(typeLabel);

        return convertView;
    }
}
