package com.cp470.groupproject.work;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.notification.NotificationHelper;

/**
 * Background task that checks the inventory for expired items and shows the alert.
 * it only wires the pieces together; the decision logic is in {@link ExpirationChecker}.
 */

public class ExpirationWorker extends Worker {

    private static final String TAG = "ExpirationWorker";

    /** after this many failed runs the worker gives up until the next period */
    private static final int MAX_ATTEMPTS = 3;

    /** called by workmanager, not by our code */
    public ExpirationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** runs on a background thread, so the repository's sync methods can be used here */
    @NonNull
    @Override
    public Result doWork() {
        try {
            Application app = (Application) getApplicationContext();
            int reported = new ExpirationChecker(new PantryRepository(app),
                    new NotificationHelper(app)).run(System.currentTimeMillis());
            Log.d(TAG, "expiration check finished, items reported: " + reported);
            return Result.success();
        } catch (Exception e) {
            if (getRunAttemptCount() >= MAX_ATTEMPTS) {
                // a periodic work that fails is still scheduled for its next period
                Log.e(TAG, "giving up after " + getRunAttemptCount() + " attempts", e);
                return Result.failure();
            }
            Log.e(TAG, "expiration check failed, will retry", e);
            return Result.retry();
        }
    }
}
