package sae.openminds.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// ============================================================
//  app/src/main/java/sae/openminds/utils/BootReceiver.java
//  Re-planifie les notifications après redémarrage
// ============================================================
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationReceiver.scheduleDailyNotification(context);
        }
    }
}
