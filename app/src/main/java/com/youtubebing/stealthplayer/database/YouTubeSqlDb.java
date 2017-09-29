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
package com.youtubebing.stealthplayer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import com.youtubebing.stealthplayer.YTApplication;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.model.YouTubePlaylist;

import java.util.ArrayList;

/**
 * SQLite database for storing recentlyWatchedVideos and playlist
 * Created by Stevan Medic on 17.3.16..
 */
public class YouTubeSqlDb {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YouTubeDb.db";

    public static final String RECENTLY_WATCHED_TABLE_NAME = "recently_watched_videos";
    public static final String FAVORITES_TABLE_NAME = "favorites_videos";
    public static final String PLAYLIST_TABLE_NAME = "playlist_videos";
    public static final String PLAYLIST_SPINNER_NAME = "spinner_table";
    private static final String TAG = "SMEDIC TABLE SQL";

    public enum VIDEOS_TYPE {FAVORITE, RECENTLY_WATCHED, PLAYLIST}

    private YouTubeDbHelper dbHelper;

    // private Playlists playlists;
    private Videos recentlyWatchedVideos;
    private Videos favoriteVideos;
    private PlaylistVideos playlistVideos;
    private Playlist_Spinner spinnerLists;

    private static YouTubeSqlDb ourInstance = new YouTubeSqlDb();

    public static YouTubeSqlDb getInstance() {
        return ourInstance;
    }

    private YouTubeSqlDb() {
    }

    public void init(Context context) {
        dbHelper = new YouTubeDbHelper(context);
        dbHelper.getWritableDatabase();

        //       playlists = new Playlists();
        recentlyWatchedVideos = new Videos(RECENTLY_WATCHED_TABLE_NAME);
        favoriteVideos = new Videos(FAVORITES_TABLE_NAME);
        playlistVideos = new PlaylistVideos(PLAYLIST_TABLE_NAME);
        spinnerLists = new Playlist_Spinner(PLAYLIST_SPINNER_NAME);
    }

    public Videos videos(VIDEOS_TYPE type) {

        if (type == VIDEOS_TYPE.FAVORITE) {
            return favoriteVideos;
        } else if (type == VIDEOS_TYPE.RECENTLY_WATCHED) {
            return recentlyWatchedVideos;
        }

        Log.e(TAG, "Error. Unknown video type!");
        return null;
    }

    public PlaylistVideos getPlaylistVideos(){
        return playlistVideos;
    }

    public Playlist_Spinner getSpinnerLists() {
        return spinnerLists;
    }

    /*
        public Playlists playlists() {
            return playlists;
        }
    */
    private final class YouTubeDbHelper extends SQLiteOpenHelper {
        public YouTubeDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //       db.execSQL(YouTubeVideoEntry.DATABASE_FAVORITES_TABLE_CREATE);
            db.execSQL(YouTubeVideoEntry.DATABASE_RECENTLY_WATCHED_TABLE_CREATE);
            db.execSQL(YouTubeVideoEntry.DATABASE_PLAYLIST_TABLE_CREATE);
            db.execSQL(YouTubeVideoEntry.DATABASE_SPINNER_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(YouTubeVideoEntry.DROP_QUERY_RECENTLY_WATCHED);
            db.execSQL(YouTubeVideoEntry.DROP_QUERY_PLAYLIST);
            db.execSQL(YouTubeVideoEntry.DROP_QUERY_SPINNER);
            //       db.execSQL(YouTubePlaylistEntry.DROP_QUERY);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    /**
     * Class that enables basic CRUD operations on Playlists database table
     */
    public class Videos {

        private String tableName;

        private Videos(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Creates video entry in videos table
         *
         * @param video
         * @return
         */
        public boolean create(VideoItem video) {

            if(checkIfExists(video.getId())){
                return false;
            }
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(YouTubeVideoEntry.COLUMN_VIDEO_ID, video.getId());
            values.put(YouTubeVideoEntry.COLUMN_TITLE, video.getTitle());
            values.put(YouTubeVideoEntry.COLUMN_DURATION, video.getDuration());
            values.put(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL, video.getThumbnailUrl());
            values.put(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER, video.getViewCount());
            values.put(YouTubeVideoEntry.COLUMN_TYPE, video.getVideoType());
            return db.insert(tableName, YouTubeVideoEntry.COLUMN_NAME_NULLABLE, values) > 0;
        }

        /**
         * Checks if entry is already present in database
         * @param videoId
         * @return
         */
        public boolean checkIfExists(String videoId) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String Query = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'";
            Cursor cursor = db.rawQuery(Query, null);
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
            cursor.close();
            return true;
        }

        /**
         * Reads all recentlyWatchedVideos from playlists database
         *
         * @return
         */
        public ArrayList<VideoItem> readAll() {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_ENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            ArrayList<VideoItem> list = new ArrayList<>();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String videoId = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIDEO_ID));
                String title = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TITLE));
                String duration = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_DURATION));
                String thumbnailUrl = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL));
                String viewsNumber = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER));
                String type = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TYPE));
                list.add(new VideoItem(videoId, title, thumbnailUrl, duration, viewsNumber, "", type));
            }
            c.close();
            return list;
        }



        /**
         * Deletes video entry with provided ID
         *
         * @param videoId
         * @return
         */
        public boolean delete(String videoId) {
            return dbHelper.getWritableDatabase().delete(tableName,
                    YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'", null) > 0;
        }

        /**
         * Deletes all entries from database
         *
         * @return
         */
        public boolean deleteAll() {
            return dbHelper.getWritableDatabase().delete(tableName, "1", null) > 0;
        }
    }

    public class PlaylistVideos {

        private String tableName;

        private PlaylistVideos(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Creates video entry in videos table
         *
         * @param video
         * @return
         */
        public boolean create(VideoItem video, String playlistId) {

            if(checkIfExists(video.getId())){
                Toast.makeText(YTApplication.getAppContext(), "Already exist", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(YouTubeVideoEntry.COLUMN_VIDEO_ID, video.getId());
            values.put(YouTubeVideoEntry.COLUMN_TITLE, video.getTitle());
            values.put(YouTubeVideoEntry.COLUMN_DURATION, video.getDuration());
            values.put(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL, video.getThumbnailUrl());
            values.put(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER, video.getViewCount());
            values.put(YouTubeVideoEntry.COLUMN_PLAYLIST_ID, playlistId);
            values.put(YouTubeVideoEntry.COLUMN_TYPE, video.getVideoType());
            return db.insert(tableName, YouTubeVideoEntry.COLUMN_NAME_NULLABLE, values) > 0;
        }

        /**
         * Checks if entry is already present in database
         * @param videoId
         * @return
         */
        public boolean checkIfExists(String videoId) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String Query = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'";
            Cursor cursor = db.rawQuery(Query, null);
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
            cursor.close();
            return true;
        }

        /**
         * Reads all recentlyWatchedVideos from playlists database
         *
         * @return
         */
        public ArrayList<VideoItem> readAll() {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_ENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            ArrayList<VideoItem> list = new ArrayList<>();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String videoId = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIDEO_ID));
                String title = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TITLE));
                String duration = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_DURATION));
                String thumbnailUrl = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL));
                String viewsNumber = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER));
                String type = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TYPE));
                list.add(new VideoItem(videoId, title, thumbnailUrl, duration, viewsNumber, "", type));
            }
            c.close();
            return list;
        }

        public ArrayList<VideoItem> readPlaylist(String id) {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_PLAYLIST_ID + "='" + id + "'" + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_ENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            ArrayList<VideoItem> list = new ArrayList<>();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String videoId = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIDEO_ID));
                String title = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TITLE));
                String duration = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_DURATION));
                String thumbnailUrl = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_THUMBNAIL_URL));
                String viewsNumber = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_VIEWS_NUMBER));
                String type = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_TYPE));
                list.add(new VideoItem(videoId, title, thumbnailUrl, duration, viewsNumber, "", type));
            }
            c.close();
            return list;
        }

        public boolean updatePlaylist(String oldList, String newList) {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_PLAYLIST_ID + "='" + oldList + "'" + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_ENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                int rowId = c.getInt(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_ENTRY_ID));
                ContentValues args = new ContentValues();
                SQLiteDatabase db1 = dbHelper.getWritableDatabase();

                args.put(YouTubeVideoEntry.COLUMN_PLAYLIST_ID, newList);
                if(db1.update(tableName, args, YouTubeVideoEntry.COLUMN_ENTRY_ID + "=" + rowId, null) <= 0)
                    return false;
            }
            c.close();


            return true;
        }
        /**
         * Deletes video entry with provided ID
         *
         * @param videoId
         * @return
         */
        public boolean delete(String videoId) {
            return dbHelper.getWritableDatabase().delete(tableName,
                    YouTubeVideoEntry.COLUMN_VIDEO_ID + "='" + videoId + "'", null) > 0;
        }

        public boolean deletePlaylist(String listname){
            return dbHelper.getWritableDatabase().delete(tableName,
                    YouTubeVideoEntry.COLUMN_PLAYLIST_ID + "='" + listname + "'", null) > 0;
        }
        /**
         * Deletes all entries from database
         *
         * @return
         */
        public boolean deleteAll() {
            return dbHelper.getWritableDatabase().delete(tableName, "1", null) > 0;
        }
    }

    /**
     * Inner class that defines Videos table entry
     */
    public static abstract class YouTubeVideoEntry implements BaseColumns {
        public static final String COLUMN_ENTRY_ID = "_id";
        public static final String COLUMN_VIDEO_ID = "video_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DURATION = "duration";
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";
        public static final String COLUMN_VIEWS_NUMBER = "views_number";
        public static final String COLUMN_PLAYLIST_ID = "playlist_id";
        public static final String COLUMN_TYPE = "type";

        public static final String COLUMN_PLAYLISTENTRY_ID = "_id";
        public static final String COLUMN_PL_ID = "playlist_id";

        public static final String COLUMN_NAME_NULLABLE = "null";

        private static final String DATABASE_SPINNER_TABLE_CREATE =
                "CREATE TABLE " + PLAYLIST_SPINNER_NAME + "(" +
                        COLUMN_PLAYLISTENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_PL_ID + " TEXT NOT NULL UNIQUE)";



        private static final String DATABASE_RECENTLY_WATCHED_TABLE_CREATE =
                "CREATE TABLE " + RECENTLY_WATCHED_TABLE_NAME + "(" +
                        COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_VIDEO_ID + " TEXT NOT NULL UNIQUE," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_DURATION + " TEXT," +
                        COLUMN_THUMBNAIL_URL + " TEXT," +
                        COLUMN_VIEWS_NUMBER + " TEXT," +
                        COLUMN_TYPE + " TEXT)";
        private static final String DATABASE_PLAYLIST_TABLE_CREATE =
                "CREATE TABLE " + PLAYLIST_TABLE_NAME + "(" +
                        COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_VIDEO_ID + " TEXT NOT NULL UNIQUE," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_DURATION + " TEXT," +
                        COLUMN_THUMBNAIL_URL + " TEXT," +
                        COLUMN_VIEWS_NUMBER + " TEXT," +
                        COLUMN_PLAYLIST_ID + " TEXT," +
                        COLUMN_TYPE + " TEXT)";

              private static final String DATABASE_FAVORITES_TABLE_CREATE =
                      "CREATE TABLE " + FAVORITES_TABLE_NAME + "(" +
                              COLUMN_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                              COLUMN_VIDEO_ID + " TEXT NOT NULL UNIQUE," +
                              COLUMN_TITLE + " TEXT NOT NULL," +
                              COLUMN_DURATION + " TEXT," +
                              COLUMN_THUMBNAIL_URL + " TEXT," +
                              COLUMN_VIEWS_NUMBER + " TEXT)";

        public static final String DROP_QUERY_RECENTLY_WATCHED = "DROP TABLE " + RECENTLY_WATCHED_TABLE_NAME;
        public static final String DROP_QUERY_PLAYLIST = "DROP TABLE " + PLAYLIST_TABLE_NAME;
        public static final String DROP_QUERY_SPINNER = "DROP TABLE " + PLAYLIST_SPINNER_NAME;
    }

    public class Playlist_Spinner {

        private String tableName;

        private Playlist_Spinner(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Creates video entry in videos table
         *
         * @param spinnerList
         * @return
         */
        public boolean create(String spinnerList) {

            if(checkIfExists(spinnerList)){
                Toast.makeText(YTApplication.getAppContext(), "Already exist", Toast.LENGTH_SHORT).show();
                return false;
            }
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(YouTubeVideoEntry.COLUMN_PL_ID, spinnerList);
            return db.insert(tableName, YouTubeVideoEntry.COLUMN_NAME_NULLABLE, values) > 0;
        }

        /**
         * Checks if entry is already present in database
         * @param spinnerList
         * @return
         */
        public boolean checkIfExists(String spinnerList) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String Query = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_PL_ID + "='" + spinnerList + "'";
            Cursor cursor = db.rawQuery(Query, null);
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
            cursor.close();
            return true;
        }

        /**
         * Reads all recentlyWatchedVideos from playlists database
         *
         * @return
         */
        public ArrayList<String> readAll() {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_PL_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            ArrayList<String> list = new ArrayList<>();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                String type = c.getString(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_PL_ID));
                list.add(type);
            }
            c.close();
            return list;
        }



        /**
         * Deletes video entry with provided ID
         *
         * @param
         * @return
         */
        public boolean update(String oldList, String newList) {

            final String SELECT_QUERY_ORDER_DESC = "SELECT * FROM " + tableName + " WHERE " + YouTubeVideoEntry.COLUMN_PL_ID + "='" + oldList + "'" + " ORDER BY "
                    + YouTubeVideoEntry.COLUMN_PLAYLISTENTRY_ID + " DESC";

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor c = db.rawQuery(SELECT_QUERY_ORDER_DESC, null);
            while (c.moveToNext()) {
                int rowId = c.getInt(c.getColumnIndexOrThrow(YouTubeVideoEntry.COLUMN_PLAYLISTENTRY_ID));
                ContentValues args = new ContentValues();
                SQLiteDatabase db1 = dbHelper.getWritableDatabase();

                args.put(YouTubeVideoEntry.COLUMN_PL_ID, newList);
                if(db1.update(tableName, args, YouTubeVideoEntry.COLUMN_PLAYLISTENTRY_ID + "=" + rowId, null) <= 0)
                    return false;
            }
            c.close();


            return true;
        }

        public boolean delete(String videoId) {
            return dbHelper.getWritableDatabase().delete(tableName,
                    YouTubeVideoEntry.COLUMN_PL_ID + "='" + videoId + "'", null) > 0;
        }

        /**
         * Deletes all entries from database
         *
         * @return
         */
        public boolean deleteAll() {
            return dbHelper.getWritableDatabase().delete(tableName, "1", null) > 0;
        }
    }


}
