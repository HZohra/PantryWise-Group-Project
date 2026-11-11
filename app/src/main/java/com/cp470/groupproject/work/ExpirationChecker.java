package com.cp470.groupproject.work;

import androidx.annotation.NonNull;

import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.notification.NotificationHelper;
import com.cp470.groupproject.util.ExpiryUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides which inventory items deserve an expiration alert and asks {@link NotificationHelper} to
 * show it. it contains no android code of its own and receives its dependencies through the
 * constructor, so unit tests can replace them with mockito mocks.
 */

public class ExpirationChecker {

    private final PantryRepository repository;
    private final NotificationHelper notifier;

    /**
     * @param repository source of the active inventory
     * @param notifier shows the notification
     */
    public ExpirationChecker(@NonNull PantryRepository repository,
                             @NonNull NotificationHelper notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    /**
     * splits the active inventory into expired items and items expiring soon (default window of
     * {@link ExpiryUtils#DEFAULT_USE_SOON_DAYS} days) and posts one notification if there is any.
     * must be called off the main thread because it reads the database.
     *
     * @param now current time in milliseconds
     * @return how many items motivated the alert, 0 when nothing was shown
     */
    public int run(long now) {
        List<FoodItem> expired = new ArrayList<>();
        List<FoodItem> soon = new ArrayList<>();
        for (FoodItem item : repository.getActiveItemsSync()) {
            switch (ExpiryUtils.classify(item.expirationDate, now,
                    ExpiryUtils.DEFAULT_USE_SOON_DAYS)) {
                case EXPIRED:
                    expired.add(item);
                    break;
                case USE_SOON:
                    soon.add(item);
                    break;
                default:
                    break; // fresh and no-date items are not reported
            }
        }
        if (expired.isEmpty() && soon.isEmpty()) return 0;
        notifier.showExpirationNotification(expired, soon, now);
        return expired.size() + soon.size();
    }
}
