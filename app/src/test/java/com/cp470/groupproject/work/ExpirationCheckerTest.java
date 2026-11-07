package com.cp470.groupproject.work;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.notification.NotificationHelper;
import com.cp470.groupproject.util.ExpiryUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests the alert decision logic without android: the repository and the notification helper are
 * mockito mocks, so no database or notification system is involved.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpirationCheckerTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = ExpiryUtils.DAY_MS;

    @Mock
    PantryRepository repo;

    @Mock
    NotificationHelper notifier;

    private static FoodItem item(String name, long expirationDate) {
        return new FoodItem(name, 1, "pcs", 1, 1, 0, expirationDate);
    }

    @Test
    public void run_expiredAndSoon_notifiesWithRightPartition() {
        FoodItem expired = item("Milk", NOW - 2 * DAY);
        FoodItem soon = item("Yogurt", NOW + DAY);
        FoodItem fresh = item("Rice", NOW + 90 * DAY);
        when(repo.getActiveItemsSync()).thenReturn(Arrays.asList(expired, soon, fresh));

        int reported = new ExpirationChecker(repo, notifier).run(NOW);

        assertEquals(2, reported);
        verify(notifier).showExpirationNotification(
                Collections.singletonList(expired), Collections.singletonList(soon), NOW);
    }

    @Test
    public void run_onlyFreshOrNoDate_doesNotNotify() {
        when(repo.getActiveItemsSync()).thenReturn(Arrays.asList(
                item("Rice", NOW + 90 * DAY), item("Tea", 0)));

        int reported = new ExpirationChecker(repo, notifier).run(NOW);

        assertEquals(0, reported);
        verify(notifier, never()).showExpirationNotification(any(), any(), anyLong());
    }

    @Test
    public void run_emptyInventory_doesNotNotify() {
        when(repo.getActiveItemsSync()).thenReturn(Collections.emptyList());

        int reported = new ExpirationChecker(repo, notifier).run(NOW);

        assertEquals(0, reported);
        verify(notifier, never()).showExpirationNotification(any(), any(), anyLong());
    }

    @Test
    public void run_exactlyAtWindowLimit_isIncluded() {
        FoodItem atLimit = item("Chicken", NOW + ExpiryUtils.DEFAULT_USE_SOON_DAYS * DAY);
        when(repo.getActiveItemsSync()).thenReturn(Collections.singletonList(atLimit));

        int reported = new ExpirationChecker(repo, notifier).run(NOW);

        assertEquals(1, reported);
        verify(notifier).showExpirationNotification(
                Collections.emptyList(), Collections.singletonList(atLimit), NOW);
    }

    @Test
    public void run_justBeyondWindow_isNotIncluded() {
        FoodItem beyond = item("Cheese", NOW + ExpiryUtils.DEFAULT_USE_SOON_DAYS * DAY + 1);
        when(repo.getActiveItemsSync()).thenReturn(Collections.singletonList(beyond));

        int reported = new ExpirationChecker(repo, notifier).run(NOW);

        assertEquals(0, reported);
        verify(notifier, never()).showExpirationNotification(any(), any(), anyLong());
    }

    @Test
    public void run_expirationEqualsNow_isSoonNotExpired() {
        FoodItem exactlyNow = item("Bread", NOW);
        when(repo.getActiveItemsSync()).thenReturn(Collections.singletonList(exactlyNow));

        new ExpirationChecker(repo, notifier).run(NOW);

        // classify() treats "before now" as expired, so "equal to now" is still use-soon
        verify(notifier).showExpirationNotification(
                Collections.emptyList(), Collections.singletonList(exactlyNow), NOW);
    }
}
