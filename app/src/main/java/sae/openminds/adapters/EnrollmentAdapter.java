package sae.openminds.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import java.util.List;

import sae.openminds.Config;
import sae.openminds.R;
import sae.openminds.models.Enrollment;

public class EnrollmentAdapter extends ArrayAdapter<Enrollment> {

    public EnrollmentAdapter(Context context, List<Enrollment> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_enrollment, parent, false);

        Enrollment e = getItem(position);
        if (e == null) return convertView;

        TextView tvTitle  = convertView.findViewById(R.id.tvEnrollTitle);
        TextView tvStatus = convertView.findViewById(R.id.tvEnrollStatus);
        TextView tvScore  = convertView.findViewById(R.id.tvEnrollScore);
        // NOUVEAU : Récupération de l'ImageView de la croix
        ImageView ivDelete = convertView.findViewById(R.id.ivDeleteEnrollment);

        tvTitle.setText(e.title);
        tvStatus.setText(e.status);

        // Couleur selon le statut
        switch (e.status) {
            case "termine":
                tvStatus.setTextColor(getContext().getColor(R.color.status_done));
                tvScore.setVisibility(View.VISIBLE);
                tvScore.setText(e.score + "%");
                break;
            case "abandonne":
                tvStatus.setTextColor(getContext().getColor(R.color.status_abandoned));
                tvScore.setVisibility(View.GONE);
                break;
            default:
                tvStatus.setTextColor(getContext().getColor(R.color.status_ongoing));
                tvScore.setVisibility(View.GONE);
        }

        // GESTION DU CLIC SUR LA CROIX
        ivDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Confirmation de suppression")
                    .setMessage("Êtes-vous sûr de vouloir supprimer la formation : " + e.title + " ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        // Lancer la requête de suppression côté serveur
                        deleteEnrollmentFromServer(e, position);
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        return convertView;
    }

    private void deleteEnrollmentFromServer(Enrollment enrollment, int position) {
        SharedPreferences prefs = getContext().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(Config.KEY_TOKEN, "");

        Ion.with(getContext())
                .load("POST", Config.BASE_URL + "deleteEnrollment.php")
                .setBodyParameter("token", token)
                .setBodyParameter("enrollment_id", String.valueOf(enrollment.id)) // Assurez-vous que votre modèle Enrollment a un champ 'id'
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(getContext(), getContext().getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        String status = json.get("status").getAsString();
                        if (status.equals("success")) {
                            // Suppression réussie côté serveur -> on l'enlève de la liste
                            remove(getItem(position));
                            notifyDataSetChanged();
                            Toast.makeText(getContext(), "Formation supprimée.", Toast.LENGTH_SHORT).show();
                        } else {
                            String message = json.has("message") ? json.get("message").getAsString() : "Erreur lors de la suppression.";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(getContext(), "Erreur de format de réponse serveur.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}