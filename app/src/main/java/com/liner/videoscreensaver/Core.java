package com.liner.videoscreensaver;

import android.app.Application;


public class Core extends Application {
    public static final Wallpaper cross_black  = new Wallpaper("video/cross_black.mp4");
    public static final Wallpaper cross_red  = new Wallpaper("video/cross_red.mp4");
    public static final Wallpaper cross_black_texture  = new Wallpaper("video/cross_black_texture.mp4");
    public static final Wallpaper cross_red_texture= new Wallpaper("video/cross_red_texture.mp4");
    public static Wallpaper cross_fade  = new Wallpaper("video/cross_fade.mp4");
    public static Wallpaper selectedWallpaper = cross_black;
    public static final boolean fitCenter = true;

    @Override
    public void onCreate() {
        super.onCreate();
        PM.init(this);
    }
}
