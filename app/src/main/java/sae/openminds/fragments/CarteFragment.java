package sae.openminds.fragments;

// ============================================================
//  CarteFragment — Carte interactive + liste des formations proches
//  • Carte OSMDroid (zoom/dézoom, marqueurs cliquables)
//  • Liste défilable en dessous (FormationAdapter existant)
//  • Géocodage Nominatim (OpenStreetMap, pas de clé API)
//  • Popup fiche formation au clic sur un marqueur
// ============================================================

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.FormationAdapter;
import sae.openminds.models.Formation;

public class CarteFragment extends Fragment {

    // ── Vues ──────────────────────────────────────────────────
    private MapView              mapView;
    private ProgressBar          progressBar;
    private ListView             listView;
    private TextView             tvEmpty, tvCityInfo;
    private CardView             cardFormation;
    private TextView             tvPopupTitle, tvPopupTheme, tvPopupLocation, tvPopupDescription;
    private Button               btnPopupEnroll;
    private FloatingActionButton fabMyLocation;
    private Spinner              spFilterTheme;

    // ── État ──────────────────────────────────────────────────
    private String               token;
    private MyLocationNewOverlay myLocationOverlay;
    private final ExecutorService executor   = Executors.newCachedThreadPool();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());
    private List<Formation>      allFormations = new ArrayList<>();

    private static final int REQUEST_LOCATION = 42;

    // ── Cycle de vie ──────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Initialisation obligatoire d'OSMDroid
        Configuration.getInstance().load(
                requireContext(),
                requireActivity().getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_carte, container, false);

        // Références vues
        mapView            = view.findViewById(R.id.mapView);
        progressBar        = view.findViewById(R.id.progressBar);
        listView           = view.findViewById(R.id.listViewCarte);
        tvEmpty            = view.findViewById(R.id.tvEmpty);
        tvCityInfo         = view.findViewById(R.id.tvCityInfo);
        cardFormation      = view.findViewById(R.id.cardFormation);
        tvPopupTitle       = view.findViewById(R.id.tvPopupTitle);
        tvPopupTheme       = view.findViewById(R.id.tvPopupTheme);
        tvPopupLocation    = view.findViewById(R.id.tvPopupLocation);
        tvPopupDescription = view.findViewById(R.id.tvPopupDescription);
        btnPopupEnroll     = view.findViewById(R.id.btnPopupEnroll);
        fabMyLocation      = view.findViewById(R.id.fabMyLocation);
        spFilterTheme      = view.findViewById(R.id.spFilterTheme);
        Button     btnUpdateCity = view.findViewById(R.id.btnUpdateCity);
        ImageButton btnClose     = view.findViewById(R.id.btnClosePopup);

        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        // ── Configuration carte ────────────────────────────────
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(6.0);
        mapView.getController().setCenter(new GeoPoint(46.8, 2.3));

        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // ── Listeners ─────────────────────────────────────────
        fabMyLocation.setOnClickListener(v -> centerOnMyLocation());
        btnUpdateCity.setOnClickListener(v -> showUpdateCityDialog());
        btnClose.setOnClickListener(v -> cardFormation.setVisibility(View.GONE));

        listView.setOnItemClickListener((parent, v2, position, id) -> {
            Formation f = (Formation) parent.getItemAtPosition(position);
            showFormationPopup(f);
        });

        setupFilter();

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }

        fetchNearby();
        return view;
    }

    private void setupFilter() {
        String[] themes = {
                getString(R.string.theme_all),
                getString(R.string.theme_environment),
                getString(R.string.theme_inclusion)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, themes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterTheme.setAdapter(adapter);

        spFilterTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(themes[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter(String theme) {
        if (allFormations == null) return;
        List<Formation> filtered;
        if (theme.equals(getString(R.string.theme_all))) {
            filtered = allFormations;
        } else {
            filtered = allFormations.stream()
                    .filter(f -> f.theme != null && f.theme.equalsIgnoreCase(theme))
                    .collect(Collectors.toList());
        }
        updateUI(filtered);
    }

    private void updateUI(List<Formation> formations) {
        mapView.getOverlays().removeIf(o -> o instanceof Marker);
        if (formations.isEmpty()) {
            listView.setAdapter(null);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setAdapter(new FormationAdapter(getActivity(), formations));
            geocodeAndAddMarkers(formations);
        }
    }

    @Override public void onResume()      { super.onResume();  mapView.onResume();  }
    @Override public void onPause()       { super.onPause();   mapView.onPause();   }
    @Override public void onDestroyView() { super.onDestroyView(); executor.shutdownNow(); }

    private void fetchNearby() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        cardFormation.setVisibility(View.GONE);

        Ion.with(this)
                .load("POST", Config.BASE_URL + "getFormationsProches.php")
                .setBodyParameter("token", token)
                .asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        String city = json.has("user_city") && !json.get("user_city").isJsonNull()
                                ? json.get("user_city").getAsString() : null;
                        boolean noNearby = json.has("no_nearby") && json.get("no_nearby").getAsBoolean();

                        if (city == null) tvCityInfo.setText(getString(R.string.carte_no_city));
                        else if (noNearby) tvCityInfo.setText(getString(R.string.carte_no_nearby, city));
                        else tvCityInfo.setText(getString(R.string.carte_near, city));
                        tvCityInfo.setVisibility(View.VISIBLE);

                        Type listType = new TypeToken<List<Formation>>(){}.getType();
                        allFormations = new Gson().fromJson(json.getAsJsonArray("formations"), listType);
                        applyFilter(spFilterTheme.getSelectedItem().toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void geocodeAndAddMarkers(List<Formation> formations) {
        executor.execute(() -> {
            for (Formation f : formations) {
                if (f.location == null || f.location.trim().isEmpty()) continue;
                GeoPoint point = geocode(f.location.trim());
                if (point == null) continue;
                final GeoPoint finalPoint = point;
                mainHandler.post(() -> addMarker(f, finalPoint));
                try { Thread.sleep(1100); } catch (InterruptedException ignored) { return; }
            }
        });
    }

    private GeoPoint geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address + ", France", "UTF-8");
            String urlStr  = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", requireContext().getPackageName());
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            JSONArray results = new JSONArray(sb.toString());
            if (results.length() == 0) return null;
            JSONObject first = results.getJSONObject(0);
            return new GeoPoint(first.getDouble("lat"), first.getDouble("lon"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addMarker(Formation formation, GeoPoint point) {
        if (getActivity() == null || !isAdded()) return;
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(formation.title);
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map);
        if (icon != null) marker.setIcon(icon);
        marker.setInfoWindow(null);
        marker.setOnMarkerClickListener((m, mapV) -> {
            showFormationPopup(formation);
            return true;
        });
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void showFormationPopup(Formation f) {
        tvPopupTitle.setText(f.title != null ? f.title : "");
        tvPopupTheme.setText(f.theme != null ? f.theme : "");
        tvPopupLocation.setText(getString(R.string.lbl_location) + " " + (f.location != null ? f.location : ""));
        tvPopupDescription.setText(f.description != null ? f.description : "");
        btnPopupEnroll.setOnClickListener(v -> enrollFormation(f));
        cardFormation.setVisibility(View.VISIBLE);
    }

    private void enrollFormation(Formation f) {
        Ion.with(this)
                .load("POST", Config.BASE_URL + "enrollFormation.php")
                .setBodyParameter("token", token)
                .setBodyParameter("formation_id", String.valueOf(f.id))
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                        switch (status) {
                            case "success":
                                Toast.makeText(getActivity(), getString(R.string.enroll_success), Toast.LENGTH_SHORT).show();
                                cardFormation.setVisibility(View.GONE);
                                break;
                            case "already_enrolled":
                                Toast.makeText(getActivity(), getString(R.string.already_enrolled), Toast.LENGTH_SHORT).show();
                                break;
                            case "own_formation":
                                Toast.makeText(getActivity(), getString(R.string.err_own_formation), Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
    }

    private void centerOnMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), "Permission GPS non accordée", Toast.LENGTH_SHORT).show();
            return;
        }
        Location loc = myLocationOverlay.getLastFix();
        if (loc != null) {
            mapView.getController().animateTo(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
            mapView.getController().setZoom(13.0);
        } else {
            Toast.makeText(getActivity(), "Position GPS non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdateCityDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);
        EditText etCity = new EditText(getActivity());
        etCity.setHint(getString(R.string.hint_city));
        layout.addView(etCity);

        SharedPreferences prefs = requireActivity().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        etCity.setText(prefs.getString("city", ""));

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.update_city_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                    String city = etCity.getText().toString().trim();
                    if (city.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putString("city", city).remove("address").apply();
                    Ion.with(this)
                            .load("POST", Config.BASE_URL + "updateProfile.php")
                            .setBodyParameter("token", token)
                            .setBodyParameter("city", city)
                            .setBodyParameter("address", "")
                            .asString()
                            .setCallback((e, result) -> {
                                Toast.makeText(getActivity(), getString(R.string.city_updated), Toast.LENGTH_SHORT).show();
                                fetchNearby();
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
        }
    }
}