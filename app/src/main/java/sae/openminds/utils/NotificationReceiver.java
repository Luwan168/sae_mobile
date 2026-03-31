package sae.openminds.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.koushikdutta.ion.Ion;

import org.json.JSONObject;

import java.util.Calendar;

import sae.openminds.Config;
import sae.openminds.R;

// ============================================================
//  app/src/main/java/sae/openminds/utils/NotificationReceiver.java
// ============================================================
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "openminds_daily";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notifsEnabled = prefs.getBoolean(Config.KEY_NOTIFS, true);
        if (!notifsEnabled) return;

        Ion.with(context)
                .load("GET", Config.BASE_URL + "getBonnesPratiques.php")
                .asString()
                .setCallback((e, result) -> {
                    String message = context.getString(R.string.notif_default_tip);
                    if (e == null && result != null) {
                        try {
                            JSONObject json = new JSONObject(result);
                            if (json.getString("status").equals("success")) {
                                JSONObject pratique = json.getJSONObject("pratique");
                                message = pratique.getString("content");
                            }
                        } catch (Exception ignored) {}
                    }
                    showNotification(context, message);
                });
    }

    private void showNotification(Context context, String message) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true);
        nm.notify(1, builder.build());
    }

    public static void scheduleDailyNotification(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }
}
