package sae.openminds.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import sae.openminds.Config;
import sae.openminds.R;

// ============================================================
//  app/src/main/java/sae/openminds/fragments/QuizFragment.java
//  Affiche les questions d'un quiz et soumet les réponses
// ============================================================
public class QuizFragment extends Fragment {

    private ProgressBar   progressBar;
    private LinearLayout  layoutQuiz;
    private TextView      tvEmpty, tvResult;
    private Button        btnSubmit;
    private ScrollView    scrollView;
    private String        token;
    private int           currentFormationId = -1;

    // Map question_id -> réponse sélectionnée
    private final Map<Integer, RadioGroup> radioGroups = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        progressBar  = view.findViewById(R.id.progressBar);
        layoutQuiz   = view.findViewById(R.id.layoutQuiz);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        tvResult     = view.findViewById(R.id.tvResult);
        btnSubmit    = view.findViewById(R.id.btnSubmitQuiz);
        scrollView   = view.findViewById(R.id.scrollView);

        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        // Récupérer l'ID de formation passé en argument si présent
        Bundle args = getArguments();
        if (args != null && args.containsKey("formation_id")) {
            currentFormationId = args.getInt("formation_id");
            fetchQuiz(currentFormationId);
        } else {
            tvEmpty.setText(getString(R.string.select_formation_first));
            tvEmpty.setVisibility(View.VISIBLE);
        }

        btnSubmit.setOnClickListener(v -> submitQuiz());
        return view;
    }

    public void loadQuizForFormation(int formationId) {
        currentFormationId = formationId;
        fetchQuiz(formationId);
    }

    private void fetchQuiz(int formationId) {
        progressBar.setVisibility(View.VISIBLE);
        layoutQuiz.removeAllViews();
        radioGroups.clear();

        Ion.with(this)
                .load("POST", Config.BASE_URL + "getQuiz.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(formationId))
                .asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json   = new JSONObject(result);
                        String     status = json.getString("status");
                        if (!status.equals("success")) { tvEmpty.setVisibility(View.VISIBLE); return; }

                        JSONArray questions = json.getJSONArray("questions");
                        if (questions.length() == 0) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        buildQuizUI(questions);
                        btnSubmit.setVisibility(View.VISIBLE);

                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void buildQuizUI(JSONArray questions) throws Exception {
        layoutQuiz.setVisibility(View.VISIBLE);
        for (int i = 0; i < questions.length(); i++) {
            JSONObject q       = questions.getJSONObject(i);
            int        qId     = q.getInt("id");
            String     qText   = q.getString("question");
            JSONArray  choices = q.getJSONArray("choices");

            TextView tvQuestion = new TextView(getActivity());
            tvQuestion.setText((i + 1) + ". " + qText);
            tvQuestion.setTextSize(15f);
            tvQuestion.setPadding(0, 24, 0, 8);
            tvQuestion.setTextColor(getResources().getColor(R.color.text_primary, null));
            layoutQuiz.addView(tvQuestion);

            RadioGroup rg = new RadioGroup(getActivity());
            rg.setOrientation(RadioGroup.VERTICAL);
            for (int j = 0; j < choices.length(); j++) {
                RadioButton rb = new RadioButton(getActivity());
                rb.setText(choices.getString(j));
                rb.setPadding(8, 4, 8, 4);
                rg.addView(rb);
            }
            layoutQuiz.addView(rg);
            radioGroups.put(qId, rg);
        }
    }

    private void submitQuiz() {
        if (currentFormationId == -1) return;

        // Construire le JSON des réponses { "qId": "reponse", ... }
        JSONObject answers = new JSONObject();
        try {
            for (Map.Entry<Integer, RadioGroup> entry : radioGroups.entrySet()) {
                RadioGroup rg        = entry.getValue();
                int        checkedId = rg.getCheckedRadioButtonId();
                if (checkedId == -1) {
                    Toast.makeText(getActivity(), getString(R.string.err_answer_all), Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton rb = rg.findViewById(checkedId);
                answers.put(String.valueOf(entry.getKey()), rb.getText().toString());
            }
        } catch (Exception e) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        Ion.with(this)
                .load("POST", Config.BASE_URL + "submitQuiz.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(currentFormationId))
                .setBodyParameter("answers",      answers.toString())
                .asString()
                .setCallback((e, result) -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json  = new JSONObject(result);
                        int        score = json.getInt("score");
                        boolean    badge = json.getBoolean("badge_awarded");

                        String msg = getString(R.string.quiz_result, score);
                        if (badge) msg += "\n" + getString(R.string.badge_awarded);
                        else if (score < 70) msg += "\n" + getString(R.string.quiz_failed);

                        tvResult.setText(msg);
                        tvResult.setVisibility(View.VISIBLE);
                        layoutQuiz.setVisibility(View.GONE);
                        btnSubmit.setVisibility(View.GONE);

                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
