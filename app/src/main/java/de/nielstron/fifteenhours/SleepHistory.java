package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SleepHistory {
    private static final String PREFS = "sleep_history";
    private static final String RECORDS = "records";
    private static final int WINDOW_DAYS = 15;

    private SleepHistory() {}

    static void record(Context context, boolean inTime) {
        Map<LocalDate, Boolean> records = records(context);
        records.put(LocalDate.now(), inTime);
        persist(context, trim(records));
    }

    static Boolean[] last15(Context context) {
        Map<LocalDate, Boolean> records = records(context);
        Boolean[] days = new Boolean[WINDOW_DAYS];
        LocalDate oldest = LocalDate.now().minusDays(WINDOW_DAYS - 1L);
        for (int i = 0; i < WINDOW_DAYS; i++) {
            days[i] = records.get(oldest.plusDays(i));
        }
        return days;
    }

    static int streak(Context context) {
        return streak(records(context));
    }

    private static int streak(Map<LocalDate, Boolean> records) {
        LocalDate latest = null;
        for (LocalDate date : records.keySet()) {
            if (latest == null || date.isAfter(latest)) {
                latest = date;
            }
        }
        if (latest == null) {
            return 0;
        }

        int count = 0;
        LocalDate cursor = latest;
        while (Boolean.TRUE.equals(records.get(cursor))) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    private static Map<LocalDate, Boolean> trim(Map<LocalDate, Boolean> records) {
        LocalDate cutoff = LocalDate.now().minusDays(WINDOW_DAYS - 1L);
        Map<LocalDate, Boolean> trimmed = new HashMap<>();
        for (Map.Entry<LocalDate, Boolean> entry : records.entrySet()) {
            if (!entry.getKey().isBefore(cutoff)) {
                trimmed.put(entry.getKey(), entry.getValue());
            }
        }
        return trimmed;
    }

    private static Map<LocalDate, Boolean> records(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String encoded = prefs.getString(RECORDS, "");
        Map<LocalDate, Boolean> records = new HashMap<>();
        if (encoded.isEmpty()) {
            return records;
        }
        for (String item : encoded.split(";")) {
            String[] parts = item.split("=");
            records.put(LocalDate.parse(parts[0]), "1".equals(parts[1]));
        }
        return records;
    }

    private static void persist(Context context, Map<LocalDate, Boolean> records) {
        List<String> encoded = new ArrayList<>();
        for (Map.Entry<LocalDate, Boolean> entry : records.entrySet()) {
            encoded.add(entry.getKey() + "=" + (entry.getValue() ? "1" : "0"));
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(RECORDS, String.join(";", encoded))
                .apply();
    }
}
