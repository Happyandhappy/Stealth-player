package com.youtubebing.stealthplayer;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Created by smedic on 5.3.17..
 */

public class YTApplication extends Application {

    public static boolean POWER_BTN_CLICK_STOP;
    private static Context mContext;

    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return mContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
