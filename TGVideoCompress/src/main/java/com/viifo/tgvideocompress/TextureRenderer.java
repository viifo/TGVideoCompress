package com.viifo.tgvideocompress;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class TextureRenderer {

    private FloatBuffer verticesBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer renderTextureBuffer;
    private FloatBuffer maskTextureBuffer;
    private FloatBuffer bitmapVerticesBuffer;

    float[] bitmapData = {
            -1.0f, 1.0f,
            1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
    };
    private int transformedWidth;
    private int transformedHeight;

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private static final String VERTEX_SHADER_300 =
            "#version 320 es\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "in vec4 aPosition;\n" +
                    "in vec4 aTextureCoord;\n" +
                    "out vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private static final String VERTEX_SHADER_MASK =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "attribute vec4 mTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec2 MTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "  MTextureCoord = (uSTMatrix * mTextureCoord).xy;\n" +
                    "}\n";

    private static final String VERTEX_SHADER_MASK_300 =
            "#version 320 es\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "in vec4 aPosition;\n" +
                    "in vec4 aTextureCoord;\n" +
                    "in vec4 mTextureCoord;\n" +
                    "out vec2 vTextureCoord;\n" +
                    "out vec2 MTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "  MTextureCoord = (uSTMatrix * mTextureCoord).xy;\n" +
                    "}\n";

    private static final String FRAGMENT_EXTERNAL_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);" +
                    "}\n";

    private static final String FRAGMENT_EXTERNAL_MASK_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec2 MTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform sampler2D sMask;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord) * texture2D(sMask, MTextureCoord).a;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private static final String FRAGMENT_MASK_SHADER =
            "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec2 MTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "uniform sampler2D sMask;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord) * texture2D(sMask, MTextureCoord).a;\n" +
                    "}\n";

    private static final String GRADIENT_FRAGMENT_SHADER =
            "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform vec4 gradientTopColor;\n" +
                    "uniform vec4 gradientBottomColor;\n" +
                    "float interleavedGradientNoise(vec2 n) {\n" +
                    "    return fract(52.9829189 * fract(.06711056 * n.x + .00583715 * n.y));\n" +
                    "}\n" +
                    "void main() {\n" +
                    "  gl_FragColor = mix(gradientTopColor, gradientBottomColor, vTextureCoord.y + (.2 * interleavedGradientNoise(gl_FragCoord.xy) - .1));\n" +
                    "}\n";

    private int NUM_FILTER_SHADER = -1;
    private int NUM_EXTERNAL_SHADER = -1;
    private int NUM_GRADIENT_SHADER = -1;

    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private float[] mSTMatrixIdentity = new float[16];
    private int mTextureID;
    private int[] mProgram;
    private int[] muMVPMatrixHandle;
    private int[] muSTMatrixHandle;
    private int[] maPositionHandle;
    private int[] maTextureHandle;
    private int[] mmTextureHandle;
    private int[] maskTextureHandle;
    private int texSizeHandle;

    private int simplePositionHandle;
    private int simpleInputTexCoordHandle;

    private boolean blendEnabled;
    private int gradientTopColor, gradientBottomColor;

    public TextureRenderer(int w, int h, int rotation) {
        float[] texData = {
                0.f, 0.f,
                1.f, 0.f,
                0.f, 1.f,
                1.f, 1.f,
        };

        textureBuffer = ByteBuffer.allocateDirect(texData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(texData).position(0);

        bitmapVerticesBuffer = ByteBuffer.allocateDirect(bitmapData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        bitmapVerticesBuffer.put(bitmapData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mSTMatrixIdentity, 0);

        transformedWidth = w;
        transformedHeight = h;

        int count = 0;
        NUM_EXTERNAL_SHADER = count++;

        Matrix.setIdentityM(mMVPMatrix, 0);

        mProgram = new int[count];
        muMVPMatrixHandle = new int[count];
        muSTMatrixHandle = new int[count];
        maPositionHandle = new int[count];
        maTextureHandle = new int[count];
        mmTextureHandle = new int[count];
        maskTextureHandle = new int[count];

        float[] verticesData = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };
        verticesBuffer = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(verticesData).position(0);

        int textureRotation = rotation;
        float[] textureData;
        if (textureRotation == 90) {
            textureData = new float[]{
                    1.f, 0.f,
                    1.f, 1.f,
                    0.f, 0.f,
                    0.f, 1.f
            };
        } else if (textureRotation == 180) {
            textureData = new float[]{
                    1.f, 1.f,
                    0.f, 1.f,
                    1.f, 0.f,
                    0.f, 0.f
            };
        } else if (textureRotation == 270) {
            textureData = new float[]{
                    0.f, 1.f,
                    0.f, 0.f,
                    1.f, 1.f,
                    1.f, 0.f
            };
        } else {
            textureData = new float[]{
                    0.f, 0.f,
                    1.f, 0.f,
                    0.f, 1.f,
                    1.f, 1.f
            };
        }

        renderTextureBuffer = ByteBuffer.allocateDirect(textureData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        renderTextureBuffer.put(textureData).position(0);

        textureData = new float[]{
                0.f, 0.f,
                1.f, 0.f,
                0.f, 1.f,
                1.f, 1.f
        };
        maskTextureBuffer = ByteBuffer.allocateDirect(textureData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        maskTextureBuffer.put(textureData).position(0);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st/*, long time*/) {
        int texture;
        int target;
        int index;
        float[] stMatrix;

        st.getTransformMatrix(mSTMatrix);
        texture = mTextureID;
        index = NUM_EXTERNAL_SHADER;
        target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        stMatrix = mSTMatrix;

        GLES20.glUseProgram(mProgram[index]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(target, texture);

        GLES20.glVertexAttribPointer(maPositionHandle[index], 2, GLES20.GL_FLOAT, false, 8, verticesBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle[index]);
        GLES20.glVertexAttribPointer(maTextureHandle[index], 2, GLES20.GL_FLOAT, false, 8, renderTextureBuffer);
        GLES20.glEnableVertexAttribArray(maTextureHandle[index]);

        if (texSizeHandle != 0) {
            GLES20.glUniform2f(texSizeHandle, transformedWidth, transformedHeight);
        }

        GLES20.glUniformMatrix4fv(muSTMatrixHandle[index], 1, false, stMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle[index], 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glFinish();
    }

    private void drawTexture(boolean bind, int texture) {
        drawTexture(bind, texture, -10000, -10000, -10000, -10000, 0, false);
    }

    private void drawTexture(boolean bind, int texture, float x, float y, float w, float h, float rotation, boolean mirror) {
        drawTexture(bind, texture, x, y, w, h, rotation, mirror, false, -1);
    }

    private void drawTexture(boolean bind, int texture, float x, float y, float w, float h, float rotation, boolean mirror, boolean useCropMatrix, int matrixIndex) {
        if (!blendEnabled) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            blendEnabled = true;
        }
        if (x <= -10000) {
            bitmapData[0] = -1.0f;
            bitmapData[1] = 1.0f;

            bitmapData[2] = 1.0f;
            bitmapData[3] = 1.0f;

            bitmapData[4] = -1.0f;
            bitmapData[5] = -1.0f;

            bitmapData[6] = 1.0f;
            bitmapData[7] = -1.0f;
        } else {
            x = x * 2 - 1.0f;
            y = (1.0f - y) * 2 - 1.0f;
            w = w * 2;
            h = h * 2;

            bitmapData[0] = x;
            bitmapData[1] = y;

            bitmapData[2] = x + w;
            bitmapData[3] = y;

            bitmapData[4] = x;
            bitmapData[5] = y - h;

            bitmapData[6] = x + w;
            bitmapData[7] = y - h;
        }
        float mx = (bitmapData[0] + bitmapData[2]) / 2;
        if (mirror) {
            float temp = bitmapData[2];
            bitmapData[2] = bitmapData[0];
            bitmapData[0] = temp;

            temp = bitmapData[6];
            bitmapData[6] = bitmapData[4];
            bitmapData[4] = temp;
        }
        if (rotation != 0) {
            float ratio = transformedWidth / (float) transformedHeight;
            float my = (bitmapData[5] + bitmapData[1]) / 2;
            for (int a = 0; a < 4; a++) {
                float x1 = bitmapData[a * 2    ] - mx;
                float y1 = (bitmapData[a * 2 + 1] - my) / ratio;
                bitmapData[a * 2    ] = (float) (x1 * Math.cos(rotation) - y1 * Math.sin(rotation)) + mx;
                bitmapData[a * 2 + 1] = (float) (x1 * Math.sin(rotation) + y1 * Math.cos(rotation)) * ratio + my;
            }
        }
        bitmapVerticesBuffer.put(bitmapData).position(0);
        GLES20.glVertexAttribPointer(simplePositionHandle, 2, GLES20.GL_FLOAT, false, 8, useCropMatrix ? verticesBuffer : bitmapVerticesBuffer);
        GLES20.glEnableVertexAttribArray(simpleInputTexCoordHandle);
        GLES20.glVertexAttribPointer(simpleInputTexCoordHandle, 2, GLES20.GL_FLOAT, false, 8, useCropMatrix ? renderTextureBuffer : textureBuffer);
        if (bind) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @SuppressLint("WrongConstant")
    public void surfaceCreated() {
        for (int a = 0; a < mProgram.length; a++) {
            String fragSshader = null;
            String vertexShader = VERTEX_SHADER;
            if (a == NUM_EXTERNAL_SHADER) {
                fragSshader = /*messageVideoMaskPath != null ? FRAGMENT_EXTERNAL_MASK_SHADER : */FRAGMENT_EXTERNAL_SHADER;
                vertexShader = /*messageVideoMaskPath != null ? VERTEX_SHADER_MASK :*/ VERTEX_SHADER;
            } else if (a == NUM_FILTER_SHADER) {
                fragSshader = /*messageVideoMaskPath != null ? FRAGMENT_MASK_SHADER : */FRAGMENT_SHADER;
                vertexShader = /*messageVideoMaskPath != null ? VERTEX_SHADER_MASK : */VERTEX_SHADER;
            } else if (a == NUM_GRADIENT_SHADER) {
                fragSshader = GRADIENT_FRAGMENT_SHADER;
            }
            if (vertexShader == null || fragSshader == null) {
                continue;
            }
            mProgram[a] = createProgram(vertexShader, fragSshader, false);
            maPositionHandle[a] = GLES20.glGetAttribLocation(mProgram[a], "aPosition");
            maTextureHandle[a] = GLES20.glGetAttribLocation(mProgram[a], "aTextureCoord");
            mmTextureHandle[a] = GLES20.glGetAttribLocation(mProgram[a], "mTextureCoord");
            muMVPMatrixHandle[a] = GLES20.glGetUniformLocation(mProgram[a], "uMVPMatrix");
            muSTMatrixHandle[a] = GLES20.glGetUniformLocation(mProgram[a], "uSTMatrix");
            maskTextureHandle[a] = GLES20.glGetUniformLocation(mProgram[a], "sMask");
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private int createProgram(String vertexSource, String fragmentSource, boolean is300) {
        if (is300) {
            int vertexShader = FilterShaders.loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = FilterShaders.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }
            int program = GLES30.glCreateProgram();
            if (program == 0) {
                return 0;
            }
            GLES30.glAttachShader(program, vertexShader);
            GLES30.glAttachShader(program, pixelShader);
            GLES30.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES30.GL_TRUE) {
                GLES30.glDeleteProgram(program);
                program = 0;
            }
            return program;
        } else {
            int vertexShader = FilterShaders.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = FilterShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            if (program == 0) {
                return 0;
            }
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
            return program;
        }
    }

    public void release() {
//        if (mediaEntities != null) {
//            for (int a = 0, N = mediaEntities.size(); a < N; a++) {
//                VideoEditedInfo.MediaEntity entity = mediaEntities.get(a);
//                if (entity.ptr != 0) {
//                    RLottieDrawable.destroy(entity.ptr);
//                }
//                if (entity.bitmap != null) {
//                    entity.bitmap.recycle();
//                    entity.bitmap = null;
//                }
//            }
//        }
    }

    public void changeFragmentShader(String fragmentExternalShader, String fragmentShader, boolean is300) {
        String vertexCode;
//        if (messageVideoMaskPath != null) {
//            vertexCode = is300 ? VERTEX_SHADER_MASK_300 : VERTEX_SHADER_MASK;
//        } else {
//            vertexCode = is300 ? VERTEX_SHADER_300 : VERTEX_SHADER;
//        }
        vertexCode = is300 ? VERTEX_SHADER_300 : VERTEX_SHADER;
        if (NUM_EXTERNAL_SHADER >= 0 && NUM_EXTERNAL_SHADER < mProgram.length) {
            int newProgram = createProgram(vertexCode, fragmentExternalShader, is300);
            if (newProgram != 0) {
                GLES20.glDeleteProgram(mProgram[NUM_EXTERNAL_SHADER]);
                mProgram[NUM_EXTERNAL_SHADER] = newProgram;

                texSizeHandle = GLES20.glGetUniformLocation(newProgram, "texSize");
            }
        }
        if (NUM_FILTER_SHADER >= 0 && NUM_FILTER_SHADER < mProgram.length) {
            int newProgram = createProgram(vertexCode, fragmentShader, is300);
            if (newProgram != 0) {
                GLES20.glDeleteProgram(mProgram[NUM_FILTER_SHADER]);
                mProgram[NUM_FILTER_SHADER] = newProgram;
            }
        }
    }
}
