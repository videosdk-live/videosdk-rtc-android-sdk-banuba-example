package live.videosdk.rtc.android.java;

import static java.util.Objects.requireNonNull;

import android.app.Application;

import com.androidnetworking.AndroidNetworking;
import com.banuba.sdk.manager.BanubaSdkManager;

import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.VideoSDK;

public class MainApplication extends Application {
    private Meeting meeting;
    public Meeting getMeeting() {
        return meeting;
    }
    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
    @Override
    public void onCreate() {
        super.onCreate();

        String KEY = " Add Banuba SDK Key";
        VideoSDK.initialize(getApplicationContext());
        AndroidNetworking.initialize(getApplicationContext());
        BanubaSdkManager.initialize(requireNonNull(getApplicationContext()), KEY);
    }

    @Override
    public void onTerminate() {
        BanubaSdkManager.deinitialize();
        super.onTerminate();
    }

}