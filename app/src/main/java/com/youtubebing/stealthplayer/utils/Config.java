package com.youtubebing.stealthplayer.utils;

/**
 * Basic configuration values used in app
 * Created by smedic on 2.2.16..
 */

public final class Config {

    public static final boolean DEBUG = false;

    public static final String SUGGESTIONS_URL = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=";
    public static final String YOUTUBE_BASE_URL = "http://youtube.com/watch?v=";
    public static final String SHARE_VIDEO_URL = "http://youtube.com/watch?v=";
    public static final String SHARE_PLAYLIST_URL = "https://www.youtube.com/playlist?list=";
    public static final String YOUTUBE_TYPE = "YT_MEDIA_TYPE";

    public static final String NET_TYPE = "NET_TYPE";

    public static final String YOUTUBE_TYPE_PLAYLIST= "YT_PLAYLIST";
    public static final String YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE_API_KEY = "AIzaSyAR3lyb-ucc8JYrSHw0rfCaXCYHveGy6U8";

    public static final String KEY_NAME     = "Ocp-Apim-Subscription-Key";
    public static final String ACCOUNT_KEY  = "0476fd8897dc4d8c884233efb7442cd3";
    public static final String BING_URL     = "https://api.cognitive.microsoft.com/bing/v5.0/videos/search?q=";

    public static final String BING_TYPE_PLAYLIST= "B_PLAYLIST";
    public static final String BING_TYPE_PLAYLIST_VIDEO_POS = "B_PLAYLIST_VIDEO_POS";

    public static final String DETAIL_PLAYLIST= "D_PLAYLIST";
    public static final String DETAIL_PLAYLIST_VIDEO_POS = "D_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE = "YOUTUBE";
    public static final String BING = "BING";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 30; //due to YouTube API rules - MAX 50

    public static boolean POWER_BTN_CLICK_STOP;
}