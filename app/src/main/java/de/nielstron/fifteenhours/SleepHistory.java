package de.nielstron.fifteenhours;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SleepHistory {
    private static final String PREFS = "sleep_history";
    private static final String RECORDS = "records";
    private static final int WINDOW_DAYS = 15;
    private static final long LEGACY_MISSED_MS = TimerState.DURATION_MS + TimerState.GRACE_MS + 1L;

    private SleepHistory() {}

    static final class Entry {
        final LocalDate date;
        final long elapsedMs;

        Entry(LocalDate date, long elapsedMs) {
            this.date = date;
            this.elapsedMs = elapsedMs;
        }

        double hours() {
            return elapsedMs / 3600000.0;
        }

        float fill() {
            return Math.min(1f, Math.max(0f, elapsedMs / (float) TimerState.DURATION_MS));
        }

        boolean inTime() {
            return elapsedMs <= TimerState.DURATION_MS + TimerState.GRACE_MS;
        }
    }

    static void record(Context context, long elapsedMs) {
        Map<LocalDate, Long> records = records(context);
        records.put(LocalDate.now(), elapsedMs);
        persist(context, records);
    }

    static Entry[] last15(Context context, long nowMs) {
        Map<LocalDate, Long> records = records(context);
        if (TimerState.active(context)) {
            records.put(LocalDate.now(), nowMs - TimerState.startedAt(context));
        }
        List<Map.Entry<LocalDate, Long>> sorted = new ArrayList<>(records.entrySet());
        Collections.sort(sorted, Comparator.comparing(Map.Entry::getKey));

        Entry[] entries = new Entry[WINDOW_DAYS];
        int start = Math.max(0, (((sorted.size() - 1) / 5) - 2) * 5);
        for (int i = start; i < sorted.size() && i - start < WINDOW_DAYS; i++) {
            Map.Entry<LocalDate, Long> entry = sorted.get(i);
            entries[i - start] = new Entry(entry.getKey(), entry.getValue());
        }
        return entries;
    }

    static String dateDescription(LocalDate date) {
        long daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (daysAgo == 0L) {
            return "today";
        }
        if (daysAgo == 1L) {
            return "yesterday";
        }
        return daysAgo + " days ago";
    }

    static int streak(Context context) {
        return streak(records(context));
    }

    private static int streak(Map<LocalDate, Long> records) {
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
        while (records.containsKey(cursor)
                && records.get(cursor) <= TimerState.DURATION_MS + TimerState.GRACE_MS) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    private static Map<LocalDate, Long> records(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String encoded = prefs.getString(RECORDS, "");
        Map<LocalDate, Long> records = new HashMap<>();
        if (encoded.isEmpty()) {
            return records;
        }
        for (String item : encoded.split(";")) {
            String[] parts = item.split("=");
            records.put(LocalDate.parse(parts[0]), decodeElapsed(parts[1]));
        }
        return records;
    }

    private static long decodeElapsed(String value) {
        if ("1".equals(value)) {
            return TimerState.DURATION_MS;
        }
        if ("0".equals(value)) {
            return LEGACY_MISSED_MS;
        }
        return Long.parseLong(value);
    }

    private static void persist(Context context, Map<LocalDate, Long> records) {
        List<Map.Entry<LocalDate, Long>> sorted = new ArrayList<>(records.entrySet());
        Collections.sort(sorted, Comparator.comparing(Map.Entry::getKey));
        List<String> encoded = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : sorted) {
            encoded.add(entry.getKey() + "=" + entry.getValue());
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(RECORDS, String.join(";", encoded))
                .apply();
    }
}
