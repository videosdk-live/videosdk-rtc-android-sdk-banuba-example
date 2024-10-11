package live.videosdk.rtc.android.java.banuba;
import android.os.Handler;
import android.util.Size;
import com.banuba.sdk.effect_player.CameraOrientation;
import com.banuba.sdk.frame.FramePixelBufferFormat;
import com.banuba.sdk.input.StreamInput;
import com.banuba.sdk.output.FrameOutput;
import com.banuba.sdk.player.Player;
import com.banuba.sdk.types.FrameData;
import com.banuba.sdk.types.FullImageData;
import org.webrtc.EglBase;
import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;
import java.util.Objects;
import live.videosdk.rtc.android.listeners.VideoFrameProcessor;

public class BanubaProcessor implements VideoFrameProcessor {
    private Player mPlayer;
    private FrameOutput mFrameOutput;
    private final EglBase.Context eglContext = EglBase.create().getEglBaseContext();
    private SurfaceTextureHelper mSurfaceTextureHelper;
    VideoFrame videoFrame;
    final StreamInput streamInput = new StreamInput();

    public BanubaProcessor (){
        mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext, false);
        mPlayer = new Player();
    }

    // Method to set a visual effect in the Banuba Player
    public void setEffect(String effect)
    {
        mPlayer.loadAsync("effects/" + effect);
        mPlayer.play();
    }

    // Method to remove any applied effects
    public void removeEffect()
    {
        mPlayer.loadAsync("" );
    }

    // Method to close the player when it's no longer needed
    public void removePlayer()
    {
        mPlayer.close();
    }

    // Method to initialize the frame output for video processing
    private VideoFrame initOutput() {
        final Handler handler = mSurfaceTextureHelper.getHandler();
        mFrameOutput = new FrameOutput((iOutput, framePixelBuffer) -> {
            handler.post(() -> {

                final JavaI420Buffer i420buffer = JavaI420Buffer.wrap(
                        framePixelBuffer.getWidth(),
                        framePixelBuffer.getHeight(),
                        framePixelBuffer.getPlane(0),
                        framePixelBuffer.getBytesPerRowOfPlane(0),
                        framePixelBuffer.getPlane(1),
                        framePixelBuffer.getBytesPerRowOfPlane(1),
                        framePixelBuffer.getPlane(2),
                        framePixelBuffer.getBytesPerRowOfPlane(2),
                        null);
                videoFrame = new VideoFrame(i420buffer, 0, System.nanoTime());
            });
        });
        mFrameOutput.setFormat(FramePixelBufferFormat.I420_BT601_FULL);

        // Close the FrameOutput after a delay of 2 seconds
        handler.postDelayed(() -> {
            mFrameOutput.close();
        },2000);

        // Attach the FrameOutput to the player for rendering
        mPlayer.use(mFrameOutput);

        // Return the processed video frame
        return videoFrame;
    }

    // Method called when a new video frame is captured
    @Override
    public VideoFrame onFrameCaptured(VideoFrame videoFrame) {
        if(videoFrame != null) {
            // Determine the camera orientation based on the rotation of the video frame
            final CameraOrientation cameraOrientation = CameraOrientation.values()[(videoFrame.getRotation() / 90) % 4];
            final FullImageData.Orientation orientation = new FullImageData.Orientation(cameraOrientation, true, 0);
            final VideoFrame.I420Buffer i420Buffer = videoFrame.getBuffer().toI420();

            // Create FullImageData using the frame data and orientation
            final FullImageData fullImageData = new FullImageData(
                    new Size(i420Buffer.getWidth(), i420Buffer.getHeight()),
                    i420Buffer.getDataY(),
                    i420Buffer.getDataU(),
                    i420Buffer.getDataV(),
                    i420Buffer.getStrideY(),
                    i420Buffer.getStrideU(),
                    i420Buffer.getStrideV(),
                    1, 1, 1,
                    orientation);
            final FrameData frameData = Objects.requireNonNull(FrameData.create());
            frameData.addFullImg(fullImageData);
            i420Buffer.release();
            streamInput.push(frameData, videoFrame.getTimestampNs());

            // Attach the StreamInput to the player for further processing
            mPlayer.use(streamInput);

            // Return the processed video frame
            return initOutput();
        }
        return null;
    }
}
