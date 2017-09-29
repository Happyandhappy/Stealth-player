package com.youtubebing.stealthplayer.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.utils.Config;
import com.youtubebing.stealthplayer.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static com.youtubebing.stealthplayer.youtube.YouTubeSingleton.getYouTube;

/**
 * Created by smedic on 13.2.17..
 */

public class YouTubeVideosLoader extends AsyncTaskLoader<List<VideoItem>> {

    private YouTube youtube = getYouTube();
    private String keywords;

    public YouTubeVideosLoader(Context context, String keywords) {
        super(context);
        this.keywords = keywords;
    }

    @Override
    public List<VideoItem> loadInBackground() {

        ArrayList<VideoItem> items = new ArrayList<>();
        try {
            YouTube.Search.List searchList = youtube.search().list("id,snippet");
            YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails,statistics");

            searchList.setKey(Config.YOUTUBE_API_KEY);
            searchList.setType("video"); //TODO ADD PLAYLISTS SEARCH
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url)");

            videosList.setKey(Config.YOUTUBE_API_KEY);
            videosList.setFields("items(contentDetails/duration,statistics/viewCount)");

            //search
            searchList.setQ(keywords);
            SearchListResponse searchListResponse = searchList.execute();
            List<SearchResult> searchResults = searchListResponse.getItems();

            //find video list
            videosList.setId(Utils.concatenateIDs(searchResults));  //save all ids from searchList list in order to find video list
            VideoListResponse resp = videosList.execute();
            List<Video> videoResults = resp.getItems();
            //make items for displaying in listView
            for (int ind = 0; ind < searchResults.size(); ind++) {
                VideoItem item = new VideoItem();
                //searchList list info
                item.setTitle(searchResults.get(ind).getSnippet().getTitle());
                item.setThumbnailUrl(searchResults.get(ind).getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(searchResults.get(ind).getId().getVideoId());
                item.setVideoType(Config.YOUTUBE);
                //video info
                if (videoResults.get(ind) != null) {
                    if (videoResults.get(ind).getStatistics() != null) {
                        BigInteger viewsNumber = videoResults.get(ind).getStatistics().getViewCount();
                        String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                        item.setViewCount(viewsFormatted);
                    }
                    if (videoResults.get(ind).getContentDetails() != null) {
                        String isoTime = videoResults.get(ind).getContentDetails().getDuration();
                        String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                        item.setDuration(time);
                    }
                } else {
                    item.setDuration("");
                }

                //add to the list
                items.add(item);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("SMEDIC", "loadInBackground: return " + items.size());
        return items;
    }

    @Override
    public void deliverResult(List<VideoItem> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }
}
