package com.youtubebing.stealthplayer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.squareup.picasso.Picasso;
import com.youtubebing.stealthplayer.adapters.Detail2Adapter;
import com.youtubebing.stealthplayer.adapters.DetailAdapter;
import com.youtubebing.stealthplayer.database.YouTubeSqlDb;
import com.youtubebing.stealthplayer.interfaces.DetailItemEventListener;
import com.youtubebing.stealthplayer.model.ItemType;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.utils.Config;
import com.youtubebing.stealthplayer.utils.NetworkConf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 7/31/2017.
 */

public class DetailActivity extends AppCompatActivity implements DetailItemEventListener<VideoItem> {

    private ImageView currentThumbnailView;
    private TextView currentTitleText;
    private TextView currentDescriptionText;
    private RecyclerView searchListView;
    private RecyclerView relatedListView;
    private DetailAdapter searchListAdapter;
    private Detail2Adapter relatedListAdapter;
    private List<VideoItem> searchList;
    private List<VideoItem> relatedList;
    private AdView mAdView;
    private Intent intent;

    private static ProgressDialog pDialog;

    private NetworkConf networkConf;

    private int init_pos;


    public DetailActivity(){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mAdView = (AdView) findViewById(R.id.adView_detail);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        networkConf = new NetworkConf(this);

        YouTubeSqlDb.getInstance().init(this);

        currentThumbnailView = (ImageView)findViewById(R.id.detail_video_thumbnail);
        currentTitleText = (TextView)findViewById(R.id.detail_title);
        currentDescriptionText = (TextView)findViewById(R.id.detail_description);
        searchListView = (RecyclerView)findViewById(R.id.detail_search_list);
        searchListView.setLayoutManager(new LinearLayoutManager(this));
        relatedListView = (RecyclerView)findViewById(R.id.detail_relatec_list);
        relatedListView.setLayoutManager(new LinearLayoutManager(this));

        searchList = new ArrayList<>();
        relatedList = new ArrayList<>();
        intent = getIntent();
        init_pos = intent.getIntExtra(Config.DETAIL_PLAYLIST_VIDEO_POS, 0);
        searchList = (ArrayList<VideoItem>) intent.getSerializableExtra(Config.DETAIL_PLAYLIST);
        relatedList.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll());
        searchListAdapter = new DetailAdapter(this, searchList);
        relatedListAdapter = new Detail2Adapter(this, relatedList);


        searchListAdapter.setOnItemEventsListener(this);
        relatedListAdapter.setOnItemEventsListener(this);

        VideoItem selectedItem = searchList.get(init_pos);

        currentTitleText.setText(selectedItem.getTitle());
        currentDescriptionText.setText(selectedItem.getDescription());
        if (!TextUtils.isEmpty(selectedItem.getThumbnailUrl())) {
            Picasso.with(this).load(selectedItem.getThumbnailUrl())
                    .error(R.drawable.ic_action_playlist)
                    .placeholder(R.drawable.ic_action_playlist)
                    .into(currentThumbnailView);
        }

        int count = searchListAdapter.getItemCount();
        searchListView.setAdapter(searchListAdapter);
        relatedListView.setAdapter(relatedListAdapter);

        pDialog = new ProgressDialog(DetailActivity.this);
        // Set progressbar title
        pDialog.setTitle("Loading Stream Video");
        // Set progressbar message
        pDialog.setMessage("buffering...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);

        setVisible(false);

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sp = getSharedPreferences("OURINFO", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sp = getSharedPreferences("OURINFO", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        relatedListAdapter.notifyDataSetChanged();
        searchListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity( intent );
    }



    @Override
    public void onItemClick1(VideoItem videoItem) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        Handler messageHandler = new DetailActivity.MessageHandler();
        switch(videoItem.getVideoType()){
            case Config.YOUTUBE:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.YOUTUBE_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) searchList);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, searchList.indexOf(videoItem));
                break;
            case Config.BING:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.BING_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST, (ArrayList) searchList);
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST_VIDEO_POS, searchList.indexOf(videoItem));
                break;
            default:
                break;
        }


        startService(serviceIntent);

        currentTitleText.setText(videoItem.getTitle());
        currentDescriptionText.setText(videoItem.getDescription());
        if (!TextUtils.isEmpty(videoItem.getThumbnailUrl())) {

            Picasso.with(this).load(videoItem.getThumbnailUrl())
                    .error(R.drawable.ic_action_playlist)
                    .placeholder(R.drawable.ic_action_playlist)
                    .into(currentThumbnailView);

        }
    }

    @Override
    public void onItemClick2(VideoItem videoItem) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        Handler messageHandler = new DetailActivity.MessageHandler();
        switch(videoItem.getVideoType()){
            case Config.YOUTUBE:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.YOUTUBE_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) relatedList);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, relatedList.indexOf(videoItem));
                break;
            case Config.BING:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.BING_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST, (ArrayList) relatedList);
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST_VIDEO_POS, relatedList.indexOf(videoItem));
                break;
            default:
                break;
        }


        startService(serviceIntent);

        currentTitleText.setText(videoItem.getTitle());
        currentDescriptionText.setText(videoItem.getDescription());
        if (!TextUtils.isEmpty(videoItem.getThumbnailUrl())) {

            Picasso.with(this).load(videoItem.getThumbnailUrl())
                    .error(R.drawable.ic_action_playlist)
                    .placeholder(R.drawable.ic_action_playlist)
                    .into(currentThumbnailView);

        }
    }

    public static class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case 0:
                    pDialog.show();
                    break;
                case 1:
                    pDialog.dismiss();
                    break;
            }
        }
    }
}
