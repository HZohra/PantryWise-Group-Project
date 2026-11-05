package com.cp470.groupproject;

import android.app.Application;
import android.util.Log;

import com.cp470.groupproject.notification.NotificationHelper;
import com.cp470.groupproject.work.ExpirationScheduler;

import java.time.ZoneId;

/**
 * Entry point of the app process: it runs before any activity. it exists so the daily expiration
 * check is scheduled without depending on a particular screen being opened.
 */
public class PantryWiseApp extends Application {

    private static final String TAG = "PantryWiseApp";

    @Override
    public void onCreate() {
        super.onCreate();
        new NotificationHelper(this).createChannel();
        ExpirationScheduler.from(this)
                .scheduleDaily(System.currentTimeMillis(), ZoneId.systemDefault());
        Log.d(TAG, "daily expiration check scheduled");
    }
}
