package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
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

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.adapters.FormationAdapter;
import sae.openminds.models.Formation;

// ============================================================
//  FormateurFragment — toutes les formations sont en présentiel
// ============================================================
public class FormateurFragment extends Fragment {

    private String         token;
    private ProgressBar    progressBar;
    private ListView       listView;
    private TextView       tvEmpty;
    private List<Formation> mesFormations = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_formateur, container, false);

        token       = requireActivity().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE).getString(Config.KEY_TOKEN, "");
        progressBar = view.findViewById(R.id.progressBar);
        listView    = view.findViewById(R.id.listViewFormations);
        tvEmpty     = view.findViewById(R.id.tvEmpty);

        view.findViewById(R.id.btnCreateFormation).setOnClickListener(v -> showCreateFormationDialog());
        view.findViewById(R.id.btnAddQuestion).setOnClickListener(v -> showAddQuestionDialog());
        view.findViewById(R.id.btnCreateRessource).setOnClickListener(v -> showCreateRessourceDialog());

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
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        if (!json.get("status").getAsString().equals("success")) { tvEmpty.setVisibility(View.VISIBLE); return; }
                        Type listType = new TypeToken<List<Formation>>(){}.getType();
                        mesFormations = new Gson().fromJson(json.getAsJsonArray("formations"), listType);
                        if (mesFormations == null || mesFormations.isEmpty()) { tvEmpty.setVisibility(View.VISIBLE); return; }
                        listView.setAdapter(new FormationAdapter(getActivity(), mesFormations));
                        listView.setOnItemClickListener((parent, v, pos, id) ->
                                showStudentsDialog(mesFormations.get(pos)));
                    } catch (Exception ex) { tvEmpty.setVisibility(View.VISIBLE); }
                });
    }

    // ── Voir les élèves ──────────────────────────────────────
    private void showStudentsDialog(Formation f) {
        Ion.with(this).load("POST", Config.BASE_URL + "getFormationStudents.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(f.id))
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); return; }
                    try {
                        JSONObject json     = new JSONObject(result);
                        JSONArray  students = json.getJSONArray("students");
                        if (students.length() == 0) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(f.title).setMessage(getString(R.string.no_students_yet))
                                    .setPositiveButton(getString(R.string.btn_ok), null).show();
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < students.length(); i++) {
                            JSONObject s = students.getJSONObject(i);
                            sb.append(s.getString("firstname")).append(" ").append(s.getString("lastname"))
                                    .append(" — ").append(s.getString("status"));
                            if (!s.isNull("score")) sb.append(" (").append(s.getInt("score")).append("%)");
                            sb.append("\n");
                        }
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.students_enrolled_in, f.title))
                                .setMessage(sb.toString())
                                .setPositiveButton(getString(R.string.btn_ok), null).show();
                    } catch (Exception ex) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); }
                });
    }

    // ── Créer une formation (présentiel uniquement, lieu obligatoire) ─
    private void showCreateFormationDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);

        EditText etTitle = new EditText(getActivity()); etTitle.setHint(getString(R.string.hint_formation_title)); layout.addView(etTitle);
        EditText etDesc  = new EditText(getActivity()); etDesc.setHint(getString(R.string.hint_formation_description)); etDesc.setMinLines(2); layout.addView(etDesc);
        EditText etTheme = new EditText(getActivity()); etTheme.setHint(getString(R.string.hint_formation_theme)); layout.addView(etTheme);

        // Lieu obligatoire (toutes formations = présentiel)
        EditText etLocation  = new EditText(getActivity()); etLocation.setHint(getString(R.string.hint_formation_location)); layout.addView(etLocation);

        // Places max (optionnel)
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
                    String theme      = etTheme.getText().toString().trim();
                    String location   = etLocation.getText().toString().trim();
                    String maxPlacesS = etMaxPlaces.getText().toString().trim();

                    if (title.isEmpty() || theme.isEmpty() || location.isEmpty()) {
                        Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show(); return;
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
                                if (e != null || result == null) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); return; }
                                try {
                                    String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    if (status.equals("success")) { Toast.makeText(getActivity(), getString(R.string.formation_created), Toast.LENGTH_SHORT).show(); loadMyFormations(); }
                                    else Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                } catch (Exception ex) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); }
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null).show();
    }

    // ── Ajouter une question ─────────────────────────────────
    private void showAddQuestionDialog() {
        if (mesFormations.isEmpty()) { Toast.makeText(getActivity(), getString(R.string.select_formation_first), Toast.LENGTH_SHORT).show(); return; }
        LinearLayout layout = new LinearLayout(getActivity()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 30, 50, 20);
        String[] titles = new String[mesFormations.size()];
        for (int i = 0; i < mesFormations.size(); i++) titles[i] = mesFormations.get(i).title;
        TextView tvLabel = new TextView(getActivity()); tvLabel.setText(getString(R.string.select_formation_label)); layout.addView(tvLabel);
        Spinner spFormation = new Spinner(getActivity());
        spFormation.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, titles));
        layout.addView(spFormation);
        EditText etQuestion = new EditText(getActivity()); etQuestion.setHint(getString(R.string.hint_question)); etQuestion.setPadding(0, 16, 0, 0); layout.addView(etQuestion);
        EditText[] choices = new EditText[4];
        for (int i = 0; i < 4; i++) { choices[i] = new EditText(getActivity()); choices[i].setHint(getString(R.string.hint_choice, i + 1)); layout.addView(choices[i]); }
        EditText etCorrect = new EditText(getActivity()); etCorrect.setHint(getString(R.string.hint_correct_answer)); layout.addView(etCorrect);

        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.add_question_title)).setView(layout)
                .setPositiveButton(getString(R.string.btn_add_question), (dialog, which) -> {
                    int formId = mesFormations.get(spFormation.getSelectedItemPosition()).id;
                    String q = etQuestion.getText().toString().trim(), corr = etCorrect.getText().toString().trim();
                    JSONArray ca = new JSONArray();
                    for (EditText ce : choices) { String c = ce.getText().toString().trim(); if (!c.isEmpty()) ca.put(c); }
                    if (q.isEmpty() || corr.isEmpty() || ca.length() < 2) { Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show(); return; }
                    Ion.with(this).load("POST", Config.BASE_URL + "addQuestion.php")
                            .setBodyParameter("token", token).setBodyParameter("formation_id", String.valueOf(formId))
                            .setBodyParameter("question", q).setBodyParameter("choices", ca.toString()).setBodyParameter("correct_answer", corr)
                            .asString().setCallback((e, result) -> {
                                if (e != null || result == null) return;
                                try { String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    Toast.makeText(getActivity(), status.equals("success") ? getString(R.string.question_added) : getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                } catch (Exception ignored) {}
                            });
                }).setNegativeButton(getString(R.string.btn_cancel), null).show();
    }

    // ── Créer une ressource ──────────────────────────────────
    private void showCreateRessourceDialog() {
        LinearLayout layout = new LinearLayout(getActivity()); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(50, 30, 50, 20);
        EditText etTitle = new EditText(getActivity()); etTitle.setHint(getString(R.string.hint_formation_title)); layout.addView(etTitle);
        EditText etContent = new EditText(getActivity()); etContent.setHint(getString(R.string.hint_tip_content)); etContent.setMinLines(3); layout.addView(etContent);
        EditText etTheme = new EditText(getActivity()); etTheme.setHint(getString(R.string.hint_formation_theme)); layout.addView(etTheme);
        RadioGroup rg = new RadioGroup(getActivity()); rg.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbArticle = new RadioButton(getActivity()); rbArticle.setText(getString(R.string.lbl_article)); rbArticle.setChecked(true);
        RadioButton rbGuide = new RadioButton(getActivity()); rbGuide.setText(getString(R.string.lbl_guide));
        rg.addView(rbArticle); rg.addView(rbGuide); layout.addView(rg);

        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.create_ressource_title)).setView(layout)
                .setPositiveButton(getString(R.string.btn_create_ressource), (dialog, which) -> {
                    String title = etTitle.getText().toString().trim(), content = etContent.getText().toString().trim(), theme = etTheme.getText().toString().trim();
                    if (title.isEmpty() || content.isEmpty()) { Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show(); return; }
                    Ion.with(this).load("POST", Config.BASE_URL + "createRessource.php")
                            .setBodyParameter("token", token).setBodyParameter("title", title)
                            .setBodyParameter("content", content).setBodyParameter("theme", theme)
                            .setBodyParameter("type", rbGuide.isChecked() ? "guide" : "article")
                            .asString().setCallback((e, result) -> {
                                if (e != null || result == null) return;
                                try { String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    Toast.makeText(getActivity(), status.equals("success") ? getString(R.string.ressource_created) : getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                } catch (Exception ignored) {}
                            });
                }).setNegativeButton(getString(R.string.btn_cancel), null).show();
    }
}
