package sae.openminds.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import sae.openminds.Config;
import sae.openminds.R;

// ============================================================
//  QuizFragment — liste des formations inscrites avec quiz
//  Navigation : liste de cartes → quiz d'une formation
// ============================================================
public class QuizFragment extends Fragment {

    // ── Widgets communs ──────────────────────────────────────
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    // ── Vue 1 : liste des formations ─────────────────────────
    private ScrollView   scrollFormations;
    private LinearLayout layoutFormations;

    // ── Vue 2 : quiz ─────────────────────────────────────────
    private Button       btnBack;
    private ScrollView   scrollView;
    private LinearLayout layoutQuiz;
    private Button       btnSubmit;
    private CardView     cardResult;
    private TextView     tvResult;

    // ── État ─────────────────────────────────────────────────
    private String  token;
    private int     currentFormationId = -1;
    private final Map<Integer, RadioGroup> radioGroups = new HashMap<>();

    // ─────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // Widgets communs
        progressBar      = view.findViewById(R.id.progressBar);
        tvEmpty          = view.findViewById(R.id.tvEmpty);

        // Vue 1
        scrollFormations = view.findViewById(R.id.scrollFormations);
        layoutFormations = view.findViewById(R.id.layoutFormations);

        // Vue 2
        btnBack          = view.findViewById(R.id.btnBack);
        scrollView       = view.findViewById(R.id.scrollView);
        layoutQuiz       = view.findViewById(R.id.layoutQuiz);
        btnSubmit        = view.findViewById(R.id.btnSubmitQuiz);
        cardResult       = view.findViewById(R.id.cardResult);
        tvResult         = view.findViewById(R.id.tvResult);

        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        btnBack.setOnClickListener(v -> showFormationList());
        btnSubmit.setOnClickListener(v -> submitQuiz());

        // Charger la liste des formations au démarrage
        fetchFormationList();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rafraîchir la liste si on revient sur le fragment
        // (ex : nouvelle inscription depuis l'onglet Formations)
        if (currentFormationId == -1) {
            fetchFormationList();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VUE 1 — Liste des formations
    // ══════════════════════════════════════════════════════════

    private void fetchFormationList() {
        showLoading(true);
        Ion.with(this)
                .load("POST", Config.BASE_URL + "getQuizFormations.php")
                .setBodyParameter("token", token)
                .asString()
                .setCallback((e, result) -> {
                    showLoading(false);
                    android.util.Log.d("QUIZ_DEBUG", "formations result: " + result);

                    if (e != null || result == null) {
                        showEmpty(getString(R.string.err_server));
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!json.getString("status").equals("success")) {
                            showEmpty(getString(R.string.select_formation_first));
                            return;
                        }
                        JSONArray formations = json.getJSONArray("formations");
                        if (formations.length() == 0) {
                            showEmpty(getString(R.string.select_formation_first));
                            return;
                        }
                        buildFormationList(formations);
                    } catch (Exception ex) {
                        android.util.Log.e("QUIZ_DEBUG", "parse error: " + ex.getMessage());
                        showEmpty(getString(R.string.err_server));
                    }
                });
    }

    private void buildFormationList(JSONArray formations) throws Exception {
        layoutFormations.removeAllViews();

        for (int i = 0; i < formations.length(); i++) {
            JSONObject f             = formations.getJSONObject(i);
            int        formationId   = f.getInt("formation_id");
            String     title         = f.getString("title");
            String     theme         = f.optString("theme", "");
            String     status        = f.getString("enrollment_status");
            int        quizCount     = f.getInt("quiz_count");
            boolean    done          = status.equals("termine");
            Integer    score         = f.isNull("score") ? null : f.getInt("score");

            // ── Carte ────────────────────────────────────────
            CardView card = new CardView(requireContext());
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT,
                    CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 20);
            card.setLayoutParams(cardParams);
            card.setRadius(16f);
            card.setCardElevation(4f);
            card.setUseCompatPadding(true);

            // ── Contenu de la carte ───────────────────────────
            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(40, 32, 40, 32);

            // Titre de la formation
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(title);
            tvTitle.setTextSize(16f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(getResources().getColor(R.color.text_primary, null));
            content.addView(tvTitle);

            // Thème
            if (!theme.isEmpty()) {
                TextView tvTheme = new TextView(requireContext());
                tvTheme.setText(theme);
                tvTheme.setTextSize(13f);
                tvTheme.setTextColor(getResources().getColor(R.color.text_secondary, null));
                tvTheme.setPadding(0, 4, 0, 0);
                content.addView(tvTheme);
            }

            // Séparateur
            View divider = new View(requireContext());
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divParams.setMargins(0, 16, 0, 16);
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(getResources().getColor(R.color.text_secondary, null));
            divider.setAlpha(0.2f);
            content.addView(divider);

            // Ligne infos : nb questions + statut/score
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvCount = new TextView(requireContext());
            tvCount.setText(quizCount + " question" + (quizCount > 1 ? "s" : ""));
            tvCount.setTextSize(13f);
            tvCount.setTextColor(getResources().getColor(R.color.text_secondary, null));
            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvCount.setLayoutParams(countParams);
            row.addView(tvCount);

            // Badge statut
            TextView tvBadge = new TextView(requireContext());
            if (done && score != null) {
                tvBadge.setText("Score : " + score + "%");
                tvBadge.setBackgroundColor(
                        score >= 70
                                ? Color.parseColor("#EAF3DE")
                                : Color.parseColor("#FCEBEB"));
                tvBadge.setTextColor(
                        score >= 70
                                ? Color.parseColor("#27500A")
                                : Color.parseColor("#501313"));
            } else {
                tvBadge.setText("À faire");
                tvBadge.setBackgroundColor(Color.parseColor("#E6F1FB"));
                tvBadge.setTextColor(Color.parseColor("#0C447C"));
            }
            tvBadge.setTextSize(12f);
            tvBadge.setPadding(16, 6, 16, 6);
            tvBadge.setTypeface(null, Typeface.BOLD);
            row.addView(tvBadge);
            content.addView(row);

            card.addView(content);
            layoutFormations.addView(card);

            // Clic → ouvrir le quiz (même si déjà fait : lecture seule)
            card.setOnClickListener(v -> openQuiz(formationId, done, score));
        }

        scrollFormations.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showFormationList() {
        // Réinitialiser l'état
        currentFormationId = -1;
        radioGroups.clear();
        layoutQuiz.removeAllViews();

        // Masquer vue 2
        btnBack.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);
        cardResult.setVisibility(View.GONE);

        // Rafraîchir et afficher vue 1 ← déplacé ici
        fetchFormationList();
    }

    // ══════════════════════════════════════════════════════════
    //  VUE 2 — Quiz d'une formation
    // ══════════════════════════════════════════════════════════

    private void openQuiz(int formationId, boolean alreadyDone, Integer existingScore) {
        currentFormationId = formationId;

        // Basculer vers la vue 2
        scrollFormations.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        btnBack.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        tvResult.setVisibility(View.GONE);
        layoutQuiz.removeAllViews();
        radioGroups.clear();

        fetchQuiz(formationId, alreadyDone);
    }

    private void fetchQuiz(int formationId, boolean readOnly) {
        showLoading(true);

        Ion.with(this)
                .load("POST", Config.BASE_URL + "getQuiz.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(formationId))
                .asString()
                .setCallback((e, result) -> {
                    showLoading(false);
                    android.util.Log.d("QUIZ_DEBUG", "quiz result: " + result);

                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json   = new JSONObject(result);
                        String     status = json.getString("status");
                        if (!status.equals("success")) {
                            showEmpty(getString(R.string.err_server));
                            return;
                        }
                        JSONArray questions = json.getJSONArray("questions");
                        if (questions.length() == 0) {
                            showEmpty(getString(R.string.select_formation_first));
                            return;
                        }

                        buildQuizUI(questions, readOnly);

                        scrollView.setVisibility(View.VISIBLE);
                        // Bouton soumettre uniquement si pas déjà fait
                        btnSubmit.setVisibility(readOnly ? View.GONE : View.VISIBLE);

                        // Si déjà terminé, afficher le score en lecture seule
                        if (readOnly) {
                            fetchAndShowResult(formationId);
                        }

                    } catch (Exception ex) {
                        android.util.Log.e("QUIZ_DEBUG", "quiz parse error: " + ex.getMessage());
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAndShowResult(int formationId) {
        // Récupérer le score sauvegardé + afficher le feedback en lecture seule
        Ion.with(this)
                .load("POST", Config.BASE_URL + "getQuizResult.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(formationId))
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!json.getString("status").equals("success")) return;

                        int     score = json.getInt("score");
                        boolean badge = json.getBoolean("badge_awarded");

                        // Afficher le résultat
                        String msg = getString(R.string.quiz_result, score);
                        if (badge) msg += "\n" + getString(R.string.badge_awarded);
                        else if (score < 70) msg += "\n" + getString(R.string.quiz_failed);
                        tvResult.setText(msg);
                        cardResult.setVisibility(View.VISIBLE);
                        tvResult.setVisibility(View.VISIBLE);

                        // Afficher le feedback visuel si disponible
                        if (json.has("details")) {
                            showAnswerFeedback(json.getJSONArray("details"));
                        }

                    } catch (Exception ex) {
                        android.util.Log.d("QUIZ_DEBUG", "formations result: " + result);
// Afficher le résultat brut à l'écran pour déboguer
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void buildQuizUI(JSONArray questions, boolean readOnly) throws Exception {
        layoutQuiz.setVisibility(View.VISIBLE);
        for (int i = 0; i < questions.length(); i++) {
            JSONObject q       = questions.getJSONObject(i);
            int        qId     = q.getInt("id");
            String     qText   = q.getString("question");
            JSONArray  choices = q.getJSONArray("choices");

            TextView tvQuestion = new TextView(requireContext());
            tvQuestion.setText((i + 1) + ". " + qText);
            tvQuestion.setTextSize(15f);
            tvQuestion.setPadding(0, 24, 0, 8);
            tvQuestion.setTextColor(getResources().getColor(R.color.text_primary, null));
            layoutQuiz.addView(tvQuestion);

            RadioGroup rg = new RadioGroup(requireContext());
            rg.setOrientation(RadioGroup.VERTICAL);
            for (int j = 0; j < choices.length(); j++) {
                RadioButton rb = new RadioButton(requireContext());
                rb.setText(choices.getString(j));
                rb.setPadding(8, 4, 8, 4);
                rb.setEnabled(!readOnly); // désactivé en lecture seule
                rg.addView(rb);
            }
            layoutQuiz.addView(rg);
            radioGroups.put(qId, rg);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Soumission du quiz
    // ══════════════════════════════════════════════════════════

    private void submitQuiz() {
        if (currentFormationId == -1) return;

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

        showLoading(true);
        btnSubmit.setEnabled(false);

        Ion.with(this)
                .load("POST", Config.BASE_URL + "submitQuiz.php")
                .setBodyParameter("token",        token)
                .setBodyParameter("formation_id", String.valueOf(currentFormationId))
                .setBodyParameter("answers",      answers.toString())
                .asString()
                .setCallback((e, result) -> {
                    showLoading(false);
                    btnSubmit.setEnabled(true);
                    android.util.Log.d("QUIZ_DEBUG", "submit result: " + result);

                    if (e != null || result == null) {
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject json   = new JSONObject(result);
                        String     status = json.getString("status");
                        if (!status.equals("success")) {
                            Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int     score = json.getInt("score");
                        boolean badge = json.getBoolean("badge_awarded");

                        // Feedback visuel sur les réponses
                        if (json.has("details")) {
                            showAnswerFeedback(json.getJSONArray("details"));
                        }

                        // Désactiver tous les radio buttons
                        for (RadioGroup rg : radioGroups.values()) {
                            for (int i = 0; i < rg.getChildCount(); i++) {
                                rg.getChildAt(i).setEnabled(false);
                            }
                        }

                        btnSubmit.setVisibility(View.GONE);

                        String msg = getString(R.string.quiz_result, score);
                        if (badge) msg += "\n" + getString(R.string.badge_awarded);
                        else if (score < 75) msg += "\n" + getString(R.string.quiz_failed);
                        tvResult.setText(msg);
                        cardResult.setVisibility(View.VISIBLE);
                        tvResult.setVisibility(View.VISIBLE);

                    } catch (Exception ex) {
                        android.util.Log.e("QUIZ_DEBUG", "submit parse: " + ex.getMessage());
                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ══════════════════════════════════════════════════════════
    //  Feedback visuel vert/rouge après correction
    // ══════════════════════════════════════════════════════════

    private void showAnswerFeedback(JSONArray details) throws Exception {
        Map<Integer, JSONObject> detailMap = new HashMap<>();
        for (int i = 0; i < details.length(); i++) {
            JSONObject d = details.getJSONObject(i);
            detailMap.put(d.getInt("question_id"), d);
        }

        for (Map.Entry<Integer, RadioGroup> entry : radioGroups.entrySet()) {
            int        qId    = entry.getKey();
            RadioGroup rg     = entry.getValue();
            JSONObject detail = detailMap.get(qId);
            if (detail == null) continue;

            boolean isCorrect     = detail.getBoolean("is_correct");
            String  correctAnswer = detail.getString("correct_answer");

            for (int i = 0; i < rg.getChildCount(); i++) {
                rg.getChildAt(i).setEnabled(false);
            }

            int checkedId = rg.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton selected = rg.findViewById(checkedId);
                if (isCorrect) {
                    selected.setTextColor(Color.parseColor("#27500A"));
                    selected.setText("✅ " + selected.getText());
                } else {
                    selected.setTextColor(Color.parseColor("#A32D2D"));
                    selected.setText("❌ " + selected.getText());

                    for (int i = 0; i < rg.getChildCount(); i++) {
                        RadioButton rb = (RadioButton) rg.getChildAt(i);
                        if (rb.getText().toString().trim().equals(correctAnswer.trim())) {
                            rb.setTextColor(Color.parseColor("#27500A"));
                            rb.setText("✅ " + rb.getText());
                            break;
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(String msg) {
        scrollFormations.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}
