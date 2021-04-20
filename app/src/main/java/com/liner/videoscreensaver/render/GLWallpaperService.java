package com.liner.videoscreensaver.render;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.liner.videoscreensaver.Constant;
import com.liner.videoscreensaver.Core;
import com.liner.videoscreensaver.PM;
import com.liner.videoscreensaver.Wallpaper;

import java.io.IOException;

public class GLWallpaperService extends WallpaperService {
    @Override
    public void onCreate() {
        super.onCreate();
        PM.init(this);
        PM.put(Constant.KEY_WALLPAPER_RUNNING, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PM.put(Constant.KEY_WALLPAPER_RUNNING, false);
    }


    class GLWallpaperEngine extends Engine {
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView = null;
        private SimpleExoPlayer exoPlayer = null;
        private MediaSource videoSource = null;
        private DefaultTrackSelector trackSelector = null;
        private Wallpaper wallpaperCard = null;
        private GLWallpaperRenderer renderer = null;
        private boolean allowSlide = false;
        private int videoRotation = 0;
        private int videoWidth = 0;
        private int videoHeight = 0;
        private long progress = 0;

        private class GLWallpaperSurfaceView extends GLSurfaceView {
            public GLWallpaperSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            void onDestroy() {
                super.onDetachedFromWindow();
            }
        }

        GLWallpaperEngine(@NonNull final Context context) {
            this.context = context;
            setTouchEventsEnabled(false);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            allowSlide = false;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            createGLSurfaceView();
            int width = surfaceHolder.getSurfaceFrame().width();
            int height = surfaceHolder.getSurfaceFrame().height();
            renderer.setScreenSize(width, height);
            startPlayer();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (renderer != null) {
                if (visible) {
                    allowSlide = false;
                    glSurfaceView.onResume();
                    startPlayer();
                } else {
                    stopPlayer();
                    glSurfaceView.onPause();
                    allowSlide = false;
                }
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(
                    xOffset, yOffset, xOffsetStep,
                    yOffsetStep, xPixelOffset, yPixelOffset
            );
            if (allowSlide && !isPreview()) {
                renderer.setOffset(0.5f - xOffset, 0.5f - yOffset);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            renderer.setScreenSize(width, height);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            stopPlayer();
            glSurfaceView.onDestroy();
        }

        private void createGLSurfaceView() {
            if (glSurfaceView != null) {
                glSurfaceView.onDestroy();
                glSurfaceView = null;
            }
            glSurfaceView = new GLWallpaperSurfaceView(context);
            final ActivityManager activityManager = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE
            );
            final ConfigurationInfo configInfo = activityManager.getDeviceConfigurationInfo();
            if (configInfo.reqGlEsVersion >= 0x30000) {
                glSurfaceView.setEGLContextClientVersion(3);
                renderer = new GLES30WallpaperRenderer(context);
            } else if (configInfo.reqGlEsVersion >= 0x20000) {
                glSurfaceView.setEGLContextClientVersion(2);
                renderer = new GLES20WallpaperRenderer(context);
            }
            glSurfaceView.setPreserveEGLContextOnPause(true);
            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }


        private void getVideoMetadata() throws IOException {
            final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            final AssetFileDescriptor assetFileDescriptor = getAssets().openFd(wallpaperCard.getPath());
            mediaMetadataRetriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getDeclaredLength());
            assetFileDescriptor.close();
            final String rotation = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            );
            final String width = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            );
            final String height = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            );
            mediaMetadataRetriever.release();
            videoRotation = Integer.parseInt(rotation);
            videoWidth = Integer.parseInt(width);
            videoHeight = Integer.parseInt(height);
        }

        private void startPlayer() {
            if (exoPlayer != null)
                stopPlayer();
            Wallpaper oldWallpaperCard = wallpaperCard;
            wallpaperCard = Core.selectedWallpaper;
            if (wallpaperCard == null)
                return;
            try {
                getVideoMetadata();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
            exoPlayer.setVolume(0.0f);
            trackSelector.setParameters(new DefaultTrackSelector.ParametersBuilder()
                    .setForceHighestSupportedBitrate(true)
                    .setMaxVideoBitrate(Integer.MAX_VALUE)
                    .setRendererDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            );
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            final DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                    context, Util.getUserAgent(context, "com.liner.videoscreensaver")
            );
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(wallpaperCard.getUri());
            renderer.setVideoSizeAndRotation(videoWidth, videoHeight, videoRotation);
            renderer.setSourcePlayer(exoPlayer);
            exoPlayer.prepare(videoSource);
            if (oldWallpaperCard != null && oldWallpaperCard.equals(wallpaperCard))
                exoPlayer.seekTo(progress);
            exoPlayer.setPlayWhenReady(true);
        }

        private void stopPlayer() {
            if (exoPlayer != null) {
                if (exoPlayer.getPlayWhenReady()) {
                    exoPlayer.setPlayWhenReady(false);
                    progress = exoPlayer.getCurrentPosition();
                    exoPlayer.stop();
                }
                exoPlayer.release();
                exoPlayer = null;
            }
            videoSource = null;
            trackSelector = null;
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new GLWallpaperEngine(this);
    }
}
