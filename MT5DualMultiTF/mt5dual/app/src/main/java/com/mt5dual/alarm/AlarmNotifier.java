package com.mt5dual.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mt5dual.R;
import com.mt5dual.core.Signal;
import com.mt5dual.dual.DualAlarmListener;
import com.mt5dual.dual.DualTrigger;
import com.mt5dual.ui.MainActivity;

public class AlarmNotifier implements DualAlarmListener {
    public static final String CHANNEL_ID = "mt5_dual_alarm_channel";
    public static final String ACTION_SILENCE = "com.mt5dual.ACTION_SILENCE_TRIGGER";
    public static final String EXTRA_SYMBOL = "extra_symbol";
    public static final String EXTRA_TIMEFRAME = "extra_timeframe";
    public static final String EXTRA_DIRECTION = "extra_direction";

    private final Context appContext;

    public AlarmNotifier(Context context) {
        appContext = context.getApplicationContext();
        createChannelIfNeeded();
    }

    @Override public void onNewTrigger(DualTrigger trigger) { postNotification(trigger, true); }
    @Override public void onRepeatAlarm(DualTrigger trigger) { postNotification(trigger, true); }
    @Override public void onTriggerExpired(DualTrigger trigger) { cancel(trigger); }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    appContext.getString(R.string.alarm_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(appContext.getString(R.string.alarm_channel_desc));
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
    }

    public int notificationIdFor(DualTrigger trigger) {
        return (trigger.getSymbol() + "|" + trigger.getTimeframe() + "|" + trigger.getDirection()).hashCode();
    }

    private void postNotification(DualTrigger trigger, boolean sound) {
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager == null) return;

        int id = notificationIdFor(trigger);
        Intent openIntent = new Intent(appContext, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, id, openIntent, flags);

        Intent silenceIntent = new Intent(appContext, SilenceActionReceiver.class)
                .setAction(ACTION_SILENCE)
                .putExtra(EXTRA_SYMBOL, trigger.getSymbol())
                .putExtra(EXTRA_TIMEFRAME, trigger.getTimeframe().name())
                .putExtra(EXTRA_DIRECTION, trigger.getDirection().name());

        int silenceFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) silenceFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent silencePendingIntent = PendingIntent.getBroadcast(
                appContext, id, silenceIntent, silenceFlags);

        Signal reference = trigger.getCombination().get(0);
        Signal incoming = trigger.getCombination().get(1);

        String body = appContext.getString(
                R.string.alarm_notification_body,
                reference.getTimeframe().name(), formatPrice(reference.getPrice()),
                incoming.getTimeframe().name(), formatPrice(incoming.getPrice()));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dual_triggers)
                .setContentTitle(appContext.getString(
                        R.string.alarm_notification_title,
                        trigger.getSymbol(), trigger.getTimeframe().name(), trigger.getDirection().name()))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_monitor_status,
                        appContext.getString(R.string.action_silent),
                        silencePendingIntent).build());

        if (sound) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        }

        manager.notify(id, builder.build());
    }

    private void cancel(DualTrigger trigger) {
        NotificationManager manager = appContext.getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(notificationIdFor(trigger));
    }

    private String formatPrice(double value) {
        return String.valueOf(value);
    }
}
