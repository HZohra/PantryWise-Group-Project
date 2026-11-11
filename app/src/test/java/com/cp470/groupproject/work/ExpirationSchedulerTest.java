package com.cp470.groupproject.work;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Tests that the scheduler asks WorkManager for the right work. the {@link WorkManager} is a
 * mockito mock, so nothing is really scheduled. robolectric is only needed because building a
 * work request touches android classes (it cannot be combined with MockitoJUnitRunner).
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ExpirationSchedulerTest {

    private WorkManager workManager;
    private ExpirationScheduler scheduler;

    @Before
    public void setUp() {
        workManager = mock(WorkManager.class);
        scheduler = new ExpirationScheduler(workManager);
    }

    @Test
    public void scheduleDaily_enqueuesUniquePeriodicWorkWithKeepPolicy() {
        long now = ZonedDateTime.of(2026, 11, 4, 8, 0, 0, 0, ZoneOffset.UTC)
                .toInstant().toEpochMilli();

        scheduler.scheduleDaily(now, ZoneOffset.UTC);

        ArgumentCaptor<PeriodicWorkRequest> captor =
                ArgumentCaptor.forClass(PeriodicWorkRequest.class);
        verify(workManager).enqueueUniquePeriodicWork(
                eq(ExpirationScheduler.UNIQUE_WORK_NAME),
                eq(ExistingPeriodicWorkPolicy.KEEP),
                captor.capture());
        assertEquals(ExpirationWorker.class.getName(),
                captor.getValue().getWorkSpec().workerClassName);
        assertEquals(TimeUnit.DAYS.toMillis(1),
                captor.getValue().getWorkSpec().intervalDuration);
        // 08:00 -> 09:00
        assertEquals(TimeUnit.HOURS.toMillis(1),
                captor.getValue().getWorkSpec().initialDelay);
    }

    @Test
    public void schedulePeriodic_usesGivenPolicyAndName() {
        scheduler.schedulePeriodic(ExpirationScheduler.DEBUG_WORK_NAME, 60_000, 15,
                TimeUnit.MINUTES, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE);

        verify(workManager).enqueueUniquePeriodicWork(
                eq(ExpirationScheduler.DEBUG_WORK_NAME),
                eq(ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE),
                any(PeriodicWorkRequest.class));
    }

    @Test
    public void cancel_cancelsUniqueWorkByName() {
        scheduler.cancel(ExpirationScheduler.UNIQUE_WORK_NAME);

        verify(workManager).cancelUniqueWork("expiration_check_daily");
    }

    @Test
    public void runNow_enqueuesOneTimeWorkWithReplace() {
        scheduler.runNow();

        verify(workManager).enqueueUniqueWork(
                eq("expiration_check_now"),
                eq(ExistingWorkPolicy.REPLACE),
                any(OneTimeWorkRequest.class));
    }
}
