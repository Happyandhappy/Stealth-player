package com.youtubebing.stealthplayer.interfaces;

import com.youtubebing.stealthplayer.model.VideoItem;

import java.util.List;

/**
 * Created by smedic on 5.3.17..
 */

public interface OnItemSelected {
    void onVideoSelected(VideoItem video);

    void onYouTubePlaylistSelected(List<VideoItem> playlist, int position);
    void onBingPlaylistSelected(List<VideoItem> playlist, int position);
}
