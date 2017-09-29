/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.youtubebing.stealthplayer;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.youtubebing.stealthplayer.model.ItemType;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.utils.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Service class for background youtube playback
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundAudioService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {


//    WindowManager mWindowManager;
//    View mView;
//    Animation mAnimation;

    private static final String TAG = "SMEDIC service";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;

    private ItemType mediaType = ItemType.YOUTUBE_VIDEO;

    private VideoItem videoItem;

    private boolean isStarting = false;
    private int currentSongIndex = 0;

    private ArrayList<VideoItem> videoItems;

    private NotificationCompat.Builder builder = null;

    private DeviceBandwidthSampler deviceBandwidthSampler;
    private ConnectionQuality connectionQuality = ConnectionQuality.MODERATE;
    private UserPresentBroadcastReceiver mReceiver;
    private boolean first_video_test;

    private boolean is_detail = false;

    private Messenger messageHandler;

    public BackgroundAudioService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        first_video_test = false;

        IntentFilter intent_filter = new IntentFilter("com.stealth.player");
        registerReceiver(receiver, intent_filter);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mReceiver = new UserPresentBroadcastReceiver();
        registerReceiver(mReceiver, filter);


        videoItem = new VideoItem();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        initMediaSessions();
        initPhoneCallListener();
        deviceBandwidthSampler = DeviceBandwidthSampler.getInstance();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initPhoneCallListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    pauseVideo();
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    Log.d(TAG, "onCallStateChanged: ");
                    resumeVideo();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onDestroy() {

        try {
            if (mReceiver != null)
                unregisterReceiver(mReceiver);


            if (receiver != null) {
                unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * Handles intent (player options play/pause/stop...)
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        Log.d("STATE", action);
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            handleMedia(intent);
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    /**
     * Handles media - playlists and videos sent from fragments
     *
     * @param intent
     */
    private void handleMedia(Intent intent) {

        is_detail = false;

        Bundle extras = intent.getExtras();

        ItemType intentMediaType = ItemType.MEDIA_NONE;
        if (intent.getSerializableExtra(Config.NET_TYPE) != null) {
            intentMediaType = (ItemType) intent.getSerializableExtra(Config.NET_TYPE);
        }
        switch (intentMediaType) {
            case MEDIA_NONE: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                break;

            case BING_VIDEO:

                if(extras.containsKey("MESSENGER")) {
                    messageHandler = (Messenger) extras.get("MESSENGER");
                    first_video_test = true;
                    sendMessage(0);
                } else{
                    is_detail = true;
                }

                if (videoItem.getId() != null) {
                    videoItems = (ArrayList<VideoItem>) intent.getSerializableExtra(Config.BING_TYPE_PLAYLIST);
                    int startPosition = intent.getIntExtra(Config.BING_TYPE_PLAYLIST_VIDEO_POS, 0);
                    videoItem = videoItems.get(startPosition);
                    currentSongIndex = startPosition;
                    playVideo();
                }
                break;
            case YOUTUBE_VIDEO: //new playlist playback request

                if(extras.containsKey("MESSENGER")) {
                    messageHandler = (Messenger) extras.get("MESSENGER");
                    first_video_test = true;
                    sendMessage(0);
                } else{
                    is_detail = true;
                }
                videoItems = (ArrayList<VideoItem>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                int startPosition = intent.getIntExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, 0);
                videoItem = videoItems.get(startPosition);
                currentSongIndex = startPosition;
                playVideo();
                break;
            default:
                Log.d(TAG, "Unknown command");
                break;
        }
    }

    /**
     * Initializes media sessions and receives media events
     */
    private void initMediaSessions() {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mSession = new MediaSessionCompat(getApplicationContext(), "simple player session",
                null, buttonReceiverIntent);

        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());

            mSession.setCallback(
                    new MediaSessionCompat.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
                            if ((state != null && state.equalsIgnoreCase("ON")) || Config.POWER_BTN_CLICK_STOP == false) {
                                resumeVideo();
                                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                            }
                        }

                        @Override
                        public void onPause() {
                            super.onPause();
//                            if (state != null && state.equalsIgnoreCase("ON")) {
                            pauseVideo();
                            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
//                            }
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            if ((state != null && state.equalsIgnoreCase("ON")) || Config.POWER_BTN_CLICK_STOP == false) {
                                if (!isStarting) {
                                    playNext();
                                }
                                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                            }
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            if ((state != null && state.equalsIgnoreCase("ON")) || Config.POWER_BTN_CLICK_STOP == false) {
                                if (!isStarting) {
                                    playPrevious();
                                }
                                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                            }
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            stopPlayer();
                            //remove notification and stop service
                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(1);
                            Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                            stopService(intent);
                        }

                        @Override
                        public void onSetRating(RatingCompat rating) {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    /**
     * Builds notification panel with buttons and info on it
     *
     * @param action Action to be applied
     */

    private void buildNotification(NotificationCompat.Action action) {

        final NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
//----------------------DetailActivity--------------------
       // Intent clickIntent = new Intent(this, DetailActivity.class);
       // clickIntent.putExtra(Config.DETAIL_PLAYLIST, (ArrayList) videoItems);
       // clickIntent.putExtra(Config.DETAIL_PLAYLIST_VIDEO_POS, currentSongIndex);
        //--------------------------------------------------end-----------------------------
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(videoItem.getTitle());
        builder.setContentInfo(videoItem.getDuration());
        builder.setShowWhen(false);
        builder.setContentIntent(clickPendingIntent);
     //   builder.setDeleteIntent(stopPendingIntent);
        builder.setOngoing(false);
        builder.setSubText(videoItem.getViewCount());
        builder.setStyle(style);

        //load bitmap for largeScreen
        if (videoItem.getThumbnailUrl() != null && !videoItem.getThumbnailUrl().isEmpty()) {
            Picasso.with(this)
                    .load(videoItem.getThumbnailUrl())
                    .into(target);
        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1, 2);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Field which handles image loading
     */
    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            updateNotificationLargeIcon(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.d(TAG, "Load bitmap... failed");
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }
    };

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     *
     * @param bitmap
     */
    private void updateNotificationLargeIcon(Bitmap bitmap) {
        builder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }


    private void addNotification() {

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(getResources().getString(R.string.warning));

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.warning));

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(5, mBuilder.build());


//        Notification noti = new Notification.Builder(this)
//                .setContentTitle(getResources().getString(R.string.app_name))
//                .setContentText("Unfortunately, Youtube's terms of use DONOT allow us to play videos when the screen is locked.").build();
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        // hide the notification after its selected
//        noti.flags |= Notification.FLAG_AUTO_CANCEL;
//
//        notificationManager.notify(5, noti);
    }

    private void cancelNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(5);

    }


    /**
     * Generates specific action with parameters below
     *
     * @param icon
     * @param title
     * @param intentAction
     * @return
     */
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    /**
     * Plays next video in playlist
     */
    private void playNext() {
        //if media type is video not playlist, just loop it

        if (videoItems.size() > currentSongIndex + 1) {
            currentSongIndex++;
        } else { //play 1st song
            currentSongIndex = 0;
        }

        videoItem = videoItems.get(currentSongIndex);


        playVideo();
    }

    /**
     * Plays previous video in playlist
     */
    private void playPrevious() {
        //if media type is video not playlist, just loop it


        if (currentSongIndex - 1 >= 0) {
            currentSongIndex--;
        } else { //play last song
            currentSongIndex = videoItems.size() - 1;
        }
        videoItem = videoItems.get(videoItems.size() - 1);


        playVideo();
    }

    /**
     * Plays video
     */
    private void playVideo() {
        isStarting = true;
        extractUrlAndPlay();
    }

    /**
     * Pauses video
     */
    private void pauseVideo() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Resumes video
     */
    private void resumeVideo() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            //mMediaPlayer.start();
            mMediaPlayer.start();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo() {
        mMediaPlayer.start();
    }

    /**
     * Seeks to specific time
     *
     * @param seekTo
     */
    private void seekVideo(int seekTo) {
        mMediaPlayer.seekTo(seekTo);
    }

    /**
     * Stops video
     */
    private void stopPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    /**
     * Get the best available audio stream
     * <p>
     * Itags:
     * 141 - mp4a - stereo, 44.1 KHz 256 Kbps
     * 251 - webm - stereo, 48 KHz 160 Kbps
     * 140 - mp4a - stereo, 44.1 KHz 128 Kbps
     * 17 - mp4 - stereo, 44.1 KHz 96-100 Kbps
     *
     * @param ytFiles Array of available streams
     * @return Audio stream with highest bitrate
     */
    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {

        connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        int[] itags = new int[]{251, 141, 140, 17};

        if (connectionQuality != null && connectionQuality != ConnectionQuality.UNKNOWN) {
            switch (connectionQuality) {
                case POOR:
                    itags = new int[]{17, 140, 251, 141};
                    break;
                case MODERATE:
                    itags = new int[]{251, 141, 140, 17};
                    break;
                case GOOD:
                case EXCELLENT:
                    itags = new int[]{141, 251, 140, 17};
                    break;
            }
        }

        if (ytFiles.get(itags[0]) != null) {
            return ytFiles.get(itags[0]);
        } else if (ytFiles.get(itags[1]) != null) {
            return ytFiles.get(itags[1]);
        } else if (ytFiles.get(itags[2]) != null) {
            return ytFiles.get(itags[2]);
        }
        return ytFiles.get(itags[3]);
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay() {

        switch (videoItem.getVideoType()) {
            case Config.YOUTUBE:
                String youtubeLink = Config.YOUTUBE_BASE_URL + videoItem.getId();
                deviceBandwidthSampler.startSampling();

                new YouTubeExtractor(this) {
                    @Override
                    protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                        if (ytFiles == null) {
                            // Something went wrong we got no urls. Always check this.
                            Toast.makeText(YTApplication.getAppContext(), R.string.failed_playback,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        deviceBandwidthSampler.stopSampling();
                        YtFile ytFile = getBestStream(ytFiles);
                        try {
                            if (mMediaPlayer != null) {
                                mMediaPlayer.reset();
                                mMediaPlayer.setDataSource(ytFile.getUrl());
                                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mMediaPlayer.prepare();
                                mMediaPlayer.start();

                                Toast.makeText(YTApplication.getAppContext(), videoItem.getTitle(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException io) {
                            io.printStackTrace();
                        }
                    }
                }.execute(youtubeLink);
                break;
            case Config.BING:
                deviceBandwidthSampler.startSampling();
                try {

                    if (mMediaPlayer != null) {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(videoItem.getId());
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();

                        Toast.makeText(YTApplication.getAppContext(), videoItem.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
                break;
            default:
                Log.d(TAG, "Unknown command");
                break;

        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer) {
        if (mediaType == ItemType.YOUTUBE_VIDEO || mediaType == ItemType.BING_VIDEO) {
            playNext();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            restartVideo();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OURINFO", Context.MODE_MULTI_PROCESS);
        boolean isDetailActive = preferences.getBoolean("active", false);

        sendMessage(1);

       Intent intent = new Intent(this, DetailActivity.class);

         if (isDetailActive || first_video_test) {

            intent.putExtra(Config.DETAIL_PLAYLIST, (ArrayList) videoItems);
            intent.putExtra(Config.DETAIL_PLAYLIST_VIDEO_POS, currentSongIndex);
             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
            first_video_test = false;
        }

       isStarting = false;
    }


    public boolean isRunning(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (ctx.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }


    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }


    public class ScreenHandler extends BroadcastReceiver {

        public ScreenHandler() {
            super();
        }

        @Override
        public void onReceive(Context arg0, Intent intent) {

                    /*Sent when the user is present after
         * device wakes up (e.g when the keyguard is gone)
         * */
            if (intent.getAction().equals("USER_PRESENT")) {

                if (mMediaPlayer != null) {
                    playVideo();
                }
            }
        /*Device is shutting down. This is broadcast when the device
         * is being shut down (completely turned off, not sleeping)
         * */
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (mMediaPlayer != null) {
                    pauseVideo();
                }

            }
        }

    }


    private String state = "ON";
    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            try {
                state = intent.getStringExtra("screen_state");
                if (state != null) {

                    if (state.equalsIgnoreCase("ON")) {
//                        cancelNotification();
                        mMediaPlayer.start();
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        System.out.println("on");
                    } else if (state.equalsIgnoreCase("OFF")) {
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
             //                Intent intentAlert = new Intent(arg0, AlertActivity.class);
             //               intentAlert.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             //               startActivity(intentAlert);
                        }
                        pauseVideo();
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.cancel(1);
//                        addNotification();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    };

    public void sendMessage(int state) {
        Message message = Message.obtain();
        switch (state) {
            case 1 :
                message.arg1 = 1;
                break;
            case 0 :
                message.arg1 = 0;
                break;
        }
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}