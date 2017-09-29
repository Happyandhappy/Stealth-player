//package com.youtubebing.stealthplayer;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.PixelFormat;
//import android.os.IBinder;
//import android.util.Log;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.WindowManager;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
///**
// * Created by Naxtre on 19-Jul-17.
// */
//
//public class OverlayService extends Service {
//
//    private static final String TAG = OverlayService.class.getSimpleName();
//    WindowManager mWindowManager;
//    View mView;
//    Animation mAnimation;
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        registerOverlayReceiver();
//        return super.onStartCommand(intent, flags, startId);
//    }
//
//    private void showDialog(String aTitle) {
//        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
//
//        mView = View.inflate(getApplicationContext(), R.layout.fragment_overlay, null);
//        mView.setTag(TAG);
//
//        int top = getApplicationContext().getResources().getDisplayMetrics().heightPixels / 2;
//
//        RelativeLayout dialog = (RelativeLayout) mView.findViewById(R.id.dialog);
//        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dialog.getLayoutParams();
//        lp.topMargin = top;
//        lp.bottomMargin = top;
//        mView.setLayoutParams(lp);
//
//        TextView imageButton = (TextView) mView.findViewById(R.id.close);
//        lp = (RelativeLayout.LayoutParams) imageButton.getLayoutParams();
//        lp.topMargin = top - 58;
//        imageButton.setLayoutParams(lp);
//        imageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mView.setVisibility(View.INVISIBLE);
//            }
//        });
//
//        TextView title = (TextView) mView.findViewById(R.id.Title);
//        title.setText(aTitle);
//
//        final WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
//                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
//                PixelFormat.RGBA_8888);
//
//        mView.setVisibility(View.VISIBLE);
//        mAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.in);
//        mView.startAnimation(mAnimation);
//        mWindowManager.addView(mView, mLayoutParams);
//
//    }
//
//    private void hideDialog() {
//        if (mView != null && mWindowManager != null) {
//            mWindowManager.removeView(mView);
//            mView = null;
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        unregisterOverlayReceiver();
//        super.onDestroy();
//    }
//
//    private void registerOverlayReceiver() {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_SCREEN_ON);
//        filter.addAction(Intent.ACTION_SCREEN_OFF);
//        filter.addAction(Intent.ACTION_USER_PRESENT);
//        registerReceiver(overlayReceiver, filter);
//    }
//
//    private void unregisterOverlayReceiver() {
//        hideDialog();
//        unregisterReceiver(overlayReceiver);
//    }
//
//
//    private BroadcastReceiver overlayReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Log.d(TAG, "[onReceive]" + action);
//            if (action.equals(Intent.ACTION_SCREEN_ON)) {
//                showDialog("Unfortunately, Youtube's terms of use DONOT allow us to play videos when the screen is locked.");
//            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
//                hideDialog();
//            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
//                hideDialog();
//            }
//        }
//    };
//}