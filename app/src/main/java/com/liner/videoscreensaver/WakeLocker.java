package com.liner.videoscreensaver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

public class WakeLocker {
    private static PowerManager.WakeLock wakeLock;

    @SuppressLint("WakelockTimeout")
    public static void acquire(Context context, String tag) {
        if (wakeLock != null) wakeLock.release();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, tag);
        wakeLock.acquire();
    }

    @SuppressLint("WakelockTimeout")
    public static void acquireHalf(Context context, String tag){
        if (wakeLock != null) wakeLock.release();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, tag);
        wakeLock.acquire();
    }

    public static void release() {
        if (wakeLock != null) wakeLock.release(); wakeLock = null;
    }
}
