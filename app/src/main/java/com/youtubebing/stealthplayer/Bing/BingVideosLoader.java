package com.youtubebing.stealthplayer.Bing;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.utils.Config;
import com.youtubebing.stealthplayer.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 7/14/2017.
 */

public class BingVideosLoader extends AsyncTaskLoader<List<VideoItem>> {

    private String keywords;

    public BingVideosLoader(Context context, String keywords) {
        super(context);
        this.keywords = keywords;
    }

    @Override
    public List<VideoItem> loadInBackground() {

        ArrayList<VideoItem> items = new ArrayList<>();
        Integer result = 0;
        HttpURLConnection urlConnection;

        try {
            URL url = new URL(Config.BING_URL + keywords);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty (Config.KEY_NAME, Config.ACCOUNT_KEY);
            int statusCode = urlConnection.getResponseCode();

            // 200 represents HTTP OK
            if (statusCode == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    response.append(line);
                }
                String resultJson = response.toString();
                try {
                    JSONObject jsonObject = new JSONObject(resultJson);
                    JSONArray posts = jsonObject.optJSONArray("value");
                    items = new ArrayList<>();

                    for (int i = 0; i < posts.length(); i++) {
                        JSONObject post = posts.optJSONObject(i);
                        if(!(post.isNull("motionThumbnailUrl"))){

                            VideoItem item = new VideoItem();
                            item.setTitle(post.optString("name"));
                            item.setId(post.optString("motionThumbnailUrl"));

                            item.setThumbnailUrl(post.optString("thumbnailUrl"));
                            String isoTime = post.optString("duration");
                            String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                            item.setDuration(time);
                            item.setViewCount(post.optString("viewCount"));
                            item.setDescription((post.optString("description")));
                            item.setVideoType(Config.BING);
                            items.add(item);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                result = 1; // Successful
            } else {
                result = 0; //"Failed to fetch data!";
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
