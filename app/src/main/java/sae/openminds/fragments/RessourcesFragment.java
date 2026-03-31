package sae.openminds.fragments;

import android.app.AlertDialog;
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
import sae.openminds.adapters.RessourceAdapter;
import sae.openminds.models.Ressource;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/RessourcesFragment.java
// ============================================================
public class RessourcesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ressources, container, false);

        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        ListView    listView    = view.findViewById(R.id.listViewRessources);
        TextView    tvEmpty     = view.findViewById(R.id.tvEmpty);

        String token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getRessources.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        Type listType = new TypeToken<List<Ressource>>(){}.getType();
                        List<Ressource> items = new Gson().fromJson(json.getAsJsonArray("ressources"), listType);
                        if (items == null || items.isEmpty()) { tvEmpty.setVisibility(View.VISIBLE); return; }
                        RessourceAdapter adapter = new RessourceAdapter(getActivity(), items);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener((parent, v, pos, id) -> {
                            Ressource r = items.get(pos);
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(r.title)
                                    .setMessage(r.content)
                                    .setPositiveButton(getString(R.string.btn_ok), null)
                                    .show();
                        });
                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
        return view;
    }
}
