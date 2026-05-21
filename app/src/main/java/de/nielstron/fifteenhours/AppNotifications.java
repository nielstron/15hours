package de.nielstron.fifteenhours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

final class AppNotifications {
    static final int COUNTDOWN_ID = 15;
    static final int REMINDER_ID = 16;
    static final int ALARM_ID = 17;
    static final String COUNTDOWN_CHANNEL = "countdown";
    static final String REMINDER_CHANNEL = "reminders";
    static final String ALARM_CHANNEL = "alarm";

    private AppNotifications() {}

    static void showCountdown(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        createChannels(context);
        if (TimerState.active(context)
                && System.currentTimeMillis() < TimerState.notificationStartsAt(context)) {
            manager.cancel(COUNTDOWN_ID);
            return;
        }
        manager.notify(COUNTDOWN_ID, countdown(context, TimerState.endsAt(context)));
    }

    static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                COUNTDOWN_CHANNEL,
                "Countdown",
                NotificationManager.IMPORTANCE_LOW));

        NotificationChannel reminders = new NotificationChannel(
                REMINDER_CHANNEL,
                "Low time reminders",
                NotificationManager.IMPORTANCE_HIGH);
        reminders.enableVibration(true);
        manager.createNotificationChannel(reminders);

        NotificationChannel alarm = new NotificationChannel(
                ALARM_CHANNEL,
                "Sleep alarm",
                NotificationManager.IMPORTANCE_HIGH);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        alarm.setSound(alarmSound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        alarm.enableVibration(true);
        manager.createNotificationChannel(alarm);
    }

    static Notification countdown(Context context, long endsAtMs) {
        PendingIntent openApp = PendingIntent.getActivity(
                context,
                1,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent startAndOpen = PendingIntent.getActivity(
                context,
                2,
                new Intent(context, MainActivity.class)
                        .setAction(MainActivity.ACTION_START_FROM_NOTIFICATION)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        boolean active = endsAtMs != 0L;
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("15 Hours")
                .setContentIntent(active ? openApp : startAndOpen)
                .setOngoing(true)
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(COUNTDOWN_CHANNEL);
        }

        if (active) {
            builder.setContentText("Sleep deadline")
                    .setWhen(endsAtMs)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true);
        } else {
            builder.setContentText("Tap when you wake up")
                    .addAction(new Notification.Action.Builder(
                            R.drawable.ic_notification,
                            "I woke up",
                            startAndOpen).build());
        }

        return builder.build();
    }

    static Notification reminder(Context context, long remainingMs) {
        PendingIntent openApp = PendingIntent.getActivity(
                context,
                3,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Sleep soon")
                .setContentText(formatShort(remainingMs) + " left")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(REMINDER_CHANNEL);
        }
        return builder.build();
    }

    static Notification alarm(Context context) {
        PendingIntent stop = PendingIntent.getBroadcast(
                context,
                4,
                new Intent(context, ActionReceiver.class).setAction(ActionReceiver.ACTION_STOP_ALARM),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("15 hours are up")
                .setContentText("40 minutes to go to sleep")
                .setContentIntent(stop)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_notification,
                        "Dismiss",
                        stop).build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(ALARM_CHANNEL);
        }
        return builder.build();
    }

    private static String formatShort(long ms) {
        long totalMinutes = Math.max(0L, ms) / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
