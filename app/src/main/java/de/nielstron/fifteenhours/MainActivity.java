package de.nielstron.fifteenhours;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public final class MainActivity extends Activity {
    static final String ACTION_START_FROM_NOTIFICATION = "de.nielstron.fifteenhours.START_FROM_NOTIFICATION";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView countdown;
    private LinearLayout historyGrid;
    private Button primary;
    private TextView pickWakeTime;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppNotifications.createChannels(this);
        requestNotificationPermission();
        setContentView(makeView());
        if (!handleStartIntent(getIntent())) {
            ensureCountdownNotification();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleStartIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppVisibility.setForeground(this, true);
        handler.post(tick);
        String pending = OutcomeState.takePending(this);
        if (OutcomeState.MISSED.equals(pending)) {
            showOutcome("😴", "try again tomorrow!");
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        AppVisibility.setForeground(this, false);
        super.onPause();
    }

    private View makeView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        root.setBackgroundColor(Color.rgb(247, 244, 238));

        countdown = new TextView(this);
        countdown.setTextColor(Color.rgb(20, 92, 84));
        countdown.setTypeface(Typeface.DEFAULT_BOLD);
        countdown.setTextSize(76);
        countdown.setIncludeFontPadding(false);
        countdown.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        countdownParams.setMargins(0, dp(22), 0, dp(28));
        root.addView(countdown, countdownParams);

        primary = new Button(this);
        primary.setText("I woke up");
        primary.setTextSize(18);
        primary.setAllCaps(false);
        primary.setMinHeight(dp(56));
        primary.setOnClickListener(v -> {
            if (TimerState.active(this)) {
                goingToSleep();
            } else {
                startCountdown();
            }
        });
        root.addView(primary, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        pickWakeTime = new TextView(this);
        pickWakeTime.setText("Pick wake time");
        pickWakeTime.setTextColor(Color.rgb(20, 92, 84));
        pickWakeTime.setTextSize(16);
        pickWakeTime.setGravity(Gravity.CENTER);
        pickWakeTime.setMinHeight(dp(44));
        pickWakeTime.setClickable(true);
        pickWakeTime.setOnClickListener(v -> showWakeTimePicker());
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pickParams.setMargins(0, dp(8), 0, 0);
        root.addView(pickWakeTime, pickParams);

        historyGrid = new LinearLayout(this);
        historyGrid.setGravity(Gravity.CENTER);
        historyGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.setMargins(0, dp(26), 0, 0);
        root.addView(historyGrid, gridParams);

        render();
        return root;
    }

    private void startCountdown() {
        startCountdownFrom(System.currentTimeMillis());
    }

    private boolean handleStartIntent(Intent intent) {
        if (intent != null && ACTION_START_FROM_NOTIFICATION.equals(intent.getAction())) {
            startCountdown();
            return true;
        }
        return false;
    }

    private void startCountdownFrom(long startedAtMs) {
        TimerState.start(this, startedAtMs);
        long endsAt = TimerState.endsAt(this);
        Scheduler.cancelAll(this);
        Scheduler.scheduleAll(this, endsAt);
        AppNotifications.showCountdown(this);
        maybeOpenExactAlarmSettings();
        render();
    }

    private void goingToSleep() {
        boolean inTime = TimerState.inTime(this, System.currentTimeMillis());
        SleepHistory.record(this, inTime);
        TimerState.clear(this);
        Scheduler.cancelAll(this);
        AlarmSoundService.stop(this);
        AppNotifications.showCountdown(this);
        render();
        showOutcome(inTime ? "🎉" : "😴", inTime ? "congrats!" : "try again tomorrow!");
    }

    private void showWakeTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar wokeAt = Calendar.getInstance();
                    wokeAt.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    wokeAt.set(Calendar.MINUTE, minute);
                    wokeAt.set(Calendar.SECOND, 0);
                    wokeAt.set(Calendar.MILLISECOND, 0);
                    if (wokeAt.getTimeInMillis() > System.currentTimeMillis()) {
                        wokeAt.add(Calendar.DAY_OF_YEAR, -1);
                    }
                    startCountdownFrom(wokeAt.getTimeInMillis());
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true);
        dialog.show();
    }

    private void render() {
        long now = System.currentTimeMillis();
        long remaining = TimerState.remaining(this, now);
        boolean active = TimerState.active(this);
        countdown.setText(formatDuration(active ? remaining : TimerState.DURATION_MS));
        primary.setText(active ? "Going to sleep" : "I woke up");
        renderHistoryGrid();
    }

    private void renderHistoryGrid() {
        historyGrid.removeAllViews();
        Boolean[] days = SleepHistory.last15(this);
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, rowIndex == 0 ? 0 : dp(10), 0, 0);
            historyGrid.addView(row, rowParams);

            for (int columnIndex = 0; columnIndex < 5; columnIndex++) {
                int index = rowIndex * 5 + columnIndex;
                View circle = new View(this);
                circle.setContentDescription(historyDescription(days[index], index));
                circle.setBackground(historyCircle(days[index]));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(24), dp(24));
                params.setMargins(dp(7), 0, dp(7), 0);
                row.addView(circle, params);
            }
        }
    }

    private GradientDrawable historyCircle(Boolean value) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        if (value == null) {
            drawable.setColor(Color.TRANSPARENT);
            drawable.setStroke(dp(2), Color.rgb(150, 147, 140));
        } else if (value) {
            drawable.setColor(Color.rgb(20, 92, 84));
        } else {
            drawable.setColor(Color.rgb(157, 48, 48));
        }
        return drawable;
    }

    private String historyDescription(Boolean value, int index) {
        int daysAgo = 14 - index;
        String day = daysAgo == 0 ? "today" : daysAgo + " days ago";
        if (value == null) {
            return "No sleep record " + day;
        }
        return (value ? "In time " : "Missed ") + day;
    }

    private void showOutcome(String emoji, String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        GradientDrawable dialogBackground = new GradientDrawable();
        dialogBackground.setColor(Color.rgb(247, 244, 238));
        dialogBackground.setCornerRadius(dp(8));
        content.setBackground(dialogBackground);

        TextView emojiView = new TextView(this);
        emojiView.setText(emoji);
        emojiView.setTextSize(78);
        emojiView.setGravity(Gravity.CENTER);
        emojiView.setAlpha(0f);
        emojiView.setScaleX(0.45f);
        emojiView.setScaleY(0.45f);
        content.addView(emojiView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(122)));

        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextColor(Color.rgb(60, 57, 51));
        messageView.setTextSize(22);
        messageView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.setMargins(0, dp(18), 0, 0);
        content.addView(messageView, messageParams);

        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(emojiView, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(emojiView, View.SCALE_X, 0.45f, 1.2f, 1f),
                ObjectAnimator.ofFloat(emojiView, View.SCALE_Y, 0.45f, 1.2f, 1f),
                ObjectAnimator.ofFloat(emojiView, View.ROTATION, -10f, 8f, 0f));
        animatorSet.setDuration(650L);
        animatorSet.start();
        content.postDelayed(dialog::dismiss, 2600L);
    }

    private void ensureCountdownNotification() {
        AppNotifications.showCountdown(this);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    private void maybeOpenExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        AlarmManager alarmManager = getSystemService(AlarmManager.class);
        if (alarmManager.canScheduleExactAlarms()) {
            return;
        }
        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:" + getPackageName())));
    }

    private static String formatDuration(long ms) {
        boolean negative = ms < 0L;
        long seconds = Math.abs(ms) / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return String.format(Locale.US, "%s%02d:%02d:%02d", negative ? "-" : "", hours, minutes, secs);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
