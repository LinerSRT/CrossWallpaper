package com.liner.videoscreensaver;

import android.net.Uri;

public class Wallpaper {
    private final String path;
    private final Uri uri;

    public Wallpaper(String path) {
        this.path = path;
        this.uri = Uri.parse( "file:///android_asset/"+path);
    }

    public String getPath() {
        return path;
    }

    public Uri getUri() {
        return uri;
    }
}
