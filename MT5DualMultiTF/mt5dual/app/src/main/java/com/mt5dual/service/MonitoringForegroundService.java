package com.mt5dual.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.mt5dual.DualApplication;
import com.mt5dual.R;
import com.mt5dual.ui.MainActivity;

public class MonitoringForegroundService extends Service {
    public static final String CHANNEL_ID = "mt5_dual_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL = 60_000L;

    private static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            DualApplication.getInstance().getDualEngineManager().runPeriodicSafetyCheck();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannelIfNeeded();
        startForeground(NOTIFICATION_ID, buildNotification());
        running = true;
        handler.postDelayed(tick, INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        running = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        running = false;
        super.onDestroy();
    }

    public static boolean isRunning() {
        return running;
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.monitoring_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.monitoring_channel_desc));
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_monitor_status)
                .setContentTitle(getString(R.string.monitoring_notification_title))
                .setContentText(getString(R.string.monitoring_notification_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
