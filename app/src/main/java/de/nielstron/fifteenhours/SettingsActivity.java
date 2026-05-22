package de.nielstron.fifteenhours;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private Switch bedtimeAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(makeView());
    }

    private View makeView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        root.setBackgroundColor(Color.rgb(247, 244, 238));

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.rgb(20, 92, 84));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(28), 0, 0);
        root.addView(row, rowParams);

        TextView label = new TextView(this);
        label.setText("Bedtime alarm");
        label.setTextColor(Color.rgb(60, 57, 51));
        label.setTextSize(18);
        row.addView(label, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));

        bedtimeAlarm = new Switch(this);
        bedtimeAlarm.setChecked(AlarmSettings.bedtimeAlarmEnabled(this));
        bedtimeAlarm.setOnCheckedChangeListener(this::setBedtimeAlarmEnabled);
        row.addView(bedtimeAlarm, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private void setBedtimeAlarmEnabled(CompoundButton button, boolean enabled) {
        AlarmSettings.setBedtimeAlarmEnabled(this, enabled);
        if (!TimerState.active(this)) {
            return;
        }
        if (enabled) {
            long endsAt = TimerState.endsAt(this);
            Scheduler.cancelAll(this);
            Scheduler.scheduleAll(this, endsAt);
        } else {
            Scheduler.cancelAlarm(this);
            AlarmState.dismiss(this);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
