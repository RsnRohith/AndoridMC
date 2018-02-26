package com.example.rohit.videodecodeandencodemultiple;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import static junit.framework.Assert.assertTrue;

/**
 * Created by rohit on 21/02/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)

public class VideoDecodeAndEncoder {

    private static final boolean VERBOSE = true;
    private String mInputFilename, mOutputFilename;
    private int mNumberOfTimes;

    private ArrayList<byte[]> mFrames = new ArrayList<>();
    private int mFrameIndex = 0;
    private boolean once_done;
    private HashMap<Integer, Long> mTimeMapper = new HashMap<>();


    MediaExtractor videoExtractor = null;
    MediaCodec videoDecoder = null;
    MediaCodec videoEncoder = null;
    MediaMuxer muxer = null;
    CodecInputSurface inputSurface = null;
    Filter mFilter;


    private static final long TIMEOUT_USEC = 10000;

    public static final String TAG = VideoDecodeAndEncoder.class.getSimpleName();


    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private static final int OUTPUT_VIDEO_BIT_RATE = 2000000;
    private static final int OUTPUT_VIDEO_FRAME_RATE = 20;
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 5;
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    private boolean mMuxerStarted;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private int mTrackIndex;
    private int mFrameCount;
    private int mFrameWidth, mFrameHeight;


    public VideoDecodeAndEncoder(String mInputFilename, String mOutputFilename, int mNumberOfTimes) {
        this.mInputFilename = mInputFilename;
        this.mNumberOfTimes = mNumberOfTimes;
        this.mOutputFilename = mOutputFilename;
    }


    public void extractDecodeEditEncodeMux() throws Exception {
        // Exception that may be thrown during release.
        Exception exception = null;
        MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null) {
            // Don't fail CTS if they don't have an AVC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_VIDEO_MIME_TYPE);
            return;
        }
        if (VERBOSE) Log.d(TAG, "video found codec: " + videoCodecInfo.getName());
        try {
            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(mInputFilename);

            int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
            assertTrue("missing video track in test video", videoInputTrack != -1);
            MediaFormat inputFormat = videoExtractor.getTrackFormat(videoInputTrack);

            mFrameHeight = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
            mFrameWidth = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);

            videoDecoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
            videoDecoder.configure(inputFormat, null, null, 0);
            videoDecoder.start();


            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mFrameWidth, mFrameHeight);
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "video format: " + outputVideoFormat);

            videoEncoder = MediaCodec.createByCodecName(videoCodecInfo.getName());
            videoEncoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new CodecInputSurface(videoEncoder.createInputSurface());
            videoEncoder.start();

            inputSurface.makeCurrent();

            muxer = createMuxer();
            mFilter = new Filter();
            startVideoDecoding();
            startVideoEncoding();
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");
            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing inputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing muxer", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (mFilter != null) {
                    mFilter.destroy();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing mFilter", e);
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }


    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(mOutputFilename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }


    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }


    private MediaExtractor createExtractor() throws IOException {
        MediaExtractor extractor;
        extractor = new MediaExtractor();
        extractor.setDataSource(mInputFilename);
        return extractor;
    }

    /**
     * Creates a decoder for the given format, which outputs to the given surface.
     *
     * @param inputFormat the format of the stream to decode
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }

    /**
     * Creates an encoder for the given format using the specified codec, taking input from a
     * surface.
     * <p>
     * <p>The surface to use as input is stored in the given reference.
     *
     * @param codecInfo of the codec to use
     * @param format    of the stream to be produced
     */
    private MediaCodec createVideoEncoder(MediaCodecInfo codecInfo, MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = new CodecInputSurface(encoder.createInputSurface());
        encoder.start();
        return encoder;
    }


    /**
     * Does the actual work for extracting, decoding, encoding and muxing.
     */
    private void startVideoDecoding() {
        ByteBuffer[] videoDecoderInputBuffers;
        ByteBuffer[] videoDecoderOutputBuffers;
        MediaCodec.BufferInfo videoDecoderOutputBufferInfo;
        videoDecoderInputBuffers = videoDecoder.getInputBuffers();
        videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
        videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat decoderOutputVideoFormat;
        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean temp = true;


        while (!videoDecoderDone) {
            while (!videoExtractorDone || temp) {
                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no video decoder input buffer");
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned input buffer: " + decoderInputBufferIndex);
                }

                if (videoExtractorDone) {
                    if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                    videoDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    temp = false;
                    break;
                }
                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = videoExtractor.getSampleTime();

                Log.d(TAG, "video extractor: returned buffer of size " + size);
                Log.d(TAG, "video extractor: returned buffer for time " + presentationTime);
                if (size >= 0) {
                    videoDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            videoExtractor.getSampleFlags());
                }
                videoExtractorDone = !videoExtractor.advance();
                // We extracted a frame, let's try something else next.
                break;
            }

            // Poll output frames from the video decoder and feed the encoder.
            while (!videoDecoderDone) {
                int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no video decoder output buffer");
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "video decoder: output buffers changed");
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: output format changed: "
                                + decoderOutputVideoFormat);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: "
                            + decoderOutputBufferIndex);
                    Log.d(TAG, "video decoder: returned buffer of size "
                            + videoDecoderOutputBufferInfo.size);
                }
                ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: codec config buffer");
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned buffer for time "
                            + videoDecoderOutputBufferInfo.presentationTimeUs);
                }
                if (videoDecoderOutputBufferInfo.size != 0) {
                    byte[] frame = new byte[videoDecoderOutputBufferInfo.size];
                    decoderOutputBuffer.get(frame, 0, videoDecoderOutputBufferInfo.size);
                    videoDecoderOutputBuffers[decoderOutputBufferIndex].position(0);
                    mTimeMapper.put(mFrameCount, videoDecoderOutputBufferInfo.presentationTimeUs);
                    mFrameCount++;
                    mFrames.add(frame);
                }
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: EOS");
                    videoDecoderDone = true;
                }
                break;
            }
        }
        try {
            if (videoExtractor != null) {
                videoExtractor.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "error while releasing videoExtractor", e);
        }
        try {
            if (videoDecoder != null) {
                videoDecoder.stop();
                videoDecoder.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "error while releasing videoDecoder", e);
        }
    }


    private void startVideoEncoding() {
        try {
            mFrameIndex = 0;
            int TOTAL_STEPS = 1; //how many times to repeat the video
            int current_step = 0;
            boolean forward = true;
            long presentationTimeStamp = 0;

            while (true) {
                drainEncoder(false);
                if (mFrameIndex > -1 && mFrameIndex < mFrames.size()) {
                    mFilter.onDraw(mFrames.get(mFrameIndex));
                    presentationTimeStamp = mTimeMapper.get(mFrameIndex);
                    inputSurface.setPresentationTime(presentationTimeStamp);
                    inputSurface.swapBuffers();
                    if (forward) {
                        mFrameIndex++;
                    } else {
                        mFrameIndex--;
                    }
                }
                if (true) {
                    if (forward && mFrameIndex >= (mFrames.size())) {
                        break;
//                        once_done = true;
//                        forward = false;
//                        mFrameIndex = mFrames.size() - 2;

                    } else if (!forward && (mFrameIndex < 0)) {
                        forward = true;
                        current_step++;
                        mFrameIndex = 1;
                        if (current_step == TOTAL_STEPS) {
                            break;
                        }
                    }
                }
            }
            drainEncoder(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getCurrentPTS(boolean forward, long presentationTime) {
//        if (once_done) {
//            if (forward) {
//                presentationTime = presentationTime + Math.abs(mTimeMapper.get(mFrameIndex) - mTimeMapper.get(mFrameIndex - 1));
//            } else {
//                presentationTime = presentationTime + Math.abs(mTimeMapper.get(mFrameIndex) - mTimeMapper.get(mFrameIndex + 1));
//            }
//            return presentationTime;
//        }
        return mTimeMapper.get(mFrameIndex);
    }


    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream) {
            videoEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = videoEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = videoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = videoEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = videoEncoder.getOutputFormat();
                mTrackIndex = muxer.addTrack(newFormat);
                muxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }

                videoEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.d(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");
                    }
                    break;
                }
            }
        }
    }


    public class Filter {

        float[] mVerticesFrontCamera = {
                -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
                -1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, 0.0f};

        float[] mVerticesBackCamera = {
                -1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

        static final String YUV_VS = "" +
                "attribute vec4 a_position;\n" +
                "attribute vec2 a_texcoord;\n" +
                "varying vec2 v_texcoord;\n" +
                "void main() {\n" +
                "   gl_Position =  a_position;\n" +
                "   v_texcoord = a_texcoord;\n" +
                "}";

        static final String YUV_FS = "" +
                "precision highp float;\n" +
                "varying highp vec2 v_texcoord;\n" +
                "uniform sampler2D luminanceTexture;" +
                "uniform sampler2D chrominanceTexture;" +
                "void main() {\n" +
                "   lowp float y = texture2D(luminanceTexture, v_texcoord).r;" +
                "   lowp vec4 uv = texture2D(chrominanceTexture, v_texcoord);" +
                "   mediump vec4 rgba = y * vec4(1.0, 1.0, 1.0, 1.0) + " +
                "                  (uv.a - 0.5) * vec4(0.0, -0.337633, 1.732446, 0.0) + " +
                "                  (uv.r - 0.5) * vec4(1.370705, -0.698001, 0.0, 0.0); " +
                "	gl_FragColor = rgba;" +
                "}";

        protected int mGLProgId;

        protected final int SHORT_SIZE_BYTES = 2;
        protected final int FLOAT_SIZE_BYTES = 4;

        final static String A_POSITION = "a_position";
        final static String A_TEXCOORD = "a_texcoord";
        final static String U_LUMINANCE_SAMPLER = "luminanceTexture";
        final static String U_CHROMINANCE_SAMPLER = "chrominanceTexture";


        protected int maPositionHandle;
        protected int maTextureHandle;
        int mLumninanceSampler;
        int mChrominanceSampler;


        protected int mVertexBufferObjectId;

        int mElementBufferObjectId;

        int mcameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;

        private float[] mProjMatrix = new float[16];

        ByteBuffer mBufferY;
        ByteBuffer mBufferUV;

        short[] mIndices = {0, 1, 2, 1, 2, 3};

        private FloatBuffer mVertexBuffer = null;

        private ShortBuffer mIndexBuffer = null;

        private String mVertexShader;
        private String mFragmentShader;

        int[] mSamplers = null;

        private final PreviewFrameTexture mPreviewFrameTexture = new PreviewFrameTexture();

        boolean mIsInitialized;

        public Filter() {
            this(YUV_VS, YUV_FS);
            mIsInitialized = false;
            init();
        }

        public Filter(String vertexShader, String fragmentShader) {
            mVertexShader = vertexShader;
            mFragmentShader = fragmentShader;
        }

        private void init() {

            mBufferY = ByteBuffer.allocateDirect(mFrameWidth * mFrameHeight);
            //  UV channel. Used for color rendering
            mBufferUV = ByteBuffer.allocateDirect(mFrameWidth * mFrameHeight / 2);

            mPreviewFrameTexture.init();

            mPreviewFrameTexture.initWithPreview(mFrameWidth, mFrameHeight);


            onInit();

            onInitialized();
        }

        private void onInit() {
            //super.onInit();
            mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

            Matrix.orthoM(mProjMatrix, 0, -1, 1, 1, -1, 1, -1);
            maPositionHandle = GLES20.glGetAttribLocation(mGLProgId, A_POSITION);
            maTextureHandle = GLES20.glGetAttribLocation(mGLProgId, A_TEXCOORD);
            //muMVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgId, U_MVPMATRIX);
            //This call will go inside respective filter type. TO FIX

            mLumninanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_LUMINANCE_SAMPLER);
            mChrominanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_CHROMINANCE_SAMPLER);
            mSamplers = new int[2];
            mSamplers[0] = mLumninanceSampler;
            mSamplers[1] = mChrominanceSampler;

            int[] vboIds = new int[2];
            GLES20.glGenBuffers(2, vboIds, 0);
            mVertexBufferObjectId = vboIds[0];
            mElementBufferObjectId = vboIds[1];

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
            float[] modifiedBuffer = mVerticesBackCamera.clone();
            mVertexBuffer = ByteBuffer.allocateDirect(modifiedBuffer.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mVertexBuffer.put(modifiedBuffer).position(0);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * FLOAT_SIZE_BYTES, mVertexBuffer, GLES20.GL_STATIC_DRAW);

            mIndexBuffer = ByteBuffer.allocateDirect(mIndices.length * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
            mIndexBuffer.put(mIndices).position(0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer.capacity() * SHORT_SIZE_BYTES, mIndexBuffer, GLES20.GL_STATIC_DRAW);
        }

        public void onInitialized() {
            mIsInitialized = true;
        }

        private void processVideoFrame(byte[] frame) {
            mBufferY.position(0);
            mBufferY.put(frame, 0, mFrameWidth * mFrameHeight);
            mBufferUV.position(0);

            mBufferUV.put(frame,
                    mFrameWidth * mFrameHeight,
                    mFrameWidth * mFrameHeight / 2);

            mBufferY.position(0);
            mBufferUV.position(0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getYTextureHandle());

            GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    GLES20.GL_LUMINANCE,
                    mFrameWidth,
                    mFrameHeight,
                    0,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    mBufferY
            );

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getUVTextureHandle());

            GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    GLES20.GL_LUMINANCE_ALPHA,
                    mFrameWidth / 2,
                    mFrameHeight / 2,
                    0,
                    GLES20.GL_LUMINANCE_ALPHA,
                    GLES20.GL_UNSIGNED_BYTE,
                    mBufferUV
            );

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, GLES20.GL_NONE);
        }


        public void onDraw(byte[] frame) {
            GLES20.glUseProgram(mGLProgId);
            GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
            if (!mIsInitialized) {
                return;
            }

            processVideoFrame(frame);

            for (int i = 0; i < mPreviewFrameTexture.getTextureHandle().length; i++) {

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPreviewFrameTexture.getTextureHandle()[i]);
                GLES20.glUniform1i(mSamplers[i], i);
            }

            GLES20.glEnableVertexAttribArray(maPositionHandle);
            GLES20.glEnableVertexAttribArray(maTextureHandle);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferObjectId);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, true, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mElementBufferObjectId);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * SHORT_SIZE_BYTES, GLES20.GL_UNSIGNED_SHORT, 0);

            GLES20.glDisableVertexAttribArray(maTextureHandle);
            GLES20.glDisableVertexAttribArray(maPositionHandle);

            GLES20.glFinish();
            GLES20.glUseProgram(0);
        }

        float[] modifyTextureBuffers() {

            float[] modifiedBuffer;
            modifiedBuffer = mVerticesBackCamera.clone();
            return modifiedBuffer;
        }

        public void destroy() {
            mIsInitialized = false;
            GLES20.glDeleteProgram(mGLProgId);
        }
    }


    private class CodecInputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

        private Surface mSurface;

        CodecInputSurface(Surface surface) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;
            eglSetup();
        }

        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                throw new RuntimeException("unable to initialize EGL14");
            }
            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                    numConfigs, 0);
            checkEglError("eglCreateContext RGB888+recordable ES2");
            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                    attrib_list, 0);
            checkEglError("eglCreateContext");
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0);
            checkEglError("eglCreateWindowSurface");
        }

        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }
            mSurface.release();

            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface = null;
        }

        public void makeCurrent() {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        void swapBuffers() {
            EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
            checkEglError("eglSwapBuffers");
        }

        void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
            checkEglError("eglPresentationTimeANDROID");
        }

        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }

}

