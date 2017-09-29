package com.youtubebing.stealthplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.youtubebing.stealthplayer.utils.Config;

/**
 * Created by Naxtre on 19-Jul-17.
 */

public class UserPresentBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String screenOff = null;

        SharedPreferences preferences = context.getSharedPreferences("preferences", Context.MODE_MULTI_PROCESS);
        boolean powerStop = preferences.getBoolean("POWER_BTN_CLICK_STOP", false);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF) && powerStop) {
            screenOff = "OFF";
        } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            screenOff = "ON";
        }

        Intent intent1 = new Intent("com.stealth.player");
        intent1.putExtra("screen_state", screenOff);
        intent1.putExtra("power_stop", powerStop);
        context.sendBroadcast(intent1);
    }

}
