package live.videosdk.rtc.android.java.banuba;

import android.content.Context;
import android.os.Handler;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public interface IVideoFrameProcessor {
    void onCaptureCreate(Context context, Handler handler, int width, int height);

    void onCaptureStarted();

    void onCaptureStopped();

    void onCaptureDestroy();

    void pushVideoFrame(VideoFrame videoFrame, boolean isFrontCamera);

    void setSink(VideoSink videoSink);

    void callJsMethod(String method, String param);

    void loadEffect(String effectName);

    void unloadEffect();

    void setProcessorEnabled(Boolean isEnabled);
}
