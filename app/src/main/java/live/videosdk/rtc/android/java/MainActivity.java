package live.videosdk.rtc.android.java;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.json.JSONObject;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import live.videosdk.rtc.android.CustomStreamTrack;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.Stream;
import live.videosdk.rtc.android.VideoSDK;
import live.videosdk.rtc.android.java.banuba.BanubaProcessor;
import live.videosdk.rtc.android.lib.AppRTCAudioManager;
import live.videosdk.rtc.android.lib.PeerConnectionUtils;
import live.videosdk.rtc.android.listeners.MeetingEventListener;
import live.videosdk.rtc.android.listeners.MicRequestListener;
import live.videosdk.rtc.android.listeners.ParticipantEventListener;
import live.videosdk.rtc.android.listeners.WebcamRequestListener;
import live.videosdk.rtc.android.model.LivestreamOutput;

public class MainActivity extends AppCompatActivity {
    private Meeting meeting;
    private SurfaceViewRenderer svrLocal, svrShare;
    private boolean micEnabled = true;
    private boolean webcamEnabled = true;
    private boolean recording = false;
    private boolean livestreaming = false;
    private boolean localScreenShare = false;
    private static final String YOUTUBE_RTMP_URL = null;
    private static final String YOUTUBE_RTMP_STREAM_KEY = null;
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    private FloatingActionButton btnScreenShare,btnMic,btnWebcam;
    private EffectsAdapter mEffectsAdapter;
    BanubaProcessor banubaProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("VideoSDK RTC");
        setSupportActionBar(toolbar);

        svrLocal = findViewById(R.id.svrLocal);
        svrLocal.init(PeerConnectionUtils.getEglContext(), null);

        svrShare = findViewById(R.id.svrShare);
        svrShare.init(PeerConnectionUtils.getEglContext(), null);

        //
        btnScreenShare = findViewById(R.id.btnScreenShare);
        btnMic = findViewById(R.id.btnMic);
        btnWebcam = findViewById(R.id.btnWebcam);
        //
        final String token = getIntent().getStringExtra("token");
        final String meetingId = getIntent().getStringExtra("meetingId");
        final String participantName = "John Doe";

        // pass the token generated from api server
        VideoSDK.config(token);

        Map<String, CustomStreamTrack> customTracks = new HashMap<>();

        CustomStreamTrack videoCustomTrack = VideoSDK.createCameraVideoTrack("h720p_w1280p", "front", CustomStreamTrack.VideoMode.TEXT, false, this,null);
        customTracks.put("video", videoCustomTrack);

        CustomStreamTrack audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this);
        customTracks.put("mic", audioCustomTrack);

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(
                MainActivity.this, meetingId, participantName,
                micEnabled, webcamEnabled, null, null, false, customTracks, null
        );

        ((MainApplication) this.getApplication()).setMeeting(meeting);

        meeting.addEventListener(meetingEventListener);

        //
        final RecyclerView rvParticipants = findViewById(R.id.rvParticipants);
        rvParticipants.setLayoutManager(new GridLayoutManager(this, 2));
        rvParticipants.setAdapter(new ParticipantAdapter(meeting));

        // Local participant listeners
        setLocalListeners();

        //
        checkPermissions();

        // Actions
        setActionListeners();

        setAudioDeviceListeners();

        //
        final TextView tvMeetingId = findViewById(R.id.tvMeetingId);
        tvMeetingId.setText(meetingId);
        tvMeetingId.setOnClickListener(v -> copyTextToClipboard(meetingId));
    }

    private void setAudioDeviceListeners() {
        meeting.setAudioDeviceChangeListener(new AppRTCAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(AppRTCAudioManager.AudioDevice selectedAudioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                switch (selectedAudioDevice) {
                    case BLUETOOTH:
                        ((ImageButton) findViewById(R.id.btnAudioSelection)).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_bluetooth_audio_24));
                        break;
                    case WIRED_HEADSET:
                        ((ImageButton) findViewById(R.id.btnAudioSelection)).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_headset_24));
                        break;
                    case SPEAKER_PHONE:
                        ((ImageButton) findViewById(R.id.btnAudioSelection)).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_volume_up_24));
                        break;
                    case EARPIECE:
                        ((ImageButton) findViewById(R.id.btnAudioSelection)).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_baseline_phone_in_talk_24));
                        break;
                }
            }
        });
    }


    private final MeetingEventListener meetingEventListener = new MeetingEventListener() {
        @Override
        public void onMeetingJoined() {
            Log.d("#meeting", "onMeetingJoined()");
//
           banubaProcessor = new BanubaProcessor();
            VideoSDK.applyVideoProcessor(banubaProcessor) ;
        }

        @Override
        public void onMeetingLeft() {
            Log.d("#meeting", "onMeetingLeft()");
            meeting = null;
            finish();
        }

        @Override
        public void onParticipantJoined(Participant participant) {
            Toast.makeText(MainActivity.this, participant.getDisplayName() + " joined",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onParticipantLeft(Participant participant) {
            Toast.makeText(MainActivity.this, participant.getDisplayName() + " left",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPresenterChanged(String participantId) {
            updatePresenter(participantId);
        }

        @Override
        public void onRecordingStarted() {
            recording = true;
            Toast.makeText(MainActivity.this, "Recording started",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRecordingStopped() {
            recording = false;
            Toast.makeText(MainActivity.this, "Recording stopped",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLivestreamStarted() {
            livestreaming = true;
            Toast.makeText(MainActivity.this, "Livestream started",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLivestreamStopped() {
            livestreaming = false;
            Toast.makeText(MainActivity.this, "Livestream stopped",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMicRequested(String participantId, MicRequestListener listener) {
            showMicRequestDialog(listener);
        }

        @Override
        public void onWebcamRequested(String participantId, WebcamRequestListener listener) {
            showWebcamRequestDialog(listener);
        }

        @Override
        public void onExternalCallStarted() {
            Log.d("#meeting", "onExternalCallAnswered: User Answered a Call");
        }

        @Override
        public void onError(JSONObject error) {
            try {
                JSONObject errorCodes = VideoSDK.getErrorCodes();
                int code = error.getInt("code");
                if (code == errorCodes.getInt("START_LIVESTREAM_FAILED")) {
                    Log.d("#error", "Error is: " + error.get("message"));
                } else if (code == errorCodes.getInt("START_RECORDING_FAILED")) {
                    Log.d("#error", "Error is: " + error.get("message"));
                } else if (code == errorCodes.getInt("AUDIO_CONSUMER_FAILED")) {
                    Log.d("#error", "Error is: " + error.get("message"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @TargetApi(21)
    private void askPermissionForScreenShare() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(MainActivity.this, "You didn't give permission to capture the screen.", Toast.LENGTH_SHORT).show();
            localScreenShare = false;
            return;
        }

        //Used custom track for screen share.
        VideoSDK.createScreenShareVideoTrack("h720p_5fps", data, this, (track) -> {
            meeting.enableScreenShare(track);
        });

        //To Use simple track you can use below line of code
        // meeting.enableScreenShare(data);
        btnScreenShare.setImageResource(R.drawable.ic_outline_stop_screen_share_24);
    }

    private void updatePresenter(String participantId) {
        if (meeting != null) {
            if (participantId == null) {
                svrShare.clearImage();
                svrShare.setVisibility(View.GONE);
                btnScreenShare.setEnabled(true);
                return;
            } else {
                btnScreenShare.setEnabled(meeting.getLocalParticipant().getId().equals(participantId));
            }

            // find participant

            Participant participant = meeting.getParticipants().get(participantId);
            if (participant == null) return;

            // find share stream in participant
            Stream shareStream = null;

            for (Stream stream : participant.getStreams().values()) {
                if (stream.getKind().equals("share")) {
                    shareStream = stream;
                    break;
                }
            }

            if (shareStream == null) return;
            // display share video
            svrShare.setVisibility(View.VISIBLE);
            svrShare.setZOrderMediaOverlay(true);
            svrShare.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

            VideoTrack videoTrack = (VideoTrack) shareStream.getTrack();
            videoTrack.addSink(svrShare);

            // listen for share stop event
            participant.addEventListener(new ParticipantEventListener() {
                @Override
                public void onStreamDisabled(Stream stream) {
                    if (stream.getKind().equals("share")) {
                        VideoTrack track = (VideoTrack) stream.getTrack();
                        if (track != null) track.removeSink(svrShare);

                        svrShare.clearImage();
                        svrShare.setVisibility(View.GONE);
                        localScreenShare = false;
                    }
                }
            });
        }
    }

    private final PermissionHandler permissionHandler = new PermissionHandler() {
        @Override
        public void onGranted() {
            if (meeting != null) meeting.join();
        }
    };

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE
        };
        String rationale = "Please provide permissions";
        Permissions.Options options =
                new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
        Permissions.check(this, permissions, rationale, options, permissionHandler);
    }

    private void setLocalListeners() {
        meeting.getLocalParticipant().addEventListener(new ParticipantEventListener() {
            @Override
            public void onStreamEnabled(Stream stream) {
                if (stream.getKind().equalsIgnoreCase("video")) {
                    svrShare.setVisibility(View.VISIBLE);
                    svrShare.setZOrderMediaOverlay(true);

                    VideoTrack track = (VideoTrack) stream.getTrack();
                    track.addSink(svrShare);

                    webcamEnabled = true;
                    toggleWebcamIcon();
                } else if (stream.getKind().equalsIgnoreCase("audio")) {
                    micEnabled = true;
                    toggleMicIcon();
                } else if (stream.getKind().equalsIgnoreCase("share")) {
                    // display share video
                    svrShare.setVisibility(View.VISIBLE);
                    svrShare.setZOrderMediaOverlay(true);
                    svrShare.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

                    VideoTrack videoTrack = (VideoTrack) stream.getTrack();
                    videoTrack.addSink(svrShare);
                    //
                    localScreenShare = true;
                }
            }

            @Override
            public void onStreamDisabled(Stream stream) {
                if (stream.getKind().equalsIgnoreCase("video")) {
                    VideoTrack track = (VideoTrack) stream.getTrack();
                    if (track != null) track.removeSink(svrShare);

                    svrShare.clearImage();
                    svrShare.setVisibility(View.GONE);

                    webcamEnabled = false;
                    toggleWebcamIcon();
                } else if (stream.getKind().equalsIgnoreCase("audio")) {
                    micEnabled = false;
                    toggleMicIcon();
                } else if (stream.getKind().equalsIgnoreCase("share")) {
                    VideoTrack track = (VideoTrack) stream.getTrack();
                    if (track != null) track.removeSink(svrShare);
                    svrShare.clearImage();
                    svrShare.setVisibility(View.GONE);
                    //
                    localScreenShare = false;
                }
            }
        });
    }

    private void toggleWebcamIcon() {
        if (webcamEnabled) {
            btnWebcam.setImageResource(R.drawable.ic_baseline_videocam_24);
            btnWebcam.setColorFilter(Color.WHITE);
            btnWebcam.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

        } else {
            btnWebcam.setImageResource(R.drawable.ic_baseline_videocam_off_24);
            btnWebcam.setColorFilter(Color.BLACK);
            btnWebcam.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.md_grey_300)));
        }
    }

    private void toggleMicIcon() {
        if (micEnabled) {
            btnMic.setImageResource(R.drawable.ic_baseline_mic_24);
            btnMic.setColorFilter(Color.WHITE);
            btnMic.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
        } else {
            btnMic.setImageResource(R.drawable.ic_baseline_mic_off_24);
            btnMic.setColorFilter(Color.BLACK);
            btnMic.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.md_grey_300)));
        }
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied text", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(MainActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }


    private void setActionListeners() {
        // Toggle mic
        btnMic.setOnClickListener(view -> {
            if (micEnabled) {
                meeting.muteMic();
            } else {
//                meeting.unmuteMic();

//                PeerConnectionUtils peerConnectionUtils = new PeerConnectionUtils();
//                meeting.enableMic(peerConnectionUtils.createAudioTrack(MainActivity.this, "mic"));

                meeting.unmuteMic(VideoSDK.createAudioTrack("speech_low_quality", null));
            }
        });

        // Toggle webcam
        btnWebcam.setOnClickListener(view -> {
            if (webcamEnabled) {
                meeting.disableWebcam();
            } else {
                CustomStreamTrack videoCustomTrack = VideoSDK.createCameraVideoTrack("h720p_w1280p", "front", CustomStreamTrack.VideoMode.TEXT, false, this,null);

           //     initVideoFrameProcessor(videoCustomTrack.getVideoSource(), videoCustomTrack);

                meeting.enableWebcam(videoCustomTrack);
            }
        });

        // Leave meeting
        findViewById(R.id.btnLeave).setOnClickListener(view -> {
            showLeaveOrEndDialog();
        });

        // Participants list
//        findViewById(R.id.btnParticipants).setOnClickListener(view -> {
//            showParticipantsDialog();
//        });

        findViewById(R.id.btnMore).setOnClickListener(v -> showMoreOptionsDialog());

        findViewById(R.id.btnChat).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        btnScreenShare.setOnClickListener(view -> {
            toggleScreenSharing();
        });

        findViewById(R.id.btnAudioSelection).setOnClickListener(v -> showAudioInputDialog());

        findViewById(R.id.btnSwitchCamera).setOnClickListener(v -> meeting.changeWebcam());

        findViewById(R.id.btnBanuba).setOnClickListener(v -> showDialog());

    }

    private void showLeaveOrEndDialog() {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("Leave or End meeting")
                .setMessage("Leave from meeting or end the meeting for everyone ?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    meeting.leave();
                    finish();
                })
                .setNegativeButton("End", (dialog, which) -> {
                    meeting.end();
                    finish();
                })
                .show();
    }

    private void showParticipantsDialog() {
        // Prepare list
        final int nParticipants = meeting.getParticipants().size();

        final String[] items = nParticipants > 0
                ? new String[nParticipants]
                : new String[]{"No participants have joined yet."};

        final Iterator<Participant> participants = meeting.getParticipants().values().iterator();

        for (int i = 0; i < nParticipants; i++) {
            final Participant participant = participants.next();
            items[i] = participant.getId() + " - " + participant.getDisplayName();
        }

        // Display list in dialog
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(getString(R.string.participants_list))
                .setItems(items, null)
                .show();
    }

    private void showMoreOptionsDialog() {
        final String[] items = new String[]{
                recording ? "Stop recording" : "Start recording",
                livestreaming ? "Stop livestreaming" : "Start livestreaming"
        };

        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(getString(R.string.more_options))
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: {
                            toggleRecording();
                            break;
                        }
                        case 1: {
                            toggleLivestreaming();
                            break;
                        }
                    }
                })
                .show();
    }

    private void toggleRecording() {
        if (!recording) {
    //        meeting.startRecording(null);
        } else {
            meeting.stopRecording();
        }
    }

    private void toggleLivestreaming() {
        if (!livestreaming) {
            if (YOUTUBE_RTMP_URL == null || YOUTUBE_RTMP_STREAM_KEY == null) {
                throw new Error("RTMP url or stream key missing.");
            }

            List<LivestreamOutput> outputs = new ArrayList<>();
            outputs.add(new LivestreamOutput(YOUTUBE_RTMP_URL, YOUTUBE_RTMP_STREAM_KEY));

//            meeting.startLivestream(outputs);
        } else {
            meeting.stopLivestream();
        }
    }

    private void toggleScreenSharing() {
        if (!localScreenShare) {
            askPermissionForScreenShare();
        } else {
            meeting.disableScreenShare();
            btnScreenShare.setImageResource(R.drawable.ic_outline_screen_share_24);
        }
        localScreenShare = !localScreenShare;
    }


    private void showMicRequestDialog(MicRequestListener listener) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("Mic requested")
                .setMessage("Host is asking you to unmute your mic, do you want to allow ?")
                .setPositiveButton("Yes", (dialog, which) -> listener.accept())
                .setNegativeButton("No", (dialog, which) -> listener.reject())
                .show();
    }

    private void showWebcamRequestDialog(WebcamRequestListener listener) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("Webcam requested")
                .setMessage("Host is asking you to enable your webcam, do you want to allow ?")
                .setPositiveButton("Yes", (dialog, which) -> listener.accept())
                .setNegativeButton("No", (dialog, which) -> listener.reject())
                .show();
    }

    @Override
    public void onBackPressed() {
        showLeaveOrEndDialog();
    }

    @Override
    protected void onDestroy() {
        if (meeting != null) meeting.leave();
        svrShare.clearImage();
        svrShare.release();
        svrShare.clearImage();
        svrShare.setVisibility(View.GONE);
        svrShare.release();
        banubaProcessor.removePlayer();
        super.onDestroy();
    }

    private void showAudioInputDialog() {
        Set<AppRTCAudioManager.AudioDevice> mics = meeting.getMics();

        // Prepare list
        final String[] items = new String[mics.size()];
        for (int i = 0; i < mics.size(); i++) {
            items[i] = mics.toArray()[i].toString();
        }
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(getString(R.string.audio_options))
                .setItems(items, (dialog, which) -> {
                    AppRTCAudioManager.AudioDevice audioDevice = null;
                    switch (items[which]) {
                        case "BLUETOOTH":
                            audioDevice = AppRTCAudioManager.AudioDevice.BLUETOOTH;
                            break;
                        case "WIRED_HEADSET":
                            audioDevice = AppRTCAudioManager.AudioDevice.WIRED_HEADSET;
                            break;
                        case "SPEAKER_PHONE":
                            audioDevice = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE;
                            break;
                        case "EARPIECE":
                            audioDevice = AppRTCAudioManager.AudioDevice.EARPIECE;
                            break;
                    }
                })
                .show();

    }

    private void showDialog() {
        Dialog dialog = new Dialog(MainActivity.this);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recycle);


        List<String> pathsToEffectsList = new ArrayList<String>();
        pathsToEffectsList.add("off()");
        AssetManager manager = getApplicationContext().getAssets();

        String path = "bnb-resources/effects/";

        try {
           String[]  items= manager.list(path);

            if (items != null) {
                for (String item : items) {
                    // Construct the full path to check if it's a directory
                    String itemPath = path.isEmpty() ? item : path + item;
                    Log.d("TAG", "showDialog: 1 " + itemPath);

                    // Try listing the contents of the item; if not null, it's a directory
                    String[] subItems = manager.list(itemPath);
                    if (subItems != null && subItems.length > 0) {
                        pathsToEffectsList.add(item);
                    }
                }
            } else {
                Log.e("TAG", "No items found in assets/" + path);
            }

        } catch (IOException ignored) {

        }

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerView);
        mEffectsAdapter = new EffectsAdapter(pathsToEffectsList);
        recyclerView.setAdapter(mEffectsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        dialog.show();

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);

        /* effects load callback */
        mEffectsAdapter.setOnItemClickListener(effectName -> {
            Log.d("TAG", "showDialog: " + effectName);
            if (effectName == "off()") {
                banubaProcessor.removeEffect();
            } else {
                banubaProcessor.setEffect(effectName);
            }
            dialog.dismiss();
        });
    }

}