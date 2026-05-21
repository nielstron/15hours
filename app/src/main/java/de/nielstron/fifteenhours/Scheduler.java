package de.nielstron.fifteenhours;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class Scheduler {
    private static final int REQUEST_ALARM = 100;
    private static final int REQUEST_SHOW_COUNTDOWN = 150;
    private static final int REQUEST_REMINDER_BASE = 200;
    private static final long[] REMIND_BEFORE_MS = {
            TimerState.LOW_TIME_MS,
            90L * 60L * 1000L,
            60L * 60L * 1000L,
            30L * 60L * 1000L,
            15L * 60L * 1000L,
            5L * 60L * 1000L
    };

    private Scheduler() {}

    static void scheduleAll(Context context, long endsAtMs) {
        long failureAtMs = endsAtMs + TimerState.GRACE_MS;
        long showCountdownAt = failureAtMs - TimerState.LOW_TIME_MS;
        if (showCountdownAt > System.currentTimeMillis()) {
            schedule(context, REQUEST_SHOW_COUNTDOWN, showCountdownAt, ActionReceiver.ACTION_SHOW_COUNTDOWN, 0L);
        }
        schedule(context, REQUEST_ALARM, endsAtMs, ActionReceiver.ACTION_ALARM, 0L);
        for (int i = 0; i < REMIND_BEFORE_MS.length; i++) {
            long remindAt = failureAtMs - REMIND_BEFORE_MS[i];
            if (remindAt > System.currentTimeMillis()) {
                schedule(context, REQUEST_REMINDER_BASE + i, remindAt, ReminderReceiver.ACTION_REMIND, REMIND_BEFORE_MS[i]);
            }
        }
    }

    static void cancelAll(Context context) {
        cancel(context, REQUEST_ALARM, ActionReceiver.ACTION_ALARM);
        cancel(context, REQUEST_SHOW_COUNTDOWN, ActionReceiver.ACTION_SHOW_COUNTDOWN);
        for (int i = 0; i < REMIND_BEFORE_MS.length; i++) {
            cancel(context, REQUEST_REMINDER_BASE + i, ReminderReceiver.ACTION_REMIND);
        }
    }

    private static void schedule(Context context, int requestCode, long atMs, String action, long remainingAtFireMs) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, action.equals(ReminderReceiver.ACTION_REMIND) ? ReminderReceiver.class : ActionReceiver.class)
                        .setAction(action)
                        .putExtra(ReminderReceiver.EXTRA_REMAINING_MS, remainingAtFireMs),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(atMs, pendingIntent), pendingIntent);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pendingIntent);
    }

    private static void cancel(Context context, int requestCode, String action) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, action.equals(ReminderReceiver.ACTION_REMIND) ? ReminderReceiver.class : ActionReceiver.class)
                        .setAction(action),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}
