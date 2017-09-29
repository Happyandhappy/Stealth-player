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

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.youtubebing.stealthplayer.database.YouTubeSqlDb;
import com.youtubebing.stealthplayer.fragments.BingSearchFragment;
import com.youtubebing.stealthplayer.fragments.YouTubeSearchFragment;
import com.youtubebing.stealthplayer.interfaces.OnFavoritesSelected;
import com.youtubebing.stealthplayer.interfaces.OnItemSelected;
import com.youtubebing.stealthplayer.model.ItemType;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.playlist.PlaylistSettingModal;
import com.youtubebing.stealthplayer.utils.Config;
import com.youtubebing.stealthplayer.utils.NetworkConf;
import com.youtubebing.stealthplayer.youtube.SuggestionsLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.youtubebing.stealthplayer.R.layout.suggestions;
import static com.youtubebing.stealthplayer.youtube.YouTubeSingleton.getCredential;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, OnFavoritesSelected, TabLayout.OnTabSelectedListener, PlaylistSettingModal.PlaylistDialogListener{

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final int PERMISSIONS = 1;
    private static final String PREF_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    private static final String PREF_TEXT_COLOR = "TEXT_COLOR";
    public static final String PREF_ACCOUNT_NAME = "accountName";

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private int initialColor = 0xffff0040;
    private int initialColors[] = new int[2];

    private YouTubeSearchFragment youtubeSearchFragment;
    private BingSearchFragment bingSearchFragment;

    private String m_query;

    private AdView mAdView;

    private static ProgressDialog pDialog;

    private Intent intent_resume;

    private PowerManager pm;
    private PowerManager.WakeLock wl;

    private int[] tabIcons = {
            R.drawable.bing,
            R.drawable.you,

    };

    private NetworkConf networkConf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");


        Intent i0 = new Intent(this,AEScreenOnOffService.class);
        startService(i0);
        YouTubeSqlDb.getInstance().init(this);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(1);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(this);

        networkConf = new NetworkConf(this);

        pDialog = new ProgressDialog(MainActivity.this);
        // Set progressbar title
        pDialog.setTitle("Loading Stream Video");
        // Set progressbar message
        pDialog.setMessage("buffering...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);

        setupTabIcons();
        loadColor();


        SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        boolean stayAwake = preferences.getBoolean("STAY_AWAKE", false);
        if(stayAwake){
            wl.acquire();
        } else{
            if(wl.isHeld())
                wl.release();
        }
      //  bingSearchFragment.searchQuery(m_query);

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        Config.POWER_BTN_CLICK_STOP = preferences.getBoolean("POWER_BTN_CLICK_STOP", false);


    }

    @Override
    protected void onStart() {
        super.onStart();
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

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */


    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            m_query = query;


      //      viewPager.setCurrentItem(1, true); //switch to search fragment
            if(viewPager.getCurrentItem() == 1) {
                if (youtubeSearchFragment != null) {
                    youtubeSearchFragment.searchQuery(query);
                }
            }else{
                if (bingSearchFragment != null) {
                    bingSearchFragment.searchQuery(query);
                }
            }
        }
    }

    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(0).setCustomView(R.layout.bing_tab);
        tabLayout.getTabAt(1).setCustomView(R.layout.youtube_tab);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        youtubeSearchFragment = YouTubeSearchFragment.newInstance();
        bingSearchFragment = BingSearchFragment.newInstance();


        adapter.addFragment(bingSearchFragment, null);
        adapter.addFragment(youtubeSearchFragment, null);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied: ");
    }

    @Override
    public void onVideoSelected(VideoItem video) {

    }

    @Override
    public void onYouTubePlaylistSelected(List<VideoItem> playlist, int position) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        Handler messageHandler = new MessageHandler();

        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.NET_TYPE, ItemType.YOUTUBE_VIDEO);
        serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) playlist);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, position);
        startService(serviceIntent);
    }

    @Override
    public void onBingPlaylistSelected(List<VideoItem> playlist, int position) {

        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        Handler messageHandler = new MessageHandler();

        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.NET_TYPE, ItemType.BING_VIDEO);
        serviceIntent.putExtra("MESSENGER", new Messenger(messageHandler));
        serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST, (ArrayList) playlist);
        serviceIntent.putExtra(Config.BING_TYPE_PLAYLIST_VIDEO_POS, position);
        startService(serviceIntent);
    }

    @Override
    public void onFavoritesSelected(final VideoItem video, boolean isChecked) {


        List<String> categories = new ArrayList<String>();
        categories.addAll(YouTubeSqlDb.getInstance().getSpinnerLists().readAll());

        if(categories.size() <= 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Add playlist");

            final EditText input = new EditText(MainActivity.this);
            //            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String m_Text = input.getText().toString();
                    YouTubeSqlDb.getInstance().getSpinnerLists().create(m_Text);
                    android.app.FragmentManager manager = getFragmentManager();
                    android.app.Fragment frag = manager.findFragmentByTag("fragment_edit_name");
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    PlaylistSettingModal editNameDialog = new PlaylistSettingModal();
                    editNameDialog.show(manager, "fragment_edit_name");
                    editNameDialog.setVideoitem(video);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else {

            android.app.FragmentManager manager = getFragmentManager();
            android.app.Fragment frag = manager.findFragmentByTag("fragment_edit_name");
            if (frag != null) {
                manager.beginTransaction().remove(frag).commit();
            }
            PlaylistSettingModal editNameDialog = new PlaylistSettingModal();
            editNameDialog.show(manager, "fragment_edit_name");
            editNameDialog.setVideoitem(video);
        }

 }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if(m_query == null)return;
        if(tab.getPosition() == 1) {
            if (youtubeSearchFragment != null) {
                youtubeSearchFragment.searchQuery(m_query);
            }
        }else{
            if (bingSearchFragment != null) {
                bingSearchFragment.searchQuery(m_query);
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        YouTubeSqlDb.getInstance().getPlaylistVideos().create(((PlaylistSettingModal)dialog).getVideoitem(), ((PlaylistSettingModal)dialog).getPlaylistName());
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }


    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }

    /**
     * Options menu in action bar
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        MenuItem stopItem = menu.findItem(R.id.action_stop_stream);
        stopItem.setChecked(Config.POWER_BTN_CLICK_STOP);

        SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        boolean stayAwake = preferences.getBoolean("STAY_AWAKE", false);
        MenuItem stayAwakeItem = menu.findItem(R.id.action_stay_awake);
        stayAwakeItem.setChecked(stayAwake);
        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
                suggestions,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        searchView.setSuggestionsAdapter(suggestionAdapter);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.setQuery(suggestions.get(position), false);
                searchView.clearFocus();

                Intent suggestionIntent = new Intent(Intent.ACTION_SEARCH);
                suggestionIntent.putExtra(SearchManager.QUERY, suggestions.get(position));
                handleIntent(suggestionIntent);

                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false; //if true, no new intent is started
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                // check network connection. If not available, do not query.
                // this also disables onSuggestionClick triggering
                if (query.length() > 2) { //make suggestions after 3rd letter
                    if (networkConf.isNetworkAvailable()) {

                        getSupportLoaderManager().restartLoader(4, null, new LoaderManager.LoaderCallbacks<List<String>>() {
                            @Override
                            public Loader<List<String>> onCreateLoader(final int id, final Bundle args) {
                                return new SuggestionsLoader(getApplicationContext(), query);
                            }

                            @Override
                            public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
                                if (data == null)
                                    return;
                                suggestions.clear();
                                suggestions.addAll(data);
                                String[] columns = {
                                        BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1
                                };
                                MatrixCursor cursor = new MatrixCursor(columns);

                                for (int i = 0; i < data.size(); i++) {
                                    String[] tmp = {Integer.toString(i), data.get(i)};
                                    cursor.addRow(tmp);
                                }
                                suggestionAdapter.swapCursor(cursor);
                            }

                            @Override
                            public void onLoaderReset(Loader<List<String>> loader) {
                                suggestions.clear();
                                suggestions.addAll(Collections.<String>emptyList());
                            }
                        }).forceLoad();
                        return true;
                    }
                }
                return false;
            }
        });

        return true;
    }

    /**
     * Handles selected item from action bar
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, PlaylistActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_clear_list) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        } else if (id == R.id.action_color_picker) {
            /* Show color picker dialog */
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle(getString(R.string.choose_colors))
                    .initialColor(initialColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .setPickerCount(2)
                    .initialColors(initialColors)
                    .density(12)
                    .setOnColorSelectedListener(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int selectedColor) {
                        }
                    })
                    .setPositiveButton(getString(R.string.ok), new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            //changeBackgroundColor(selectedColor);
                            if (allColors != null) {
                                setColors(allColors[0], allColors[1]);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .showColorEdit(true)
                    .build()
                    .show();
        } else if(id == R.id.action_stop_stream){
            if(item.isChecked()){
                item.setChecked(false);
                SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("POWER_BTN_CLICK_STOP",false);
                editor.apply();
                Config.POWER_BTN_CLICK_STOP = false;
            } else{
                item.setChecked(true);
                SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("POWER_BTN_CLICK_STOP",true);
                editor.apply();
                Config.POWER_BTN_CLICK_STOP = true;
            }


        } else if(id == R.id.action_stay_awake){



            if(item.isChecked()){
                item.setChecked(false);
                SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("STAY_AWAKE",false);
                editor.apply();
                wl.release();

            } else{
                item.setChecked(true);
                SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("STAY_AWAKE",true);
                editor.apply();
                wl.acquire();
            }


        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads app theme color saved in preferences
     */
    private void loadColor() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int backgroundColor = sp.getInt(PREF_BACKGROUND_COLOR, -1);
        int textColor = sp.getInt(PREF_TEXT_COLOR, -1);

        if (backgroundColor != -1 && textColor != -1) {
            setColors(backgroundColor, textColor);
        } else {
            initialColors = new int[]{
                    ContextCompat.getColor(this, R.color.colorPrimary),
                    ContextCompat.getColor(this, R.color.textColorPrimary)};
        }
    }

    /**
     * Save app theme color in preferences
     */
    private void setColors(int backgroundColor, int textColor) {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setTitleTextColor(textColor);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setBackgroundColor(backgroundColor);
        tabs.setTabTextColors(textColor, textColor);
        setStatusBarColor(backgroundColor);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putInt(PREF_BACKGROUND_COLOR, backgroundColor).apply();
        sp.edit().putInt(PREF_TEXT_COLOR, textColor).apply();

        initialColors[0] = backgroundColor;
        initialColors[1] = textColor;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
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