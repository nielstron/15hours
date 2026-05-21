package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

final class AppVisibility {
    private static final String PREFS = "visibility";
    private static final String FOREGROUND = "foreground";

    private AppVisibility() {}

    static void setForeground(Context context, boolean foreground) {
        prefs(context).edit().putBoolean(FOREGROUND, foreground).apply();
    }

    static boolean foreground(Context context) {
        return prefs(context).getBoolean(FOREGROUND, false);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
