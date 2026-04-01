package sae.openminds.fragments;

// ============================================================
//  AdminFragment — Admin uniquement
//  Stats + validation tips + modération + création actualité
// ============================================================

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import sae.openminds.Config;
import sae.openminds.R;

public class AdminFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalFormations, tvTotalEnrollments, tvAvgScore;
    private String   token;

    private String          selectedImageB64 = null;
    private ImageView       ivPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(uri);
                        Bitmap bmp    = BitmapFactory.decodeStream(is);
                        if (is != null) is.close();
                        bmp = scaleBitmap(bmp, 1024);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                        byte[] bytes = baos.toByteArray();
                        selectedImageB64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                        if (ivPreview != null) {
                            ivPreview.setImageBitmap(bmp);
                            ivPreview.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Erreur lecture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        tvTotalUsers       = view.findViewById(R.id.tvTotalUsers);
        tvTotalFormations  = view.findViewById(R.id.tvTotalFormations);
        tvTotalEnrollments = view.findViewById(R.id.tvTotalEnrollments);
        tvAvgScore         = view.findViewById(R.id.tvGlobalAvgScore);
        Button btnModerer       = view.findViewById(R.id.btnModerBonnesPratiques);
        Button btnValidate      = view.findViewById(R.id.btnValidateTips);
        Button btnCreateActu    = view.findViewById(R.id.btnCreateActualite);

        token = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Config.KEY_TOKEN, "");

        fetchStats();
        btnModerer.setOnClickListener(v -> showModerationDialog());
        btnValidate.setOnClickListener(v -> showPendingTipsDialog());
        btnCreateActu.setOnClickListener(v -> showCreateActualiteDialog());

        return view;
    }

    private void fetchStats() {
        Ion.with(this).load("POST", Config.BASE_URL + "getStats.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (!json.getString("status").equals("success")) return;
                        JSONObject stats = json.getJSONObject("stats");
                        tvTotalUsers.setText(getString(R.string.admin_total_users) + " " + stats.getInt("total_users"));
                        tvTotalFormations.setText(getString(R.string.admin_total_formations) + " " + stats.getInt("total_formations"));
                        tvTotalEnrollments.setText(getString(R.string.admin_total_enrollments) + " " + stats.getInt("total_enrollments"));
                        tvAvgScore.setText(getString(R.string.admin_avg_score) + " " + stats.getInt("avg_score") + "%");
                    } catch (Exception ignored) {}
                });
    }

    private void showCreateActualiteDialog() {
        selectedImageB64 = null;
        ScrollView scrollView = new ScrollView(getActivity());
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);
        scrollView.addView(layout);

        EditText etTitle = new EditText(getActivity());
        etTitle.setHint("Titre *");
        layout.addView(etTitle);

        EditText etContent = new EditText(getActivity());
        etContent.setHint("Contenu *");
        etContent.setMinLines(4);
        layout.addView(etContent);

        Button btnPickImage = new Button(getActivity());
        btnPickImage.setText("Choisir une image (optionnel)");
        btnPickImage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.green_light, null)));
        btnPickImage.setTextColor(Color.WHITE);
        layout.addView(btnPickImage);

        ivPreview = new ImageView(getActivity());
        ivPreview.setVisibility(View.GONE);
        ivPreview.setAdjustViewBounds(true);
        ivPreview.setMaxHeight(400);
        layout.addView(ivPreview);

        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        new AlertDialog.Builder(getActivity())
                .setTitle("Créer une actualité")
                .setView(scrollView)
                .setPositiveButton("Publier", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (title.isEmpty() || content.isEmpty()) return;

                    Ion.with(this).load("POST", Config.BASE_URL + "createActualite.php")
                            .setBodyParameter("token", token)
                            .setBodyParameter("title", title)
                            .setBodyParameter("content", content)
                            .setBodyParameter("image", selectedImageB64 != null ? selectedImageB64 : "")
                            .asString().setCallback((e, result) -> {
                                if (e != null) return;
                                Toast.makeText(getActivity(), "Actualité publiée !", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private Bitmap scaleBitmap(Bitmap src, int maxWidth) {
        if (src.getWidth() <= maxWidth) return src;
        float ratio  = (float) maxWidth / src.getWidth();
        int newH     = Math.round(src.getHeight() * ratio);
        return Bitmap.createScaledBitmap(src, maxWidth, newH, true);
    }

    private void showPendingTipsDialog() {
        Ion.with(this).load("POST", Config.BASE_URL + "getPendingTips.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray  list = json.getJSONArray("pratiques");
                        if (list.length() == 0) {
                            Toast.makeText(getActivity(), getString(R.string.no_pending_tips), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showNextTip(list, 0);
                    } catch (Exception ignored) {}
                });
    }

    private void showNextTip(JSONArray list, int index) {
        if (index >= list.length()) return;
        try {
            JSONObject tip     = list.getJSONObject(index);
            int        id      = tip.getInt("id");
            String     content = tip.getString("content");
            String     by      = tip.optString("submitted_by_name", "?");
            String     message = content + "\n\n— Soumis par : " + by;

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.pending_tips_title))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.btn_approve), (d, w) -> {
                        validateTip(id, "approve");
                        showNextTip(list, index + 1);
                    })
                    .setNegativeButton(getString(R.string.btn_reject), (d, w) -> {
                        validateTip(id, "reject");
                        showNextTip(list, index + 1);
                    })
                    .setNeutralButton(getString(R.string.btn_delete), (d, w) -> {
                        validateTip(id, "delete");
                        showNextTip(list, index + 1);
                    })
                    .create();
            
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.RED);
        } catch (Exception ignored) {}
    }

    private void validateTip(int id, String action) {
        Ion.with(this).load("POST", Config.BASE_URL + "validateTip.php")
                .setBodyParameter("token",  token)
                .setBodyParameter("id",     String.valueOf(id))
                .setBodyParameter("action", action)
                .asString().setCallback((e, r) -> {
                    if (e == null && action.equals("delete")) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), getString(R.string.tip_deleted), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ── Modération tips existants avec option de suppression ─────────────

    private void showModerationDialog() {
        Ion.with(this).load("POST", Config.BASE_URL + "getAllBonnesPratiques.php")
                .setBodyParameter("token", token).asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) return;
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONArray  list = json.getJSONArray("pratiques");

                        String[]  labels  = new String[list.length()];
                        int[]     ids     = new int[list.length()];
                        boolean[] actives = new boolean[list.length()];

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject p = list.getJSONObject(i);
                            ids[i]     = p.getInt("id");
                            String c   = p.getString("content");
                            labels[i]  = (p.getInt("active") == 1 ? "✅ " : "❌ ") + 
                                         c.substring(0, Math.min(60, c.length())) + "...";
                            actives[i] = p.getInt("active") == 1;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.admin_moderate_title))
                                .setItems(labels, (dialog, which) -> {
                                    showTipActionDialog(ids[which], list.optJSONObject(which).optString("content"), actives[which]);
                                })
                                .setPositiveButton(getString(R.string.btn_ok), null)
                                .show();
                    } catch (Exception ignored) {}
                });
    }

    private void showTipActionDialog(int id, String content, boolean isActive) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Gérer la bonne pratique")
                .setMessage(content)
                .setPositiveButton(isActive ? "Désactiver" : "Activer", (d, w) -> {
                    Ion.with(this).load("POST", Config.BASE_URL + "moderatePratique.php")
                            .setBodyParameter("token",  token)
                            .setBodyParameter("id",     String.valueOf(id))
                            .setBodyParameter("active", isActive ? "0" : "1")
                            .asString().setCallback((e, r) -> {
                                if (getActivity() != null) {
                                    Toast.makeText(getActivity(), "Statut mis à jour", Toast.LENGTH_SHORT).show();
                                    showModerationDialog(); // Recharger la liste
                                }
                            });
                })
                .setNeutralButton("Supprimer", (d, w) -> {
                    validateTip(id, "delete");
                    // Délai court pour laisser la suppression se faire avant de rafraîchir
                    if (getView() != null) {
                        getView().postDelayed(this::showModerationDialog, 500);
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();
        
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.RED);
    }
}