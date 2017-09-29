package com.youtubebing.stealthplayer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.youtubebing.stealthplayer.adapters.PlaylistsAdapter;
import com.youtubebing.stealthplayer.adapters.VideosAdapter;
import com.youtubebing.stealthplayer.database.YouTubeSqlDb;
import com.youtubebing.stealthplayer.interfaces.ItemEventsListener;
import com.youtubebing.stealthplayer.model.ItemType;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.utils.Config;
import com.youtubebing.stealthplayer.utils.NetworkConf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 7/28/2017.
 */

public class PlaylistActivity extends AppCompatActivity implements ItemEventsListener<VideoItem> , AdapterView.OnItemSelectedListener{

    private RecyclerView playlistListView;
    private PlaylistsAdapter playlistListAdapter;
    ArrayAdapter<String> dataAdapter;
    private List<VideoItem> playlistList;
    private AdView mAdView;
    private Intent intent;
    private Spinner spinner;
    private Button edit_btn, add_btn, remove_btn;

    private NetworkConf networkConf;

    private int init_pos;

    private static ProgressDialog pDialog;

    private List<String> playlists;

    private String m_Text = "";

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    public PlaylistActivity(){
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        mAdView = (AdView) findViewById(R.id.adView_detail);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        networkConf = new NetworkConf(this);


        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

        spinner = (Spinner)findViewById(R.id.playlist_spinner1);

            // Spinner click listener
        spinner.setOnItemSelectedListener(this);
        playlists = new ArrayList<String>();

        edit_btn = (Button)findViewById(R.id.edit_btn);
        add_btn = (Button)findViewById(R.id.add_btn);
        remove_btn = (Button)findViewById(R.id.remove_btn);

        edit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(playlists.size() > 0) {
                    final int pos = spinner.getSelectedItemPosition();

                    final String currentPlaylist = playlists.get(pos);

                    AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                    builder.setTitle("Edit playlist");

                    final EditText input = new EditText(PlaylistActivity.this);
                    builder.setView(input);
                    input.setText(currentPlaylist);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_Text = input.getText().toString();
                            if(!playlists.contains(m_Text))
                            playlists.set(pos, m_Text);
                            YouTubeSqlDb.getInstance().getSpinnerLists().update(currentPlaylist, m_Text);
                            YouTubeSqlDb.getInstance().getPlaylistVideos().updatePlaylist(currentPlaylist, m_Text);
                            dataAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                } else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                    builder.setTitle("There is no playlist");

                    builder.show();
                }
//
            }
        });

        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                builder.setTitle("Add playlist");

                final EditText input = new EditText(PlaylistActivity.this);
    //            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_Text = input.getText().toString();
                        if(!playlists.contains(m_Text))
                            playlists.add(m_Text);
                        YouTubeSqlDb.getInstance().getSpinnerLists().create(m_Text);
                        dataAdapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        remove_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(playlists.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                    builder.setTitle("Are you sure?");

                    //            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            YouTubeSqlDb.getInstance().getSpinnerLists().delete(spinner.getSelectedItem().toString());
                            YouTubeSqlDb.getInstance().getPlaylistVideos().deletePlaylist(spinner.getSelectedItem().toString());
                            playlists.remove(spinner.getSelectedItemPosition());
                            dataAdapter.notifyDataSetChanged();
                            playlistListAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();

                } else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                    builder.setTitle("There is no playlist");

                    builder.show();
                }
            }
        });



        playlists.addAll(YouTubeSqlDb.getInstance().getSpinnerLists().readAll());


        dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, playlists);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        playlistListView = (RecyclerView)findViewById(R.id.detail_search_list);
        playlistListView.setLayoutManager(new LinearLayoutManager(this));

        playlistList = new ArrayList<>();
        playlistListAdapter = new PlaylistsAdapter(this, playlistList);

        playlistListAdapter.setOnItemEventsListener(this);
        playlistListView.setAdapter(playlistListAdapter);

        pDialog = new ProgressDialog(PlaylistActivity.this);
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
        playlists.clear();
        playlists.addAll(YouTubeSqlDb.getInstance().getSpinnerLists().readAll());
        SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        boolean stayAwake = preferences.getBoolean("STAY_AWAKE", false);
        if(stayAwake){
            wl.acquire();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        boolean stayAwake = preferences.getBoolean("STAY_AWAKE", false);
        if(stayAwake) {
            wl.release();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if(playlists.size() > 0) {
            playlistList.clear();
            playlistList.addAll(YouTubeSqlDb.getInstance().getPlaylistVideos().readPlaylist(spinner.getSelectedItem().toString()));
            playlistListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity( intent );
    }

    @Override
    public void onShareClicked(String itemId) {

    }

    @Override
    public void onFavoriteClicked(VideoItem video, boolean isChecked) {

    }

    @Override
    public void onGarbageClicked(VideoItem video) {
        YouTubeSqlDb.getInstance().getPlaylistVideos().delete(video.getId());
        playlistList.remove(video);
        playlistListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(VideoItem video) {
        if (!networkConf.isNetworkAvailable()) {
        networkConf.createNetErrorDialog();
        return;
        }



        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);

        Handler messageHandler = new PlaylistActivity.MessageHandler();

        switch(video.getVideoType()){
            case Config.YOUTUBE:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.YOUTUBE_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) playlistList);
                serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, playlistList.indexOf(video));
                break;
            case Config.BING:
                serviceIntent.putExtra(Config.NET_TYPE, ItemType.BING_VIDEO);
                serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST, (ArrayList) playlistList);
                serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST_VIDEO_POS, playlistList.indexOf(video));
                break;
            default:
                break;
        }


        startService(serviceIntent);


    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        playlistList.clear();
        playlistList.addAll(YouTubeSqlDb.getInstance().getPlaylistVideos().readPlaylist(spinner.getItemAtPosition(position).toString()));
        playlistListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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