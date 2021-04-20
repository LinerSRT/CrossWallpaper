package com.liner.videoscreensaver;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.liner.videoscreensaver.render.GLWallpaperService;
import com.liner.videoscreensaver.service.ScreenSaverService;

import static android.provider.Settings.canDrawOverlays;


public class MainActivity extends AppCompatActivity {
    private RoundedVideoView crossRedView;
    private RoundedVideoView crossBlackView;
    private SwitchMaterial enableScreenSaver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        crossBlackView = findViewById(R.id.crossBlackView);
        crossRedView = findViewById(R.id.crossRedView);
        enableScreenSaver = findViewById(R.id.enableScreensaver);
        enableScreenSaver.setChecked(PM.get(Constant.KEY_SCREENSAVER_ENABLED, false));
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.disabled));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!canDrawOverlays(MainActivity.this)) {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(settingsIntent);
            }
        }
        crossBlackView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        });
        crossBlackView.setVideoURI(getUriFromRawFile(MainActivity.this, R.raw.cross_black_texture));
        crossBlackView.setOnClickListener(view -> crossBlackView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .withEndAction(() -> crossBlackView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .withEndAction(() -> {
                            Core.selectedWallpaper = Core.cross_black_texture;
                            final Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    new ComponentName(MainActivity.this, GLWallpaperService.class));
                            startActivity(intent);
                        }).setInterpolator(new OvershootInterpolator()).setDuration(200).start()).setInterpolator(new OvershootInterpolator()).setDuration(200).start());
        crossRedView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        });
        crossRedView.setVideoURI(getUriFromRawFile(MainActivity.this, R.raw.cross_red_texture));
        crossRedView.setOnClickListener(view -> crossRedView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .withEndAction(() -> crossRedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .withEndAction(() -> {
                            Core.selectedWallpaper = Core.cross_red_texture;
                            final Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    new ComponentName(MainActivity.this, GLWallpaperService.class));
                            startActivity(intent);
                        }).setInterpolator(new OvershootInterpolator()).setDuration(200).start()).setInterpolator(new OvershootInterpolator()).setDuration(200).start());

        enableScreenSaver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PM.put(Constant.KEY_SCREENSAVER_ENABLED, enableScreenSaver.isChecked());
                Intent serviceIntent = new Intent(MainActivity.this, ScreenSaverService.class);
                if(enableScreenSaver.isChecked()){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } else {
                    stopService(serviceIntent);
                }
            }
        });
        Intent serviceIntent = new Intent(MainActivity.this, ScreenSaverService.class);
        if(enableScreenSaver.isChecked()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            stopService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        crossBlackView.setScaleX(1f);
        crossBlackView.setScaleY(1f);
        crossRedView.setScaleX(1f);
        crossRedView.setScaleY(1f);
    }

    public static Uri getUriFromRawFile(Context context, int rawResourceId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .path(String.valueOf(rawResourceId))
                .build();
    }
}