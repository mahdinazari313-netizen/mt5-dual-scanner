package com.mt5dual.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.mt5dual.DualApplication;
import com.mt5dual.core.MetaTraderSignalParser;
import com.mt5dual.core.RawParsedSignal;
import com.mt5dual.core.SignalParser;
import com.mt5dual.core.SignalStateManager;
import com.mt5dual.core.SignalValidator;

public class MT5NotificationListenerService extends NotificationListenerService {
    private static final String TAG = "MT5DualListener";
    private static final String MT5_PACKAGE_NAME = "net.metaquotes.metatrader5";

    private final SignalParser parser = new MetaTraderSignalParser();
    private final SignalValidator validator = new SignalValidator();

    @Override
    public void onCreate() {
        super.onCreate();
        DualApplication.getInstance();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !MT5_PACKAGE_NAME.equals(sbn.getPackageName())) return;

        String text = extractNotificationText(sbn);
        if (text == null) return;

        RawParsedSignal raw = parser.tryParse(text);
        if (raw == null) return;

        long receivedAt = System.currentTimeMillis();
        SignalValidator.Result result = validator.validate(raw, receivedAt);
        if (!result.isValid()) {
            Log.w(TAG, "Rejected MT5 signal: " + result.rejectionReason);
            return;
        }

        SignalStateManager manager = DualApplication.getInstance().getSignalStateManager();
        manager.upsertSignal(result.signal);
    }

    private String extractNotificationText(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null) return null;

        Bundle extras = notification.extras;
        if (extras == null) return null;

        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text != null) return text.toString();

        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        return bigText == null ? null : bigText.toString();
    }
}
