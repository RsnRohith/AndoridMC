//package com.example.rohit.videodecodeandencodemultiple;
//
//
///**
// * Created by rohit on 06/12/17.
// */
//
//
//import android.hardware.Camera;
//import android.media.MediaCodec;
//import android.media.MediaCodecInfo;
//import android.media.MediaFormat;
//import android.media.MediaMuxer;
//import android.opengl.EGL14;
//import android.opengl.EGLConfig;
//import android.opengl.EGLContext;
//import android.opengl.EGLDisplay;
//import android.opengl.EGLExt;
//import android.opengl.EGLSurface;
//import android.opengl.GLES20;
//import android.opengl.Matrix;
//import android.os.Build;
//import android.support.annotation.RequiresApi;
//import android.util.Log;
//import android.view.Surface;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.nio.ShortBuffer;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//
//
//
//import android.hardware.Camera;
//import android.media.MediaCodec;
//import android.media.MediaCodecInfo;
//import android.media.MediaFormat;
//import android.media.MediaMuxer;
//import android.opengl.EGL14;
//import android.opengl.EGLConfig;
//import android.opengl.EGLContext;
//import android.opengl.EGLDisplay;
//import android.opengl.EGLExt;
//import android.opengl.EGLSurface;
//import android.opengl.GLES20;
//import android.opengl.Matrix;
//import android.os.Build;
//import android.support.annotation.RequiresApi;
//import android.view.Surface;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.nio.ShortBuffer;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//
//
//@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//
//public class VideoEncoder implements Runnable {
//    private static final String TAG = "VideoEncoder";
//
//    private MediaCodec mEncoder;
//    private CodecInputSurface mInputSurface;
//    private MediaMuxer mMuxer;
//    private int mTrackIndex;
//    private boolean mMuxerStarted;
//
//    private int mFrameWidth = 0;
//    private int mFrameHeight = 0;
//    private int mSurfaceWidth = 0;
//    private int mSurfaceHeight = 0;
//    private String mFileName = null;
//
//    private Filter mFilter = null;
//    private MediaCodec.BufferInfo mBufferInfo;
//
//    private int mFrameIndex = 0;
//    private boolean IsVideoInterrupted = false;
//
//    private int FRAME_RATE = 25;
//    private static final String MIME_TYPE = "video/avc";
//    private static final int IFRAME_INTERVAL = 5;
//    private boolean isCameraPreviewStopped = false;
//    private HashMap<Integer,Long> mTimeMapper = new HashMap<>();
//
//    private int mFinalEncoderWidth = 0;
//    private int mFinalEncoderHeight = 0;
//
//    private int mCameraFacing;
//    private ArrayList<byte[]> mFrames = new ArrayList<>();
////    private CameraEngineInterface.BoomerangProcessingDone mFiltercallback = null;
////    private ModularCaptureCallback.ModularCaptureBoomerangStartCallback mCallbackStart = null;
////    private ModularCaptureCallback.ModularCaptureBoomerangStopCallback mCallbackStop = null;
//
//
//    VideoEncoder(int preview_width, int preview_height, int surface_width, int surface_height, String fileName,int camerafacing){
//        mFrameWidth = preview_width;
//        mFrameHeight = preview_height;
//        mSurfaceWidth = surface_width;
//        mSurfaceHeight = surface_height;
//        mFileName = fileName;
//        mFrameIndex = 0;
////        mCallbackStart = startCallback;
////        mCallbackStop = stopCallback;
////        mFiltercallback = filterCallback;
//        mCameraFacing = camerafacing;
//    }
//
//
//    private int calcBitRate() {
//        return (int) (0.15 * FRAME_RATE * mFrameWidth * mFrameHeight);
//    }
//
//    @Override
//    public void run() {
//        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//        startBoomerangEncoding();
//    }
//
//    public void setInterrupted(boolean interrupted) {
//        isCameraPreviewStopped = true;
//        IsVideoInterrupted = IsVideoInterrupted || interrupted;
//    }
//
//    private int getCameraFacing(){
//        return mCameraFacing;
//    }
//
//    private void startBoomerangEncoding() {
//        try {
//            Log.d("TAGGERRRR", " 1");
//            prepareEncoder();
//            Log.d("TAGGERRRR", " 4");
//
//            mFrameIndex = 0;
//            int TOTAL_STEPS = 1; //how many times to repeat the video
//            int current_step = 0;
//            boolean forward = true;
//            long presentationTimeStamp = 0;
//
//            while (!IsVideoInterrupted) {
//                Log.d("TAGGERRRR", " 5");
//                drainEncoder(false);
//                Log.d("TAGGERRRR", " 6");
//                //checkFrameCount();
//                Log.d("TAGGERRRR", " 7");
//                if (mFrameIndex > -1 && mFrameIndex < mFrames.size()) {
//                    Log.d("TAGGERRRR", " 8");
//                    mFilter.onDraw(mFrames.get(mFrameIndex));
//                    Log.d("TAGGERRRR", " 9");
//                    presentationTimeStamp = getCurrentPTS(forward,presentationTimeStamp);
//                    mInputSurface.setPresentationTime(presentationTimeStamp);
//                    Log.d("TAGGERRRR", " 10");
//                    //presentationTimeStamp = presentationTimeStamp + DELTA_TIME;
//                    mInputSurface.swapBuffers();
//                    Log.d("TAGGERRRR", " 11");
//                    if (forward) {
//                        mFrameIndex++;
//                    } else {
//                        mFrameIndex--;
//                    }
//                }
//                if (isCameraPreviewStopped) {
//                    Log.d("TAGGERRRR", " 12");
//                    if (forward && mFrameIndex >= (mFrames.size())) {
//                        once_done = true;
//                        forward = false;
//                        mFrameIndex = mFrames.size() - 2;
//                    } else if (!forward && (mFrameIndex < 0)) {
//                        forward = true;
//                        current_step++;
//                        mFrameIndex = 1;
//                        if (current_step == TOTAL_STEPS) {
//                            break;
//                        }
//                    }
//                }
//            }
//            Log.d("TAGGERRRR", " 13");
//            if (mEncoder != null && mMuxer != null) {
//                drainEncoder(true);
//                Log.d("TAGGERRRR", " 15");
//            }
//            Log.d("TAGGERRRR", " 14");
//            releaseEncoder();
//            //callBackMethods((IsVideoInterrupted) ? 0 : 1);
//        } catch (Exception e) {
//            e.printStackTrace();
//            //callBackMethods(0);
//        }
//    }
//
//    private boolean once_done = false;
//
//    private long getCurrentPTS(boolean forward, long presentationTime) {
//        if (once_done) {
//            if (forward) {
//                presentationTime = presentationTime + Math.abs(mTimeMapper.get(mFrameIndex) - mTimeMapper.get(mFrameIndex - 1));
//            } else {
//                presentationTime = presentationTime + Math.abs(mTimeMapper.get(mFrameIndex) - mTimeMapper.get(mFrameIndex + 1));
//            }
//            return presentationTime;
//        }
//        return mTimeMapper.get(mFrameIndex);
//    }
//
//
//
//
//    private void prepareEncoder() {
//        mBufferInfo = new MediaCodec.BufferInfo();
//        mFinalEncoderWidth = outputSize.getKey();
//        mFinalEncoderHeight = outputSize.getValue();
//
//        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, outputSize.getKey(), outputSize.getValue());
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, getFrameRate());
//        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
//        try {
//            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
//            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//            Log.d("TAGGERRRR", " 2");
//            mInputSurface = new CodecInputSurface(mEncoder.createInputSurface());
//            mEncoder.start();
//            mMuxer = new MediaMuxer(mFileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//            Log.d("TAGGERRRR", " 3");
//            mInputSurface.makeCurrent();
//            mFilter = new Filter();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mTrackIndex = -1;
//        mMuxerStarted = false;
//    }
//
//    private int getFrameRate() {
//        if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            FRAME_RATE = 20;
//        } else if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            FRAME_RATE = 25;
//        }
//        return FRAME_RATE;
//    }
//
//
//
//    private void releaseEncoder() {
//        if (mInputSurface != null) {
//            mInputSurface.release();
//            mInputSurface = null;
//        }
//        if (mEncoder != null) {
//            mEncoder.stop();
//            mEncoder.release();
//            mEncoder = null;
//        }
//        if (mMuxer != null) {
//            mMuxer.stop();
//            mMuxer.release();
//            mMuxer = null;
//        }
//        if (mFilter != null) {
//            mFilter.destroy();
//            mFilter = null;
//        }
//        mFrames = null;
//    }
//
//    private void drainEncoder(boolean endOfStream) {
//        final int TIMEOUT_USEC = 10000;
//
//        if (endOfStream) {
//            mEncoder.signalEndOfInputStream();
//        }
//
//        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
//        while (true) {
//            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
//            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                if (!endOfStream) {
//                    break;      // out of while
//                } else {
//                    Log.d(TAG, "no output available, spinning to await EOS");
//                }
//            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                encoderOutputBuffers = mEncoder.getOutputBuffers();
//            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                if (mMuxerStarted) {
//                    throw new RuntimeException("format changed twice");
//                }
//                MediaFormat newFormat = mEncoder.getOutputFormat();
//                mTrackIndex = mMuxer.addTrack(newFormat);
//                mMuxer.start();
//                mMuxerStarted = true;
//            } else if (encoderStatus < 0) {
//                Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
//            } else {
//                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
//                if (encodedData == null) {
//                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
//                }
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
//                    mBufferInfo.size = 0;
//                }
//
//                if (mBufferInfo.size != 0) {
//                    if (!mMuxerStarted) {
//                        throw new RuntimeException("muxer hasn't started");
//                    }
//                    encodedData.position(mBufferInfo.offset);
//                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
//                }
//
//                mEncoder.releaseOutputBuffer(encoderStatus, false);
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    if (!endOfStream) {
//                        Log.d(TAG, "reached end of stream unexpectedly");
//                    } else {
//                        Log.d(TAG, "end of stream reached");
//                    }
//                    break;
//                }
//            }
//        }
//    }
//
//    private int total_frame_count = 0;
//
//    public void onPreviewFrame(byte[] bytes) {
//        int FRAME_SKIPPER = 2;
//        if(!isCameraPreviewStopped)
//            mTimeMapper.put(total_frame_count,System.nanoTime());
//        if (!isCameraPreviewStopped && (total_frame_count % FRAME_SKIPPER) == 0) { // skipping 2 in 1 frames
//            byte[] new_frame = new byte[bytes.length];
//            System.arraycopy(bytes, 0, new_frame, 0, bytes.length);
//            mFrames.add(new_frame);
//            //mTimeMapper.put(total_frame_count, (total_frame_count == 0) ? 0 : getSystemNanoTime());
//        }
//        total_frame_count++;
//    }
//
//    void stopBoomerang() {
//        isCameraPreviewStopped = true;
//    }
//
//    private class CodecInputSurface {
//        private static final int EGL_RECORDABLE_ANDROID = 0x3142;
//
//        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
//        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
//        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
//
//        private Surface mSurface;
//
//        CodecInputSurface(Surface surface) {
//            if (surface == null) {
//                throw new NullPointerException();
//            }
//            mSurface = surface;
//            eglSetup();
//        }
//
//        private void eglSetup() {
//            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
//            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
//                throw new RuntimeException("unable to get EGL14 display");
//            }
//            int[] version = new int[2];
//            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
//                throw new RuntimeException("unable to initialize EGL14");
//            }
//            int[] attribList = {
//                    EGL14.EGL_RED_SIZE, 8,
//                    EGL14.EGL_GREEN_SIZE, 8,
//                    EGL14.EGL_BLUE_SIZE, 8,
//                    EGL14.EGL_ALPHA_SIZE, 8,
//                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
//                    EGL_RECORDABLE_ANDROID, 1,
//                    EGL14.EGL_NONE
//            };
//            EGLConfig[] configs = new EGLConfig[1];
//            int[] numConfigs = new int[1];
//            EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
//                    numConfigs, 0);
//            checkEglError("eglCreateContext RGB888+recordable ES2");
//            int[] attrib_list = {
//                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
//                    EGL14.EGL_NONE
//            };
//            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
//                    attrib_list, 0);
//            checkEglError("eglCreateContext");
//            int[] surfaceAttribs = {
//                    EGL14.EGL_NONE
//            };
//            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
//                    surfaceAttribs, 0);
//            checkEglError("eglCreateWindowSurface");
//        }
//
//        public void release() {
//            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
//                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
//                        EGL14.EGL_NO_CONTEXT);
//                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
//                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
//                EGL14.eglReleaseThread();
//                EGL14.eglTerminate(mEGLDisplay);
//            }
//            mSurface.release();
//
//            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
//            mEGLContext = EGL14.EGL_NO_CONTEXT;
//            mEGLSurface = EGL14.EGL_NO_SURFACE;
//
//            mSurface = null;
//        }
//
//        public void makeCurrent() {
//            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
//            checkEglError("eglMakeCurrent");
//        }
//
//        void swapBuffers() {
//            EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
//            checkEglError("eglSwapBuffers");
//        }
//
//        void setPresentationTime(long nsecs) {
//            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
//            checkEglError("eglPresentationTimeANDROID");
//        }
//
//        private void checkEglError(String msg) {
//            int error;
//            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
//                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
//            }
//        }
//    }
//
//
//    public class Filter {
//
//        float[] mVerticesFrontCamera = {
//                -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
//                -1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
//                1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
//                1.0f, 1.0f, 0.0f, 1.0f, 0.0f};
//
//        float[] mVerticesBackCamera = {
//                -1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
//                1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
//                -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
//                1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
//
//        static final String YUV_VS = "" +
//                "attribute vec4 a_position;\n" +
//                "attribute vec2 a_texcoord;\n" +
//                "varying vec2 v_texcoord;\n" +
//                "void main() {\n" +
//                "   gl_Position =  a_position;\n" +
//                "   v_texcoord = a_texcoord;\n" +
//                "}";
//
//        static final String YUV_FS = "" +
//                "precision highp float;\n" +
//                "varying highp vec2 v_texcoord;\n" +
//                "uniform sampler2D luminanceTexture;" +
//                "uniform sampler2D chrominanceTexture;" +
//                "void main() {\n" +
//                "   lowp float y = texture2D(luminanceTexture, v_texcoord).r;" +
//                "   lowp vec4 uv = texture2D(chrominanceTexture, v_texcoord);" +
//                "   mediump vec4 rgba = y * vec4(1.0, 1.0, 1.0, 1.0) + " +
//                "                  (uv.a - 0.5) * vec4(0.0, -0.337633, 1.732446, 0.0) + " +
//                "                  (uv.r - 0.5) * vec4(1.370705, -0.698001, 0.0, 0.0); " +
//                "	gl_FragColor = rgba;" +
//                "}";
//
//        protected int mGLProgId;
//
//        protected final int SHORT_SIZE_BYTES = 2;
//        protected final int FLOAT_SIZE_BYTES = 4;
//
//        final static String A_POSITION = "a_position";
//        final static String A_TEXCOORD = "a_texcoord";
//        final static String U_LUMINANCE_SAMPLER = "luminanceTexture";
//        final static String U_CHROMINANCE_SAMPLER = "chrominanceTexture";
//
//
//        protected int maPositionHandle;
//        protected int maTextureHandle;
//        int mLumninanceSampler;
//        int mChrominanceSampler;
//
//
//        protected int mVertexBufferObjectId;
//
//        int mElementBufferObjectId;
//
//        int mcameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
//
//        private float[] mProjMatrix = new float[16];
//
//        ByteBuffer mBufferY;
//        ByteBuffer mBufferUV;
//
//        short[] mIndices = {0, 1, 2, 1, 2, 3};
//
//        private FloatBuffer mVertexBuffer = null;
//
//        private ShortBuffer mIndexBuffer = null;
//
//        private String mVertexShader;
//        private String mFragmentShader;
//
//        int[] mSamplers = null;
//
//        private final PreviewFrameTexture mPreviewFrameTexture = new PreviewFrameTexture();
//
//        boolean mIsInitialized;
//
//        public Filter() {
//            this(YUV_VS, YUV_FS);
//            mIsInitialized = false;
//            init();
//        }
//
//        public Filter(String vertexShader, String fragmentShader) {
//            mVertexShader = vertexShader;
//            mFragmentShader = fragmentShader;
//        }
//
//        private void init() {
//
//            mBufferY = ByteBuffer.allocateDirect(mFrameWidth * mFrameHeight);
//            //  UV channel. Used for color rendering
//            mBufferUV = ByteBuffer.allocateDirect(mFrameWidth * mFrameHeight / 2);
//
//            mPreviewFrameTexture.init();
//
//            mPreviewFrameTexture.initWithPreview(mFrameWidth, mFrameHeight);
//
//            mcameraFacing = getCameraFacing();
//
//            onInit();
//
//            onInitialized();
//        }
//
//        protected boolean isInitialized() {
//            return mIsInitialized;
//        }
//
//        private void onInit() {
//            //super.onInit();
//            mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
//
//            Matrix.orthoM(mProjMatrix, 0, -1, 1, 1, -1, 1, -1);
//            maPositionHandle = GLES20.glGetAttribLocation(mGLProgId, A_POSITION);
//            maTextureHandle = GLES20.glGetAttribLocation(mGLProgId, A_TEXCOORD);
//            //muMVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgId, U_MVPMATRIX);
//            //This call will go inside respective filter type. TO FIX
//
//            mLumninanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_LUMINANCE_SAMPLER);
//            mChrominanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_CHROMINANCE_SAMPLER);
//            mSamplers = new int[2];
//            mSamplers[0] = mLumninanceSampler;
//            mSamplers[1] = mChrominanceSampler;
//
//            int[] vboIds = new int[2];
//            GLES20.glGenBuffers(2, vboIds, 0);
//            mVertexBufferObjectId = vboIds[0];
//            mElementBufferObjectId = vboIds[1];
//
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
//            float[] modifiedBuffer = modifyTextureBuffers();
//            mVertexBuffer = ByteBuffer.allocateDirect(modifiedBuffer.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
//            mVertexBuffer.put(modifiedBuffer).position(0);
//            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * FLOAT_SIZE_BYTES, mVertexBuffer, GLES20.GL_STATIC_DRAW);
//
//            mIndexBuffer = ByteBuffer.allocateDirect(mIndices.length * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
//            mIndexBuffer.put(mIndices).position(0);
//            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
//            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer.capacity() * SHORT_SIZE_BYTES, mIndexBuffer, GLES20.GL_STATIC_DRAW);
//        }
//
//        public void onInitialized() {
//            mIsInitialized = true;
//        }
//
//        private void processVideoFrame(byte[] frame) {
//            mBufferY.position(0);
//            mBufferY.put(frame, 0, mFrameWidth * mFrameHeight);
//            mBufferUV.position(0);
//
//            mBufferUV.put(frame,
//                    mFrameWidth * mFrameHeight,
//                    mFrameWidth * mFrameHeight / 2);
//
//            mBufferY.position(0);
//            mBufferUV.position(0);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getYTextureHandle());
//
//            GLES20.glTexImage2D(
//                    GLES20.GL_TEXTURE_2D,
//                    0,
//                    GLES20.GL_LUMINANCE,
//                    mFrameWidth,
//                    mFrameHeight,
//                    0,
//                    GLES20.GL_LUMINANCE,
//                    GLES20.GL_UNSIGNED_BYTE,
//                    mBufferY
//            );
//
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getUVTextureHandle());
//
//            GLES20.glTexImage2D(
//                    GLES20.GL_TEXTURE_2D,
//                    0,
//                    GLES20.GL_LUMINANCE_ALPHA,
//                    mFrameWidth / 2,
//                    mFrameHeight / 2,
//                    0,
//                    GLES20.GL_LUMINANCE_ALPHA,
//                    GLES20.GL_UNSIGNED_BYTE,
//                    mBufferUV
//            );
//
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
//        }
//
//
//        public void onDraw(byte[] frame) {
//            GLES20.glUseProgram(mGLProgId);
//            GLES20.glViewport(0, 0, mFinalEncoderWidth, mFinalEncoderHeight);
//            if (!mIsInitialized) {
//                return;
//            }
//
//            processVideoFrame(frame);
//
//            for (int i = 0; i < mPreviewFrameTexture.getTextureHandle().length; i++) {
//
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getTextureHandle()[i]);
//                GLES20.glUniform1i(mSamplers[i], i);
//            }
//
//            GLES20.glEnableVertexAttribArray(maPositionHandle);
//            GLES20.glEnableVertexAttribArray(maTextureHandle);
//
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
//            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
//            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, true, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);
//
//            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
//            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * SHORT_SIZE_BYTES, GLES20.GL_UNSIGNED_SHORT, 0);
//
//            GLES20.glDisableVertexAttribArray(maTextureHandle);
//            GLES20.glDisableVertexAttribArray(maPositionHandle);
//
//            GLES20.glFinish();
//            GLES20.glUseProgram(0);
//        }
//
//        float[] modifyTextureBuffers() {
//            //Algorithm used : Explaination:
//		/*
//			https://stackoverflow.com/questions/6565703/math-algorithm-fit-image-to-screen-retain-aspect-ratio
//			So for example:
//
//			20
//			|------------------|
//			10
//			|---------|
//
//			--------------------     ---   ---
//			|         |        |      | 7   |
//			|         |        |      |     | 10
//			|----------        |     ---    |
//			|                  |            |
//		--------------------           ---
//
//				ws = 20
//				hs = 10
//				wi = 10
//				hi = 7
//				20/10 > 10/7 ==> (wi * hs/hi, hs) = (10 * 10/7, 10) = (100/7, 10) ~ (14.3, 10)
//
//		*/
//            //float surfaceAspectRatio = (1.0f*mSurfaceHeight)/mSurfaceWidth;
//            //float previewAspectRatio = (1.0f*mPreviewWidth)/mPreviewHeight; //Camera preview width and height are swapped.
//            float surfaceW = mSurfaceHeight;
//            float surfaceH = mSurfaceWidth;
//
//            float previewW = mFrameWidth;
//            float previewH = mFrameHeight;
//
//            float surfaceAspectRatio = surfaceW / surfaceH;
//            float previewAspectRatio = previewW / previewH;
//
//            float[] modifiedBuffer;
//            if (mcameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                modifiedBuffer = mVerticesFrontCamera.clone();
//            } else {
//                modifiedBuffer = mVerticesBackCamera.clone();
//            }
//            if (surfaceAspectRatio > previewAspectRatio) {
//                float newWidth;
//                float newHeight;
//                newHeight = previewH * surfaceW / previewW;
//                //Coordinates for surface width and height.
//                float yFactor = (newHeight - surfaceH) / 2;
//                //float xFactor = (newWidth - surfaceW)/2;
//                //float newAspectRatio = newWidth/newHeight;
//                float offset = yFactor / newHeight;
//                if(mcameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                    modifiedBuffer[4] = modifiedBuffer[4] - offset;
//                    modifiedBuffer[9] = modifiedBuffer[9] + offset;
//                    modifiedBuffer[14] = modifiedBuffer[14] - offset;
//                    modifiedBuffer[19] = modifiedBuffer[19] + offset;
//                }
//                else if(mcameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                    modifiedBuffer[4] = modifiedBuffer[4] - offset;
//                    modifiedBuffer[9] = modifiedBuffer[9] - offset;
//                    modifiedBuffer[14] = modifiedBuffer[14] + offset;
//                    modifiedBuffer[19] = modifiedBuffer[19] + offset;
//                }
//            }
//            return modifiedBuffer;
//        }
//
//        public void destroy() {
//            mIsInitialized = false;
//            GLES20.glDeleteProgram(mGLProgId);
//        }
//    }
//}
//
