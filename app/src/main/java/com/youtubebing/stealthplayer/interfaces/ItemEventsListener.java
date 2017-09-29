package com.youtubebing.stealthplayer.interfaces;

import com.youtubebing.stealthplayer.model.VideoItem;

/**
 * Created by smedic on 9.2.17..
 */

public interface ItemEventsListener<Model> {
    void onShareClicked(String itemId);

    void onFavoriteClicked(VideoItem video, boolean isChecked);

    void onGarbageClicked(Model video);

    void onItemClick(Model model); //handle click on a row (video or playlist)
}
