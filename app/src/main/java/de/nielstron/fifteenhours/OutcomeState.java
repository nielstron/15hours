package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

final class OutcomeState {
    private static final String PREFS = "outcome";
    private static final String PENDING = "pending";
    static final String MISSED = "missed";

    private OutcomeState() {}

    static void setPending(Context context, String outcome) {
        prefs(context).edit().putString(PENDING, outcome).apply();
    }

    static String takePending(Context context) {
        SharedPreferences prefs = prefs(context);
        String outcome = prefs.getString(PENDING, "");
        prefs.edit().remove(PENDING).apply();
        return outcome;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
