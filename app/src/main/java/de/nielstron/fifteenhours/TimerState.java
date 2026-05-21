package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

final class TimerState {
    static final long DURATION_MS = 15L * 60L * 60L * 1000L;
    static final long GRACE_MS = 40L * 60L * 1000L;
    static final long LOW_TIME_MS = 2L * 60L * 60L * 1000L;

    private static final String PREFS = "timer";
    private static final String STARTED_AT = "started_at";
    private static final String ENDS_AT = "ends_at";

    private TimerState() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void start(Context context, long nowMs) {
        prefs(context).edit()
                .putLong(STARTED_AT, nowMs)
                .putLong(ENDS_AT, nowMs + DURATION_MS)
                .apply();
    }

    static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    static long endsAt(Context context) {
        return prefs(context).getLong(ENDS_AT, 0L);
    }

    static boolean active(Context context) {
        return endsAt(context) != 0L;
    }

    static long remaining(Context context, long nowMs) {
        return endsAt(context) - nowMs;
    }

    static long failureAt(Context context) {
        return endsAt(context) + GRACE_MS;
    }

    static long notificationStartsAt(Context context) {
        return failureAt(context) - LOW_TIME_MS;
    }

    static boolean inTime(Context context, long nowMs) {
        return remaining(context, nowMs) >= -GRACE_MS;
    }
}
