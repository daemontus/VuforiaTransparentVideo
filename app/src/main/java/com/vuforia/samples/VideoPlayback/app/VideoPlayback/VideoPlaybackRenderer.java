/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VideoPlayback.app.VideoPlayback;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.SampleMath;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.vuforia.samples.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_TYPE;


// The renderer class for the VideoPlayback sample.
public class VideoPlaybackRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "VideoPlaybackRenderer";
    
    SampleApplicationSession vuforiaAppSession;
    
    // Video Playback Rendering Specific
    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackNormalHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;
    
    // Video Playback Textures for the two targets
    int videoPlaybackTextureID[] = new int[VideoPlayback.NUM_TARGETS];
    
    // Keyframe and icon rendering specific
    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeNormalHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;
    
    // We cannot use the default texture coordinates of the quad since these
    // will change depending on the video itself
    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };
    
    // This variable will hold the transformed coordinates (changes every frame)
    private float videoQuadTextureCoordsTransformedStones[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    
    private float videoQuadTextureCoordsTransformedChips[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    
    // Trackable dimensions
    Vec3F targetPositiveDimensions[] = new Vec3F[VideoPlayback.NUM_TARGETS];
    
    static int NUM_QUAD_VERTEX = 4;
    static int NUM_QUAD_INDEX = 6;
    
    double quadVerticesArray[] = { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f };
    
    double quadTexCoordsArray[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f };
    
    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };
    
    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };
    
    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;
    
    public boolean mIsActive = false;
    
    private float[][] mTexCoordTransformationMatrix = null;
    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private String mMovieName[] = null;
    private MEDIA_TYPE mCanRequestType[] = null;
    private int mSeekPosition[] = null;
    private boolean mShouldPlayImmediately[] = null;
    private long mLostTrackingSince[] = null;
    private boolean mLoadRequested[] = null;
    
    VideoPlayback mActivity;
    
    // Needed to calculate whether a screen tap is inside the target
    Matrix44F modelViewMatrix[] = new Matrix44F[VideoPlayback.NUM_TARGETS];
    
    private Vector<Texture> mTextures;
    
    boolean isTracking[] = new boolean[VideoPlayback.NUM_TARGETS];
    MEDIA_STATE currentStatus[] = new MEDIA_STATE[VideoPlayback.NUM_TARGETS];
    
    // These hold the aspect ratio of both the video and the
    // keyframe
    float videoQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];
    float keyframeQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];
    
    
    public VideoPlaybackRenderer(VideoPlayback activity,
        SampleApplicationSession session)
    {
        
        mActivity = activity;
        vuforiaAppSession = session;
        
        // Create an array of the size of the number of targets we have
        mVideoPlayerHelper = new VideoPlayerHelper[VideoPlayback.NUM_TARGETS];
        mMovieName = new String[VideoPlayback.NUM_TARGETS];
        mCanRequestType = new MEDIA_TYPE[VideoPlayback.NUM_TARGETS];
        mSeekPosition = new int[VideoPlayback.NUM_TARGETS];
        mShouldPlayImmediately = new boolean[VideoPlayback.NUM_TARGETS];
        mLostTrackingSince = new long[VideoPlayback.NUM_TARGETS];
        mLoadRequested = new boolean[VideoPlayback.NUM_TARGETS];
        mTexCoordTransformationMatrix = new float[VideoPlayback.NUM_TARGETS][16];
        
        // Initialize the arrays to default values
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = null;
            mMovieName[i] = "";
            mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
            mSeekPosition[i] = 0;
            mShouldPlayImmediately[i] = false;
            mLostTrackingSince[i] = -1;
            mLoadRequested[i] = false;
        }
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            targetPositiveDimensions[i] = new Vec3F();
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            modelViewMatrix[i] = new Matrix44F();
    }
    
    
    // Store the Player Helper object passed from the main activity
    public void setVideoPlayerHelper(int target,
        VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper[target] = newVideoPlayerHelper;
    }
    
    
    public void requestLoad(int target, String movieName, int seekPosition,
        boolean playImmediately)
    {
        mMovieName[target] = movieName;
        mSeekPosition[target] = seekPosition;
        mShouldPlayImmediately[target] = playImmediately;
        mLoadRequested[target] = true;
    }
    
    
    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        // Call function to initialize rendering:
        // The video texture is also created on this step
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        Vuforia.onSurfaceCreated();
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            
            if (mVideoPlayerHelper[i] != null)
            {
                // The VideoPlayerHelper needs to setup a surface texture given
                // the texture id
                // Here we inform the video player that we would like to play
                // the movie
                // both on texture and on full screen
                // Notice that this does not mean that the platform will be able
                // to do what we request
                // After the file has been loaded one must always check with
                // isPlayableOnTexture() whether
                // this can be played embedded in the AR scene
                if (!mVideoPlayerHelper[i]
                    .setupSurfaceTexture(videoPlaybackTextureID[i]))
                    mCanRequestType[i] = MEDIA_TYPE.FULLSCREEN;
                else
                    mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
                
                // And now check if a load has been requested with the
                // parameters passed from the main activity
                if (mLoadRequested[i])
                {
                    mVideoPlayerHelper[i].load(mMovieName[i],
                        mCanRequestType[i], mShouldPlayImmediately[i],
                        mSeekPosition[i]);
                    mLoadRequested[i] = false;
                }
            }
        }
    }
    
    
    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        // Call Vuforia function to handle render surface size changes:
        Vuforia.onSurfaceChanged(width, height);
        
        // Upon every on pause the movie had to be unloaded to release resources
        // Thus, upon every surface create or surface change this has to be
        // reloaded
        // See:
        // http://developer.android.com/reference/android/media/MediaPlayer.html#release()
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            if (mLoadRequested[i] && mVideoPlayerHelper[i] != null)
            {
                mVideoPlayerHelper[i].load(mMovieName[i], mCanRequestType[i],
                    mShouldPlayImmediately[i], mSeekPosition[i]);
                mLoadRequested[i] = false;
            }
        }
    }
    
    
    // Called to draw the current frame.
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            if (mVideoPlayerHelper[i] != null)
            {
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // First we need to update the video data. This is a built
                    // in Android call
                    // Here, the decoded data is uploaded to the OES texture
                    // We only need to do this if the movie is playing
                    if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper[i].updateVideoData();
                    }
                    
                    // According to the Android API
                    // (http://developer.android.com/reference/android/graphics/SurfaceTexture.html)
                    // transforming the texture coordinates needs to happen
                    // every frame.
                    mVideoPlayerHelper[i]
                        .getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix[i]);
                    setVideoDimensions(i,
                        mVideoPlayerHelper[i].getVideoWidth(),
                        mVideoPlayerHelper[i].getVideoHeight(),
                        mTexCoordTransformationMatrix[i]);
                }
                
                setStatus(i, mVideoPlayerHelper[i].getStatus().getNumericType());
            }
        }
        
        // Call our function to render content
        renderFrame();
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            // Ask whether the target is currently being tracked and if so react
            // to it
            if (isTracking(i))
            {
                // If it is tracking reset the timestamp for lost tracking
                mLostTrackingSince[i] = -1;
            } else
            {
                // If it isn't tracking
                // check whether it just lost it or if it's been a while
                if (mLostTrackingSince[i] < 0)
                    mLostTrackingSince[i] = SystemClock.uptimeMillis();
                else
                {
                    // If it's been more than 2 seconds then pause the player
                    if ((SystemClock.uptimeMillis() - mLostTrackingSince[i]) > 2000)
                    {
                        if (mVideoPlayerHelper[i] != null)
                            mVideoPlayerHelper[i].pause();
                    }
                }
            }
        }
        
        // If you would like the video to start playing as soon as it starts
        // tracking
        // and pause as soon as tracking is lost you can do that here by
        // commenting
        // the for-loop above and instead checking whether the isTracking()
        // value has
        // changed since the last frame. Notice that you need to be careful not
        // to
        // trigger automatic playback for fullscreen since that will be
        // inconvenient
        // for your users.
        
    }
    
    
    @SuppressLint("InlinedApi")
    void initRendering()
    {
        Log.d(LOGTAG, "VideoPlayback VideoPlaybackRenderer initRendering");
        
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            // Here we create the textures for the keyframe
            // and for all the icons
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        // Now we create the texture for the video data from the movie
        // IMPORTANT:
        // Notice that the textures are not typical GL_TEXTURE_2D textures
        // but instead are GL_TEXTURE_EXTERNAL_OES extension textures
        // This is required by the Android SurfaceTexture
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            GLES20.glGenTextures(1, videoPlaybackTextureID, i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                videoPlaybackTextureID[i]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }
        
        // The first shader is the one that will display the video data of the
        // movie
        // (it is aware of the GL_TEXTURE_EXTERNAL_OES extension)
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
            VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
            VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
            videoPlaybackShaderID, "vertexPosition");
        videoPlaybackNormalHandle = GLES20.glGetAttribLocation(
            videoPlaybackShaderID, "vertexNormal");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
            videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
            videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
            videoPlaybackShaderID, "texSamplerOES");
        
        // This is a simpler shader with regular 2D textures
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
            KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
            KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexPosition");
        keyframeNormalHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexNormal");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
            "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
            keyframeShaderID, "texSampler2D");
        
        keyframeQuadAspectRatio[VideoPlayback.STONES] = (float) mTextures
            .get(0).mHeight / (float) mTextures.get(0).mWidth;
        keyframeQuadAspectRatio[VideoPlayback.CHIPS] = (float) mTextures.get(1).mHeight
            / (float) mTextures.get(1).mWidth;
        
        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);
        
    }
    
    
    private Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
                                                                     // float
                                                                     // takes 4
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
                                                                     // short
                                                                     // takes 2
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
                                                                     // float
                                                                     // takes 4
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    @SuppressLint("InlinedApi")
    void renderFrame()
    {
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Get the state from Vuforia and mark the beginning of a rendering
        // section
        State state = Renderer.getInstance().begin();
        
        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
       
        // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
 
        // We must detect if background reflection is active and adjust the
        // culling direction.
        // If the reflection is active, this means the post matrix has been
        // reflected as well,
        // therefore standard counter clockwise face culling will result in
        // "inside out" models.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
            
        float temp[] = { 0.0f, 0.0f, 0.0f };
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            isTracking[i] = false;
            targetPositiveDimensions[i].setData(temp);
        }
        
        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            
            ImageTarget imageTarget = (ImageTarget) trackableResult
                .getTrackable();
            
            int currentTarget;
            
            // We store the modelview matrix to be used later by the tap
            // calculation
            if (imageTarget.getName().compareTo("stones") == 0)
                currentTarget = VideoPlayback.STONES;
            else
                currentTarget = VideoPlayback.CHIPS;
            
            modelViewMatrix[currentTarget] = Tool
                .convertPose2GLMatrix(trackableResult.getPose());
            
            isTracking[currentTarget] = true;
            
            targetPositiveDimensions[currentTarget] = imageTarget.getSize();
            
            // The pose delivers the center of the target, thus the dimensions
            // go from -width/2 to width/2, same for height
            temp[0] = targetPositiveDimensions[currentTarget].getData()[0] / 2.0f;
            temp[1] = targetPositiveDimensions[currentTarget].getData()[1] / 2.0f;
            targetPositiveDimensions[currentTarget].setData(temp);
            
            // If the movie is ready to start playing or it has reached the end
            // of playback we render the keyframe
            if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR))
            {
                float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();
                float[] modelViewProjectionKeyframe = new float[16];
                // Matrix.translateM(modelViewMatrixKeyframe, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);
                
                // Here we use the aspect ratio of the keyframe since it
                // is likely that it is not a perfect square
                
                float ratio = 1.0f;
                if (mTextures.get(currentTarget).mSuccess)
                    ratio = keyframeQuadAspectRatio[currentTarget];
                else
                    ratio = targetPositiveDimensions[currentTarget].getData()[1]
                        / targetPositiveDimensions[currentTarget].getData()[0];
                
                Matrix.scaleM(modelViewMatrixKeyframe, 0,
                    targetPositiveDimensions[currentTarget].getData()[0],
                    targetPositiveDimensions[currentTarget].getData()[0]
                        * ratio,
                    targetPositiveDimensions[currentTarget].getData()[0]);
                Matrix.multiplyMM(modelViewProjectionKeyframe, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixKeyframe, 0);

                //enable blending
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                GLES20.glUseProgram(keyframeShaderID);
                
                // Prepare for rendering the keyframe
                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, quadTexCoords);
                
                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                
                // The first loaded texture from the assets folder is the
                // keyframe
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(currentTarget).mTextureID[0]);
                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                    modelViewProjectionKeyframe, 0);
                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
                
                // Render
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
                
                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glUseProgram(0);
                GLES20.glDisable(GLES20.GL_BLEND);
            } else
            // In any other case, such as playing or paused, we render
            // the actual contents
            {
                float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();
                float[] modelViewProjectionVideo = new float[16];
                // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);
                
                // Here we use the aspect ratio of the video frame
                Matrix.scaleM(modelViewMatrixVideo, 0,
                    targetPositiveDimensions[currentTarget].getData()[0],
                    targetPositiveDimensions[currentTarget].getData()[0]
                        * videoQuadAspectRatio[currentTarget],
                    targetPositiveDimensions[currentTarget].getData()[0]);
                Matrix.multiplyMM(modelViewProjectionVideo, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixVideo, 0);

                //Enable blending
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                GLES20.glUseProgram(videoPlaybackShaderID);
                
                // Prepare for rendering the keyframe
                GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
                
                if (imageTarget.getName().compareTo("stones") == 0)
                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                        2, GLES20.GL_FLOAT, false, 0,
                        fillBuffer(videoQuadTextureCoordsTransformedStones));
                else
                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                        2, GLES20.GL_FLOAT, false, 0,
                        fillBuffer(videoQuadTextureCoordsTransformedChips));
                
                GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                
                // IMPORTANT:
                // Notice here that the texture that we are binding is not the
                // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[currentTarget]);
                GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                    false, modelViewProjectionVideo, 0);
                GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);
                
                // Render
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
                
                GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);
                
                GLES20.glUseProgram(0);
                GLES20.glDisable(GLES20.GL_BLEND);
            }
            
            // The following section renders the icons. The actual textures used
            // are loaded from the assets folder
            
            if ((currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                || (currentStatus[currentTarget] == VideoPlayerHelper.MEDIA_STATE.ERROR))
            {
                // If the movie is ready to be played, pause, has reached end or
                // is not
                // ready then we display one of the icons
                float[] modelViewMatrixButton = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();
                float[] modelViewProjectionButton = new float[16];
                
                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA);
                
                // The inacuracy of the rendering process in some devices means
                // that
                // even if we use the "Less or Equal" version of the depth
                // function
                // it is likely that we will get ugly artifacts
                // That is the translation in the Z direction is slightly
                // different
                // Another posibility would be to use a depth func "ALWAYS" but
                // that is typically not a good idea
                Matrix
                    .translateM(
                        modelViewMatrixButton,
                        0,
                        0.0f,
                        0.0f,
                        targetPositiveDimensions[currentTarget].getData()[1] / 10.98f);
                Matrix
                    .scaleM(
                        modelViewMatrixButton,
                        0,
                        (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f),
                        (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f),
                        (targetPositiveDimensions[currentTarget].getData()[1] / 2.0f));
                Matrix.multiplyMM(modelViewProjectionButton, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0,
                    modelViewMatrixButton, 0);
                
                GLES20.glUseProgram(keyframeShaderID);
                
                GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(keyframeNormalHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadNormals);
                GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, quadTexCoords);
                
                GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                GLES20.glEnableVertexAttribArray(keyframeNormalHandle);
                GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                
                // Depending on the status in which we are we choose the
                // appropriate
                // texture to display. Notice that unlike the video these are
                // regular
                // GL_TEXTURE_2D textures
                switch (currentStatus[currentTarget])
                {
                    case READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                        break;
                    case REACHED_END:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                        break;
                    case PAUSED:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(2).mTextureID[0]);
                        break;
                    case NOT_READY:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(3).mTextureID[0]);
                        break;
                    case ERROR:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(4).mTextureID[0]);
                        break;
                    default:
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(3).mTextureID[0]);
                        break;
                }
                GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                    modelViewProjectionButton, 0);
                GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
                
                // Render
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);
                
                GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                GLES20.glDisableVertexAttribArray(keyframeNormalHandle);
                GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);
                
                GLES20.glUseProgram(0);
                
                // Finally we return the depth func to its original state
                GLES20.glDepthFunc(GLES20.GL_LESS);
                GLES20.glDisable(GLES20.GL_BLEND);
            }
            
            SampleUtils.checkGLError("VideoPlayback renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
        
    }
    
    
    boolean isTapOnScreenInsideTarget(int target, float x, float y)
    {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();
        
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
            .Matrix44FInverse(vuforiaAppSession.getProjectionMatrix()),
            modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
            new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));
        
        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target]
            .getData()[0]))
            && (intersection.getData()[0] <= (targetPositiveDimensions[target]
                .getData()[0]))
            && (intersection.getData()[1] >= -(targetPositiveDimensions[target]
                .getData()[1]))
            && (intersection.getData()[1] <= (targetPositiveDimensions[target]
                .getData()[1])))
            return true;
        else
            return false;
    }
    
    
    void setVideoDimensions(int target, float videoWidth, float videoHeight,
        float[] textureCoordMatrix)
    {
        // The quad originaly comes as a perfect square, however, the video
        // often has a different aspect ration such as 4:3 or 16:9,
        // To mitigate this we have two options:
        // 1) We can either scale the width (typically up)
        // 2) We can scale the height (typically down)
        // Which one to use is just a matter of preference. This example scales
        // the height down.
        // (see the render call in renderFrame)
        videoQuadAspectRatio[target] = videoHeight / videoWidth;
        
        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];
        
        if (target == VideoPlayback.STONES)
        {
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[0],
                videoQuadTextureCoordsTransformedStones[1],
                videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedStones[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[2],
                videoQuadTextureCoordsTransformedStones[3],
                videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedStones[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[4],
                videoQuadTextureCoordsTransformedStones[5],
                videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedStones[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[6],
                videoQuadTextureCoordsTransformedStones[7],
                videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedStones[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedStones[7] = tempUVMultRes[1];
        } else if (target == VideoPlayback.CHIPS)
        {
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedChips[0],
                videoQuadTextureCoordsTransformedChips[1],
                videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformedChips[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedChips[2],
                videoQuadTextureCoordsTransformedChips[3],
                videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformedChips[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedChips[4],
                videoQuadTextureCoordsTransformedChips[5],
                videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformedChips[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedChips[6],
                videoQuadTextureCoordsTransformedChips[7],
                videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformedChips[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformedChips[7] = tempUVMultRes[1];
        }
        
        // textureCoordMatrix = mtx;
    }
    
    
    // Multiply the UV coordinates by the given transformation matrix
    float[] uvMultMat4f(float transformedU, float transformedV, float u,
        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
            * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
            * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;
        
        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }
    
    
    void setStatus(int target, int value)
    {
        // Transform the value passed from java to our own values
        switch (value)
        {
            case 0:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }
    
    
    boolean isTracking(int target)
    {
        return isTracking[target];
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
    
}
