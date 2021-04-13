package com.liner.videoscreensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.VideoView;

public class RoundedVideoView extends VideoView {
    public RoundedVideoView(Context context) {
        super(context);
    }

    public RoundedVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        clip(canvas);
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        clip(canvas);
        super.dispatchDraw(canvas);
    }

    private void clip(Canvas canvas) {
        Path path = new Path();
        path.reset();
        path.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), 16, 16, Path.Direction.CW);
        path.close();
        canvas.clipPath(path);
    }
}
