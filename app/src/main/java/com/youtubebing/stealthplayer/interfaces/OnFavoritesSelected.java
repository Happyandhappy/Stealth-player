package com.youtubebing.stealthplayer.interfaces;

import com.youtubebing.stealthplayer.model.VideoItem;

/**
 * Created by smedic on 5.3.17..
 */

public interface OnFavoritesSelected {
    void onFavoritesSelected(VideoItem video, boolean isChecked);
}
