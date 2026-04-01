package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.FormationAdapter;
import sae.openminds.models.Formation;

// ============================================================
//  FormateurFragment — avec sélection de média pour les ressources
// ============================================================
public class FormateurFragment extends Fragment {

    private String          token;
    private ProgressBar     progressBar;
    private ListView        listView;
    private TextView        tvEmpty;
    private List<Formation> mesFormations = new ArrayList<>();

    // ── Champs pour le sélecteur de média ───────────────────
    private Uri    selectedMediaUri  = null;
    private String selectedMimeType  = null;
    private TextView tvMediaSelected = null;   // référence au TextView du dialog
    private ActivityResultLauncher<String[]> mediaPickerLauncher;

    // ── Enregistrement du launcher AVANT onCreateView ───────
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;

                    // Persister la permission de lecture
                    requireActivity().getContentResolver()
                            .takePersistableUriPermission(uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    selectedMediaUri  = uri;
                    selectedMimeType  = requireActivity().getContentResolver().getType(uri);

                    if (tvMediaSelected != null) {
                        boolean isVideo = selectedMimeType != null
                                && selectedMimeType.startsWith("video");
                        tvMediaSelected.setText(isVideo
                                ? "🎬 Vidéo sélectionnée ✓"
                                : "🖼️ Photo sélectionnée ✓");
                        tvMediaSelected.setTextColor(
                                getResources().getColor(R.color.green_primary, null));
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_formateur, container, false);

        token       = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");
        progressBar = view.findViewById(R.id.progressBar);
        listView    = view.findViewById(R.id.listViewFormations);
        tvEmpty     = view.findViewById(R.id.tvEmpty);

        view.findViewById(R.id.btnCreateFormation)
                .setOnClickListener(v -> showCreateFormationDialog());
        view.findViewById(R.id.btnAddQuestion)
                .setOnClickListener(v -> showAddQuestionDialog());
        view.findViewById(R.id.btnCreateRessource)
                .setOnClickListener(v -> {
                    // Réinitialiser le média sélectionné à chaque ouverture
                    selectedMediaUri = null;
                    selectedMimeType = null;
                    showCreateRessourceDialog();
                });

        loadMyFormations();
        return view;
    }

    // ── Charger MES formations ───────────────────────────────
    private void loadMyFormations() {
        progressBar.setVisibility(View.VISIBLE);
        Ion.with(this).load("POST", Config.BASE_URL + "getMyFormations.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) {
                            tvEmpty.setVisibility(View.VISIBLE); return;
                        }
                        Type listType = new TypeToken<List<Formation>>(){}.getType();
                        mesFormations = new Gson().fromJson(
                                json.getAsJsonArray("formations"), listType);
                        if (mesFormations == null || mesFormations.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE); return;
                        }
                        listView.setAdapter(new FormationAdapter(getActivity(), mesFormations));
                        listView.setOnItemClickListener((parent, v, pos, id) ->
                                showStudentsDialog(mesFormations.get(pos)));
                    } catch (Exception ex) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ── Voir les élèves + Terminer la formation ──────────────
    private void showStudentsDialog(Formation f) {
        Ion.with(this).load("POST", Config.BASE_URL + "getFormationStudents.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(f.id))
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json     = new JSONObject(result);
                        JSONArray  students = json.getJSONArray("students");
                        
                        StringBuilder sb = new StringBuilder();
                        if (students.length() == 0) {
                            sb.append(getString(R.string.no_students_yet));
                        } else {
                            for (int i = 0; i < students.length(); i++) {
                                JSONObject s = students.getJSONObject(i);
                                sb.append(s.getString("firstname")).append(" ")
                                        .append(s.getString("lastname"))
                                        .append(" — ").append(s.getString("status"));
                                if (!s.isNull("score"))
                                    sb.append(" (").append(s.getInt("score")).append("%)");
                                sb.append("\n");
                            }
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.students_enrolled_in, f.title))
                                .setMessage(sb.toString())
                                .setPositiveButton(getString(R.string.btn_ok), null);

                        // Bouton pour terminer la formation
                        if (!f.is_completed) {
                            builder.setNeutralButton(getString(R.string.btn_end_formation), (dialog, which) -> {
                                confirmEndFormation(f);
                            });
                        }

                        builder.show();
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmEndFormation(Formation f) {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.end_formation_title))
                .setMessage(getString(R.string.end_formation_msg))
                .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                    Ion.with(this).load("POST", Config.BASE_URL + "completeFormation.php")
                            .setBodyParameter("token", token)
                            .setBodyParameter("formation_id", String.valueOf(f.id))
                            .asString()
                            .setCallback((e, result) -> {
                                if (e != null || result == null) {
                                    Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                                    if (json.get("status").getAsString().equals("success")) {
                                        Toast.makeText(getActivity(), getString(R.string.end_formation_success), Toast.LENGTH_SHORT).show();
                                        loadMyFormations();
                                    } else {
                                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ex) {
                                    Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ── Créer une formation ──────────────────────────────────
    private void showCreateFormationDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        EditText etTitle = new EditText(getActivity());
        etTitle.setHint(getString(R.string.hint_formation_title));
        layout.addView(etTitle);

        EditText etDesc = new EditText(getActivity());
        etDesc.setHint(getString(R.string.hint_formation_description));
        etDesc.setMinLines(2);
        layout.addView(etDesc);

        // Thème — Spinner au lieu de EditText
        TextView tvThemeLabel = new TextView(getActivity());
        tvThemeLabel.setText(getString(R.string.lbl_theme));
        tvThemeLabel.setPadding(0, 16, 0, 4);
        layout.addView(tvThemeLabel);

        String[] themes = new String[Config.THEMES.length];
        for (int i = 0; i < Config.THEMES.length; i++) {
            int resId = getActivity().getResources()
                    .getIdentifier("theme_" + Config.THEMES[i], "string",
                            getActivity().getPackageName());
            themes[i] = resId != 0 ? getString(resId) : Config.THEMES[i];
        }
        Spinner spTheme = new Spinner(getActivity());
        spTheme.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, themes));
        layout.addView(spTheme);

        EditText etLocation = new EditText(getActivity());
        etLocation.setHint(getString(R.string.hint_formation_location));
        layout.addView(etLocation);

        EditText etMaxPlaces = new EditText(getActivity());
        etMaxPlaces.setHint(getString(R.string.hint_max_places));
        etMaxPlaces.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etMaxPlaces);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.create_formation_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_create_formation), (dialog, which) -> {
                    String title      = etTitle.getText().toString().trim();
                    String desc       = etDesc.getText().toString().trim();
                    String theme      = themes[spTheme.getSelectedItemPosition()];
                    String location   = etLocation.getText().toString().trim();
                    String maxPlacesS = etMaxPlaces.getText().toString().trim();

                    if (title.isEmpty() || location.isEmpty()) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Ion.with(this).load("POST", Config.BASE_URL + "createFormation.php")
                            .setBodyParameter("token",       token)
                            .setBodyParameter("title",       title)
                            .setBodyParameter("description", desc)
                            .setBodyParameter("theme",       theme)
                            .setBodyParameter("location",    location)
                            .setBodyParameter("max_places",  maxPlacesS)
                            .asString()
                            .setCallback((e, result) -> {
                                if (e != null || result == null) {
                                    Toast.makeText(getActivity(),
                                            getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    String status = JsonParser.parseString(result)
                                            .getAsJsonObject().get("status").getAsString();
                                    if (status.equals("success")) {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.formation_created),
                                                Toast.LENGTH_SHORT).show();
                                        loadMyFormations();
                                    } else {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ex) {
                                    Toast.makeText(getActivity(),
                                            getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ── Ajouter une question ─────────────────────────────────
    private void showAddQuestionDialog() {
        if (mesFormations.isEmpty()) {
            Toast.makeText(getActivity(),
                    getString(R.string.select_formation_first), Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        String[] titles = new String[mesFormations.size()];
        for (int i = 0; i < mesFormations.size(); i++) titles[i] = mesFormations.get(i).title;

        TextView tvLabel = new TextView(getActivity());
        tvLabel.setText(getString(R.string.select_formation_label));
        layout.addView(tvLabel);

        Spinner spFormation = new Spinner(getActivity());
        spFormation.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, titles));
        layout.addView(spFormation);

        EditText etQuestion = new EditText(getActivity());
        etQuestion.setHint(getString(R.string.hint_question));
        etQuestion.setPadding(0, 16, 0, 0);
        layout.addView(etQuestion);

        EditText[] choices = new EditText[4];
        for (int i = 0; i < 4; i++) {
            choices[i] = new EditText(getActivity());
            choices[i].setHint(getString(R.string.hint_choice, i + 1));
            layout.addView(choices[i]);
        }

        EditText etCorrect = new EditText(getActivity());
        etCorrect.setHint(getString(R.string.hint_correct_answer));
        layout.addView(etCorrect);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.add_question_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.btn_add_question), (dialog, which) -> {
                    int    formId = mesFormations.get(spFormation.getSelectedItemPosition()).id;
                    String q      = etQuestion.getText().toString().trim();
                    String corr   = etCorrect.getText().toString().trim();
                    JSONArray ca  = new JSONArray();
                    for (EditText ce : choices) {
                        String c = ce.getText().toString().trim();
                        if (!c.isEmpty()) ca.put(c);
                    }
                    if (q.isEmpty() || corr.isEmpty() || ca.length() < 2) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Ion.with(this).load("POST", Config.BASE_URL + "addQuestion.php")
                            .setBodyParameter("token",        token)
                            .setBodyParameter("formation_id", String.valueOf(formId))
                            .setBodyParameter("question",     q)
                            .setBodyParameter("choices",      ca.toString())
                            .setBodyParameter("correct_answer", corr)
                            .asString()
                            .setCallback((e, result) -> {
                                if (e != null || result == null) return;
                                try {
                                    String status = JsonParser.parseString(result)
                                            .getAsJsonObject().get("status").getAsString();
                                    Toast.makeText(getActivity(),
                                            status.equals("success")
                                                    ? getString(R.string.question_added)
                                                    : getString(R.string.err_server),
                                            Toast.LENGTH_SHORT).show();
                                } catch (Exception ignored) {}
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ── Créer une ressource (avec media optionnel) ───────────
    private void showCreateRessourceDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        EditText etTitle = new EditText(getActivity());
        etTitle.setHint(getString(R.string.hint_formation_title));
        layout.addView(etTitle);

        EditText etContent = new EditText(getActivity());
        etContent.setHint(getString(R.string.hint_tip_content));
        etContent.setMinLines(3);
        layout.addView(etContent);

        // Thème — Spinner
        TextView tvThemeLabel = new TextView(getActivity());
        tvThemeLabel.setText(getString(R.string.lbl_theme));
        tvThemeLabel.setPadding(0, 16, 0, 4);
        layout.addView(tvThemeLabel);

        String[] themes = new String[Config.THEMES.length];
        for (int i = 0; i < Config.THEMES.length; i++) {
            int resId = getActivity().getResources()
                    .getIdentifier("theme_" + Config.THEMES[i], "string",
                            getActivity().getPackageName());
            themes[i] = resId != 0 ? getString(resId) : Config.THEMES[i];
        }
        Spinner spTheme = new Spinner(getActivity());
        spTheme.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, themes));
        layout.addView(spTheme);

        // RadioGroup — IDs obligatoires pour que l'exclusion mutuelle fonctionne
        RadioGroup rg = new RadioGroup(getActivity());
        rg.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbArticle = new RadioButton(getActivity());
        rbArticle.setId(View.generateViewId());
        rbArticle.setText(getString(R.string.lbl_article));
        rbArticle.setChecked(true);

        RadioButton rbGuide = new RadioButton(getActivity());
        rbGuide.setId(View.generateViewId());
        rbGuide.setText(getString(R.string.lbl_guide));

        rg.addView(rbArticle);
        rg.addView(rbGuide);
        layout.addView(rg);

        // Bouton média
        Button btnPickMedia = new Button(getActivity());
        btnPickMedia.setText("📎 Ajouter une photo / vidéo");
        btnPickMedia.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getResources().getColor(R.color.green_light, null)));
        btnPickMedia.setTextColor(getResources().getColor(android.R.color.white, null));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 20, 0, 4);
        btnPickMedia.setLayoutParams(btnParams);
        layout.addView(btnPickMedia);

        tvMediaSelected = new TextView(getActivity());
        tvMediaSelected.setText("Aucun média sélectionné");
        tvMediaSelected.setTextSize(12f);
        tvMediaSelected.setTextColor(
                getResources().getColor(android.R.color.darker_gray, null));
        layout.addView(tvMediaSelected);

        btnPickMedia.setOnClickListener(v ->
                mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.create_ressource_title))
                .setView(layout)
                .setOnDismissListener(d -> tvMediaSelected = null)
                .setPositiveButton(getString(R.string.btn_create_ressource), (dialog, which) -> {
                    String title   = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    // Correction : utiliser spTheme au lieu de etTheme
                    String theme   = themes[spTheme.getSelectedItemPosition()];
                    String type    = rbGuide.isChecked() ? "guide" : "article";

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(getActivity(),
                                getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitRessource(title, content, theme, type);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ── Envoi de la ressource (avec ou sans fichier) ─────────
    private void submitRessource(String title, String content, String theme, String type) {
        if (selectedMediaUri != null) {
            // Envoi multipart avec le fichier média
            try {
                File   mediaFile = getFileFromUri(selectedMediaUri);
                String mime      = selectedMimeType != null ? selectedMimeType : "image/jpeg";

                Ion.with(this).load("POST", Config.BASE_URL + "createRessource.php")
                        .setMultipartParameter("token",   token)
                        .setMultipartParameter("title",   title)
                        .setMultipartParameter("content", content)
                        .setMultipartParameter("theme",   theme)
                        .setMultipartParameter("type",    type)
                        .setMultipartFile("media", mime, mediaFile)
                        .asString()
                        .setCallback((e, result) -> handleRessourceResponse(e, result));

            } catch (IOException ex) {
                Toast.makeText(getActivity(),
                        getString(R.string.err_server), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Envoi classique sans fichier
            Ion.with(this).load("POST", Config.BASE_URL + "createRessource.php")
                    .setBodyParameter("token",   token)
                    .setBodyParameter("title",   title)
                    .setBodyParameter("content", content)
                    .setBodyParameter("theme",   theme)
                    .setBodyParameter("type",    type)
                    .asString()
                    .setCallback((e, result) -> handleRessourceResponse(e, result));
        }
    }

    private void handleRessourceResponse(Exception e, String result) {
        if (e != null || result == null) {
            Toast.makeText(getActivity(),
                    getString(R.string.err_server), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String status = JsonParser.parseString(result)
                    .getAsJsonObject().get("status").getAsString();
            Toast.makeText(getActivity(),
                    status.equals("success")
                            ? getString(R.string.ressource_created)
                            : getString(R.string.err_server),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    // ── Copier l'URI dans un fichier temporaire ──────────────
    private File getFileFromUri(Uri uri) throws IOException {
        String mime = requireActivity().getContentResolver().getType(uri);
        String ext  = (mime != null && mime.startsWith("video")) ? ".mp4" : ".jpg";
        File tmp = File.createTempFile("media_upload", ext,
                requireActivity().getCacheDir());

        try (InputStream  in  = requireActivity().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int len;
            if (in != null) {
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
        return tmp;
    }
}