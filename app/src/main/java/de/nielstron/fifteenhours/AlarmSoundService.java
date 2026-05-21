package de.nielstron.fifteenhours;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

public final class AlarmSoundService extends Service {
    private MediaPlayer player;

    static void start(Context context) {
        context.startForegroundService(new Intent(context, AlarmSoundService.class));
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, AlarmSoundService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppNotifications.createChannels(this);
        startForeground(AppNotifications.ALARM_ID, AppNotifications.alarm(this));
        if (player == null) {
            Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            player = MediaPlayer.create(this, alarm);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setLooping(true);
            player.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
