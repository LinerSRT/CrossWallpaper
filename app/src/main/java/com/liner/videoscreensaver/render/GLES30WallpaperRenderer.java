package com.liner.videoscreensaver.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.liner.videoscreensaver.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GLES30WallpaperRenderer extends GLWallpaperRenderer {
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_INT = 4;
    private final FloatBuffer vertices;
    private final FloatBuffer texCoords;
    private final IntBuffer indices;
    private final int[] buffers;
    private final int[] vertexArrays;
    private final int[] textures;
    private final float[] mvp;
    private int program = 0;
    private int mvpLocation = 0;
    private SurfaceTexture surfaceTexture = null;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int videoRotation = 0;
    private float xOffset = 0;
    private float yOffset = 0;
    private float maxXOffset = 0;
    private float maxYOffset = 0;
    private long updatedFrame = 0;
    private long renderedFrame = 0;

    GLES30WallpaperRenderer(@NonNull final Context context) {
        super(context);
        final float[] vertexArray = {
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };
        vertices = ByteBuffer.allocateDirect(vertexArray.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertices.put(vertexArray).position(0);
        final float[] texCoordArray = {
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };
        texCoords = ByteBuffer.allocateDirect(texCoordArray.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoords.put(texCoordArray).position(0);
        final int[] indexArray = {
                0, 1, 2,
                3, 2, 1
        };
        indices = ByteBuffer.allocateDirect(indexArray.length * BYTES_PER_INT).order(ByteOrder.nativeOrder()).asIntBuffer();
        indices.put(indexArray).position(0);
        vertexArrays = new int[1];
        buffers = new int[3];
        textures = new int[1];
        mvp = new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        };
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthMask(false);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glGenTextures(textures.length, textures, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        program = GLUtils.linkProgramGLES30(
                GLUtils.compileShaderResourceGLES30(
                        context, GLES30.GL_VERTEX_SHADER, R.raw.vertex_30
                ),
                GLUtils.compileShaderResourceGLES30(
                        context, GLES30.GL_FRAGMENT_SHADER, R.raw.fragment_30
                )
        );
        mvpLocation = GLES30.glGetUniformLocation(program, "mvp");
        GLES30.glGenBuffers(buffers.length, buffers, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.capacity() * BYTES_PER_FLOAT, vertices, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, texCoords.capacity() * BYTES_PER_FLOAT, texCoords, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * BYTES_PER_INT, indices, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES30.glGenVertexArrays(vertexArrays.length, vertexArrays, 0);
        GLES30.glBindVertexArray(vertexArrays[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0]);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
        GLES30.glBindVertexArray(0);
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (surfaceTexture == null) {
            return;
        }
        if (renderedFrame < updatedFrame) {
            surfaceTexture.updateTexImage();
        }
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0);
        GLES30.glBindVertexArray(vertexArrays[0]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_INT, 0);
        GLES30.glBindVertexArray(0);
        GLES30.glUseProgram(0);
    }


    @Override
    void setSourcePlayer(@NonNull final SimpleExoPlayer exoPlayer) {
        createSurfaceTexture();
        exoPlayer.setVideoSurface(new Surface(surfaceTexture));
    }

    @Override
    void setScreenSize(int width, int height) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width;
            screenHeight = height;
            maxXOffset = (1.0f - ((float) screenWidth / screenHeight) / ((float) videoWidth / videoHeight)) / 2;
            maxYOffset = (1.0f - ((float) screenHeight / screenWidth) / ((float) videoHeight / videoWidth)) / 2;
            updateMatrix(com.liner.videoscreensaver.Core.fitCenter);
        }
    }

    @Override
    void setVideoSizeAndRotation(int width, int height, int rotation) {
        if (rotation % 180 != 0) {
            final int swap = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = swap;
        }
        if (videoWidth != width || videoHeight != height || videoRotation != rotation) {
            videoWidth = width;
            videoHeight = height;
            videoRotation = rotation;
            maxXOffset = (1.0f - ((float) screenWidth / screenHeight) / ((float) videoWidth / videoHeight)) / 2;
            maxYOffset = (1.0f - ((float) screenHeight / screenWidth) / ((float) videoHeight / videoWidth)) / 2;
            updateMatrix(com.liner.videoscreensaver.Core.fitCenter);
        }
    }

    @Override
    void setOffset(float xOffset, float yOffset) {
        if (xOffset > maxXOffset)
            xOffset = maxXOffset;
        if (xOffset < -maxXOffset)
            xOffset = -maxXOffset;
        if (yOffset > maxYOffset)
            yOffset = maxYOffset;
        if (yOffset < -maxXOffset)
            yOffset = -maxYOffset;
        if (this.xOffset != xOffset || this.yOffset != yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            updateMatrix(true);
        }
    }

    private void createSurfaceTexture() {
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        updatedFrame = 0;
        renderedFrame = 0;
        surfaceTexture = new SurfaceTexture(textures[0]);
        surfaceTexture.setDefaultBufferSize(videoWidth, videoHeight);
        surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> ++updatedFrame);
    }

    private void updateMatrix(boolean fitScreen) {
        for (int i = 0; i < 16; ++i) {
            mvp[i] = 0.0f;
        }
        mvp[0] = mvp[5] = mvp[10] = mvp[15] = 1.0f;
        if (!fitScreen) {
            Matrix.scaleM(
                    mvp,
                    0,
                    ((float) videoWidth / videoHeight) / ((float) screenWidth / screenHeight),
                    ((float) videoHeight / videoWidth) / ((float) screenHeight / screenWidth), 1
            );
            if (videoRotation % 360 != 0)
                Matrix.rotateM(mvp, 0, -videoRotation, 0, 0, 1);
            Matrix.translateM(mvp, 0, xOffset, yOffset, 0);
            return;
        }
        final float videoRatio = (float) videoWidth / videoHeight;
        final float screenRatio = (float) screenWidth / screenHeight;
        if (videoRatio >= screenRatio) {
            Matrix.scaleM(
                    mvp, 0,
                    ((float) videoWidth / videoHeight) / ((float) screenWidth / screenHeight),
                    1, 1
            );
            if (videoRotation % 360 != 0)
                Matrix.rotateM(mvp, 0, -videoRotation, 0, 0, 1);
            Matrix.translateM(mvp, 0, xOffset, 0, 0);
        } else {
            Matrix.scaleM(
                    mvp, 0, 1,
                    ((float) videoHeight / videoWidth) / ((float) screenHeight / screenWidth), 1
            );
            if (videoRotation % 360 != 0) {
                Matrix.rotateM(mvp, 0, -videoRotation, 0, 0, 1);
            }
            Matrix.translateM(mvp, 0, 0, yOffset, 0);
        }
    }
}
