package com.liner.videoscreensaver.render;

import android.content.Context;
import android.opengl.GLSurfaceView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.SimpleExoPlayer;

abstract class GLWallpaperRenderer implements GLSurfaceView.Renderer {
    final Context context;
    GLWallpaperRenderer(@NonNull final Context context) {
        this.context = context;
    }
    @NonNull
    protected Context getContext() {
        return context;
    }

    abstract void setSourcePlayer(@NonNull final SimpleExoPlayer exoPlayer);
    abstract void setScreenSize(int width, int height);
    abstract void setVideoSizeAndRotation(int width, int height, int rotation);
    abstract void setOffset(float xOffset, float yOffset);
}