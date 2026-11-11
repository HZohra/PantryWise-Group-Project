package com.cp470.groupproject.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.ui.shoppinglist.ShoppingListActivity;
import com.cp470.groupproject.util.ExpirationTiming;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the notification channel and builds/shows the expiration alert.
 * tapping the notification opens {@link ShoppingListActivity} through an explicit intent wrapped
 * in a {@link PendingIntent}. the class is not final so tests can mock it.
 */

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    /** id of the channel the user can configure in the system settings */
    public static final String CHANNEL_ID = "expiration_alerts";

    /** fixed id: a new alert replaces the previous one instead of stacking */
    public static final int NOTIFICATION_ID = 1001;

    /** maximum lines of the expanded notification */
    private static final int MAX_LINES = 6;

    private final Context context;

    /** @param context any context, the application context is kept */
    public NotificationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /** registers the notification channel (android 8+). safe to call many times */
    public void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH); // high = heads-up pop-up with sound
        channel.setDescription(context.getString(R.string.notif_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    /**
     * shows one notification that summarizes the expired items and the ones expiring soon.
     * nothing is shown if the user turned notifications off or denied the permission.
     *
     * @param expired items already expired
     * @param soon items inside the "use soon" window
     * @param now current time in milliseconds
     */
    @SuppressLint("MissingPermission") // checked below with areNotificationsEnabled()
    public void showExpirationNotification(@NonNull List<FoodItem> expired,
                                           @NonNull List<FoodItem> soon, long now) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "notifications are disabled, skipping expiration alert");
            return;
        }
        createChannel();

        int total = expired.size() + soon.size();
        List<String> lines = buildLines(expired, soon, now);
        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        for (int i = 0; i < Math.min(lines.size(), MAX_LINES); i++) {
            inbox.addLine(lines.get(i));
        }
        if (lines.size() > MAX_LINES) {
            inbox.setSummaryText(
                    context.getString(R.string.notif_more_lines, lines.size() - MAX_LINES));
        }

        // explicit intent: open the shopping list when the notification is tapped. the task stack
        // builder adds the parent declared in the manifest (MainActivity) underneath it, so "back"
        // goes to the main screen instead of whatever screen the app had open before
        PendingIntent open = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(ShoppingListActivity.newIntent(context))
                .getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getResources()
                        .getQuantityString(R.plurals.notif_title, total, total))
                .setContentText(context.getString(R.string.notif_summary,
                        expired.size(), soon.size()))
                .setStyle(inbox)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
        Log.d(TAG, "posted expiration notification: " + expired.size() + " expired, "
                + soon.size() + " soon");
    }

    /** one text line per item, expired ones first */
    private List<String> buildLines(List<FoodItem> expired, List<FoodItem> soon, long now) {
        ZoneId zone = ZoneId.systemDefault();
        List<String> lines = new ArrayList<>();

        for (FoodItem item : expired) {
            int days = ExpirationTiming.calendarDaysUntil(item.expirationDate, now, zone);
            if (days >= 0) {
                lines.add(context.getString(R.string.notif_line_expired_today, item.name));
            } else {
                int ago = -days;
                lines.add(context.getResources().getQuantityString(
                        R.plurals.notif_line_expired_days_ago, ago, ago, item.name));
            }
        }

        for (FoodItem item : soon) {
            int days = ExpirationTiming.calendarDaysUntil(item.expirationDate, now, zone);
            if (days <= 0) {
                lines.add(context.getString(R.string.notif_line_expires_today, item.name));
            } else if (days == 1) {
                lines.add(context.getString(R.string.notif_line_expires_tomorrow, item.name));
            } else {
                lines.add(context.getResources().getQuantityString(
                        R.plurals.notif_line_expires_in_days, days, days, item.name));
            }
        }
        return lines;
    }
}
