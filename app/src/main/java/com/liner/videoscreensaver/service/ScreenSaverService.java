package com.liner.videoscreensaver.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.liner.videoscreensaver.Constant;
import com.liner.videoscreensaver.Core;
import com.liner.videoscreensaver.PM;
import com.liner.videoscreensaver.R;
import com.liner.videoscreensaver.WakeLocker;
import com.liner.videoscreensaver.receiver.RestartReceiver;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.provider.Settings.canDrawOverlays;
import static com.liner.videoscreensaver.Constant.INACTIVE_CHECK_DELAY;
import static com.liner.videoscreensaver.Constant.SHOWN_DELAY;

@SuppressWarnings("deprecation")
public class ScreenSaverService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerometerValues[];
    private View parentView;
    private boolean isShown = false;
    private boolean isDeviceInactive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        PM.init(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerValues = new float[3];
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "CVSService")
                .setSmallIcon(R.drawable.image)
                .setContentTitle("Приложение запущено")
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(
                    "CVSService",
                    "CVSService",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
        startForeground(9, notificationBuilder.build());
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (((boolean) PM.get(Constant.KEY_SCREENSAVER_ENABLED, false))) {
            WindowManager windowManager = ((WindowManager) getSystemService(WINDOW_SERVICE));
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            parentView = layoutInflater.inflate(R.layout.screensaver_layout, null);
            parentView.post(() -> parentView.setVisibility(View.GONE));
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                    PixelFormat.TRANSLUCENT);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !canDrawOverlays(ScreenSaverService.this)) {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(settingsIntent);
            }
            windowManager.addView(parentView, layoutParams);
            VideoView screenSaverVideoView = parentView.findViewById(R.id.screenSaverVideoView);
            screenSaverVideoView.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.setVolume(0, 0);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            });
            screenSaverVideoView.setVideoURI(getUriFromRawFile(ScreenSaverService.this, (Core.selectedWallpaper == Core.cross_black_texture) ? R.raw.cross_black_texture : R.raw.cross_red_texture));
            parentView.setOnTouchListener((v, event) -> {
                parentView.post(() -> parentView.setVisibility(View.GONE));
                isShown = false;
                WakeLocker.release();
                return true;
            });
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (!isShown && ((boolean) PM.get(Constant.KEY_SCREENSAVER_ENABLED, false)) && isDeviceInactive) {
                        parentView.post(() -> parentView.setVisibility(View.VISIBLE));
                        isShown = true;
                        WakeLocker.acquireHalf(ScreenSaverService.this, "cross:Enable");
                    }
                }
            };
            timer.scheduleAtFixedRate(timerTask, TimeUnit.SECONDS.toMillis(SHOWN_DELAY), TimeUnit.SECONDS.toMillis(SHOWN_DELAY));
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sensorManager.registerListener(ScreenSaverService.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                }
            }, TimeUnit.SECONDS.toMillis(INACTIVE_CHECK_DELAY),TimeUnit.SECONDS.toMillis(INACTIVE_CHECK_DELAY) );
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        restartService();
    }

    private void restartService() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 5);
        Intent intent = new Intent(this, RestartReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public static Uri getUriFromRawFile(Context context, int rawResourceId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .path(String.valueOf(rawResourceId))
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(sensorEvent.values[0] != 0) {
                isDeviceInactive = (Math.round(accelerometerValues[0]) == Math.round(sensorEvent.values[0])) && Math.round(accelerometerValues[1]) == Math.round(sensorEvent.values[1]);
                accelerometerValues = sensorEvent.values;
                sensorManager.unregisterListener(this);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
