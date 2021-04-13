package com.liner.videoscreensaver;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.liner.videoscreensaver.render.GLWallpaperService;


public class MainActivity extends AppCompatActivity {
    private RoundedVideoView crossRedView;
    private RoundedVideoView crossBlackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        crossBlackView = findViewById(R.id.crossBlackView);
        crossRedView = findViewById(R.id.crossRedView);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.disabled));
        crossBlackView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        });
        crossBlackView.setVideoURI(getUriFromRawFile(this, R.raw.cross_black));
        crossBlackView.setOnClickListener(view -> crossBlackView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .withEndAction(() -> crossBlackView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .withEndAction(() -> {
                            Core.selectedWallpaper = Core.cross_black;
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
        crossRedView.setVideoURI(getUriFromRawFile(this, R.raw.cross_red));
        crossRedView.setOnClickListener(view -> crossRedView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .withEndAction(() -> crossRedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .withEndAction(() -> {
                            Core.selectedWallpaper = Core.cross_red;
                            final Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    new ComponentName(MainActivity.this, GLWallpaperService.class));
                            startActivity(intent);
                        }).setInterpolator(new OvershootInterpolator()).setDuration(200).start()).setInterpolator(new OvershootInterpolator()).setDuration(200).start());
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