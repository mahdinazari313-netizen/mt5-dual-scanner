package com.mt5dual.alarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mt5dual.DualApplication;
import com.mt5dual.core.Direction;
import com.mt5dual.core.Timeframe;

public class SilenceActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AlarmNotifier.ACTION_SILENCE.equals(intent.getAction())) return;

        String symbol = intent.getStringExtra(AlarmNotifier.EXTRA_SYMBOL);
        String timeframeRaw = intent.getStringExtra(AlarmNotifier.EXTRA_TIMEFRAME);
        String directionRaw = intent.getStringExtra(AlarmNotifier.EXTRA_DIRECTION);

        Timeframe timeframe = Timeframe.fromString(timeframeRaw);
        Direction direction = Direction.fromRawText(directionRaw);

        if (symbol == null || timeframe == null || direction == null) return;

        // گام ۱: Trigger را سایلنت کن (Repeat متوقف می‌شود)
        DualApplication.getInstance().getDualEngineManager()
                .silence(symbol, timeframe, direction);

        // گام ۲: Notification را Cancel کن (مثل v15)
        int notificationId = (symbol + "|" + timeframe.name() + "|" + direction.name())
                .hashCode();
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId);
        }
    }
}
