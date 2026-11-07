package com.cp470.groupproject.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.cp470.groupproject.util.ExpirationTiming;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Starts and schedules the expiration check. receives the {@link WorkManager} through the
 * constructor so tests can replace it with a mock.
 */
public class ExpirationScheduler {

    private static final String TAG = "ExpirationScheduler";

    /** unique name of the immediate check */
    public static final String RUN_NOW_WORK_NAME = "expiration_check_now";

    /** unique name of the daily check */
    public static final String UNIQUE_WORK_NAME = "expiration_check_daily";

    /** unique name of the short test schedule used by the developer tools */
    public static final String DEBUG_WORK_NAME = "expiration_check_debug";

    /** hour of the day (local time) at which the daily alert is due */
    public static final int NOTIFY_HOUR = 9;

    private final WorkManager workManager;

    /** @param workManager the work manager that will run the tasks */
    public ExpirationScheduler(@NonNull WorkManager workManager) {
        this.workManager = workManager;
    }

    /** @return a scheduler backed by the real work manager of this app */
    public static ExpirationScheduler from(@NonNull Context context) {
        return new ExpirationScheduler(WorkManager.getInstance(context));
    }

    /** runs one check right away (demos and manual tests). a pending one is replaced */
    public void runNow() {
        workManager.enqueueUniqueWork(RUN_NOW_WORK_NAME, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(ExpirationWorker.class).build());
    }

    /**
     * schedules the daily check at {@link #NOTIFY_HOUR}. it is called on every app start, so an
     * existing schedule is kept instead of being created again.
     *
     * @param nowMs current time in milliseconds
     * @param zone time zone in which {@link #NOTIFY_HOUR} is read
     */
    public void scheduleDaily(long nowMs, ZoneId zone) {
        long delay = ExpirationTiming.delayUntilNextHour(nowMs, NOTIFY_HOUR, zone);
        schedulePeriodic(UNIQUE_WORK_NAME, delay, 1, TimeUnit.DAYS,
                ExistingPeriodicWorkPolicy.KEEP);
    }

    /**
     * generic periodic schedule, also used by the developer tools.
     *
     * @param uniqueName name that identifies the work, only one work per name exists
     * @param initialDelayMs wait before the first run
     * @param interval repeat interval (android does not allow less than 15 minutes)
     * @param unit unit of {@code interval}
     * @param policy what to do if a work with that name already exists
     */
    public void schedulePeriodic(@NonNull String uniqueName, long initialDelayMs, long interval,
                                 @NonNull TimeUnit unit,
                                 @NonNull ExistingPeriodicWorkPolicy policy) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(ExpirationWorker.class, interval, unit)
                        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiresBatteryNotLow(true).build())
                        .build();
        workManager.enqueueUniquePeriodicWork(uniqueName, policy, request);
        Log.d(TAG, "scheduled '" + uniqueName + "' first run in " + initialDelayMs + " ms");
    }

    /** @param uniqueName name of the work to cancel */
    public void cancel(@NonNull String uniqueName) {
        workManager.cancelUniqueWork(uniqueName);
    }
}
