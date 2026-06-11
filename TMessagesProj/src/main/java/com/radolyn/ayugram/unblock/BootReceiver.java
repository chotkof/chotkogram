package com.radolyn.ayugram.unblock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            if (UnblockController.isEnabled(context)
                    && UnblockController.prefs(context).getBoolean(UnblockController.KEY_AUTOSTART, false)) {
                UnblockController.start(context);
            }
        }
    }
}
