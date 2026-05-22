package de.nielstron.fifteenhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ActionReceiver extends BroadcastReceiver {
    static final String ACTION_START = "de.nielstron.fifteenhours.START";
    static final String ACTION_ALARM = "de.nielstron.fifteenhours.ALARM";
    static final String ACTION_SHOW_COUNTDOWN = "de.nielstron.fifteenhours.SHOW_COUNTDOWN";
    static final String ACTION_STOP_ALARM = "de.nielstron.fifteenhours.STOP_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            long now = System.currentTimeMillis();
            TimerState.start(context, now);
            Scheduler.cancelAll(context);
            Scheduler.scheduleAll(context, TimerState.endsAt(context));
            AppNotifications.showCountdown(context);
        } else if (ACTION_SHOW_COUNTDOWN.equals(action)) {
            AppNotifications.showCountdown(context);
        } else if (ACTION_ALARM.equals(action)) {
            if (!AlarmSettings.bedtimeAlarmEnabled(context)) {
                AppNotifications.showCountdown(context);
                return;
            }
            if (AppVisibility.foreground(context)) {
                AppNotifications.showCountdown(context);
                return;
            }
            AlarmState.ring(context);
        } else if (ACTION_STOP_ALARM.equals(action)) {
            AlarmState.dismiss(context);
        }
    }
}
