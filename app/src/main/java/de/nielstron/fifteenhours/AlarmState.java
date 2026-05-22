package de.nielstron.fifteenhours;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

final class AlarmState {
    private static final String PREFS = "alarm_state";
    private static final String RINGING = "ringing";

    private AlarmState() {}

    static boolean ringing(Context context) {
        return prefs(context).getBoolean(RINGING, false);
    }

    static void ring(Context context) {
        prefs(context).edit()
                .putBoolean(RINGING, true)
                .apply();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        AppNotifications.createChannels(context);
        manager.cancel(AppNotifications.ALARM_ID);
        manager.notify(AppNotifications.ALARM_ID, AppNotifications.alarm(context));
        AppNotifications.showCountdown(context);
    }

    static void dismiss(Context context) {
        prefs(context).edit()
                .putBoolean(RINGING, false)
                .apply();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancel(AppNotifications.ALARM_ID);
        AppNotifications.showCountdown(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
