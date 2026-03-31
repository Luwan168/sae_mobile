package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.FormationAdapter;
import sae.openminds.models.Formation;

// ============================================================
//  CarteFragment — Formations en présentiel proches de chez moi
//  Filtre par ville de l'utilisateur + Filtre par thème
// ============================================================
public class CarteFragment extends Fragment {

    private ProgressBar     progressBar;
    private ListView        listView;
    private TextView        tvEmpty, tvCityInfo;
    private Spinner         spFilterTheme;
    private String          token;
    private List<Formation> allFormations = new ArrayList<>();
    private String[]        filterThemes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carte, container, false);

        progressBar   = view.findViewById(R.id.progressBar);
        listView      = view.findViewById(R.id.listViewCarte);
        tvEmpty       = view.findViewById(R.id.tvEmpty);
        tvCityInfo    = view.findViewById(R.id.tvCityInfo);
        spFilterTheme = view.findViewById(R.id.spFilterTheme);
        Button btnUpdateCity = view.findViewById(R.id.btnUpdateCity);

        token = requireActivity().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE).getString(Config.KEY_TOKEN, "");

        btnUpdateCity.setOnClickListener(v -> showUpdateCityDialog());

        setupThemeFilter();
        fetchNearby();
        return view;
    }

    private void setupThemeFilter() {
        filterThemes = new String[]{
                getString(R.string.theme_all),
                getString(R.string.theme_environment),
                getString(R.string.theme_inclusion)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, filterThemes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterTheme.setAdapter(adapter);

        spFilterTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        if (allFormations == null) return;
        
        int selectedPos = spFilterTheme.getSelectedItemPosition();
        if (selectedPos == 0) { // All themes
            listView.setAdapter(new FormationAdapter(getActivity(), allFormations));
            tvEmpty.setVisibility(allFormations.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        String selectedTheme = filterThemes[selectedPos];
        List<Formation> filtered = new ArrayList<>();
        for (Formation f : allFormations) {
            if (f.theme != null && f.theme.equalsIgnoreCase(selectedTheme)) {
                filtered.add(f);
            }
        }
        listView.setAdapter(new FormationAdapter(getActivity(), filtered));
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchNearby() {
        progressBar.setVisibility(View.VISIBLE);
        listView.setAdapter(null);
        tvEmpty.setVisibility(View.GONE);

        Ion.with(this).load("POST", Config.BASE_URL + "getFormationsProches.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json    = JsonParser.parseString(result).getAsJsonObject();
                        String     city    = json.has("user_city") && !json.get("user_city").isJsonNull()
                                             ? json.get("user_city").getAsString() : null;
                        boolean    noNearby = json.has("no_nearby") && json.get("no_nearby").getAsBoolean();

                        if (city == null) {
                            tvCityInfo.setText(getString(R.string.carte_no_city));
                        } else if (noNearby) {
                            tvCityInfo.setText(getString(R.string.carte_no_nearby, city));
                        } else {
                            tvCityInfo.setText(getString(R.string.carte_near, city));
                        }
                        tvCityInfo.setVisibility(View.VISIBLE);

                        Type listType = new TypeToken<List<Formation>>(){}.getType();
                        allFormations = new Gson().fromJson(json.getAsJsonArray("formations"), listType);

                        if (allFormations == null || allFormations.isEmpty()) {
                            allFormations = new ArrayList<>();
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        applyFilter();
                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showUpdateCityDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        EditText etCity    = new EditText(getActivity()); etCity.setHint(getString(R.string.hint_city)); layout.addView(etCity);
        EditText etAddress = new EditText(getActivity()); etAddress.setHint(getString(R.string.hint_address)); layout.addView(etAddress);

        // Pré-remplir avec les valeurs existantes
        SharedPreferences prefs = requireActivity().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String savedCity    = prefs.getString("city", "");
        String savedAddress = prefs.getString("address", "");
        etCity.setText(savedCity);
        etAddress.setText(savedAddress);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.update_city_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                    String city    = etCity.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    if (city.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Sauvegarder localement
                    prefs.edit().putString("city", city).putString("address", address).apply();

                    Ion.with(this).load("POST", Config.BASE_URL + "updateProfile.php")
                            .setBodyParameter("token",   token)
                            .setBodyParameter("city",    city)
                            .setBodyParameter("address", address)
                            .asString()
                            .setCallback((e, result) -> {
                                Toast.makeText(getActivity(), getString(R.string.city_updated), Toast.LENGTH_SHORT).show();
                                fetchNearby();
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }
}
