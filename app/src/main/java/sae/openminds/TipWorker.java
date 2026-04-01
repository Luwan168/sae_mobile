package sae.openminds;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.koushikdutta.ion.Ion;

import org.json.JSONObject;

public class TipWorker extends Worker {
    public TipWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(Config.KEY_TOKEN, "");


        // Récupération du Tip via Ion (synchrone ici pour le Worker)
        try {
            // On utilise une requête bloquante car le Worker tourne déjà en arrière-plan
            String result = Ion.with(getApplicationContext())
                    .load(Config.BASE_URL + "getBonnesPratiques.php")
                    .asString().get();

            JSONObject json = new JSONObject(result);
            if (json.getString("status").equals("success")) {
                String content = json.getJSONObject("pratique").getString("content");
                showNotification("Conseil du jour", content);
            }
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "tips_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Tips of the Day", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_news)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}