package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

final class AlarmSettings {
    private static final String PREFS = "alarm_settings";
    private static final String BEDTIME_ALARM_ENABLED = "bedtime_alarm_enabled";

    private AlarmSettings() {}

    static boolean bedtimeAlarmEnabled(Context context) {
        return prefs(context).getBoolean(BEDTIME_ALARM_ENABLED, true);
    }

    static void setBedtimeAlarmEnabled(Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(BEDTIME_ALARM_ENABLED, enabled)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
