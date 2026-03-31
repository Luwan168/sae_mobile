package sae.openminds.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Type;
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.ActualiteAdapter;
import sae.openminds.models.Actualite;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/ActualitesFragment.java
// ============================================================
public class ActualitesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        Type listType = new TypeToken<List<Actualite>>(){}.getType();
                        List<Actualite> items = new Gson().fromJson(json.getAsJsonArray("actualites"), listType);
                        if (items == null || items.isEmpty()) { tvEmpty.setVisibility(View.VISIBLE); return; }
                        listView.setAdapter(new ActualiteAdapter(getActivity(), items));
                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
        return view;
    }
}
