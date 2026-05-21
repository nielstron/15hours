package de.nielstron.fifteenhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        if (TimerState.active(context) && TimerState.endsAt(context) > now) {
            Scheduler.scheduleAll(context, TimerState.endsAt(context));
        }
        AppNotifications.showCountdown(context);
    }
}
