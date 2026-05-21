package de.nielstron.fifteenhours;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ReminderReceiver extends BroadcastReceiver {
    static final String ACTION_REMIND = "de.nielstron.fifteenhours.REMIND";
    static final String EXTRA_REMAINING_MS = "remaining_ms";

    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        if (!TimerState.active(context)) {
            return;
        }
        long remaining = intent.getLongExtra(EXTRA_REMAINING_MS, TimerState.remaining(context, now));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        AppNotifications.createChannels(context);
        manager.notify(AppNotifications.REMINDER_ID, AppNotifications.reminder(context, remaining));
    }
}
