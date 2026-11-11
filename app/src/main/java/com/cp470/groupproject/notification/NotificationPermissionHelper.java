package com.cp470.groupproject.notification;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cp470.groupproject.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * Handles the runtime permission {@code POST_NOTIFICATIONS}, required from android 13 (API 33).
 * on older versions the permission does not exist and nothing is asked.
 *
 * flow: explain why with a dialog (only when android recommends it) → system permission dialog →
 * if denied, a snackbar offers to open the notification settings. the app never crashes without
 * the permission: {@link NotificationHelper} simply skips the alert.
 *
 * create the helper in {@code onCreate}, before the activity is started, because it registers an
 * activity result launcher.
 */

public class NotificationPermissionHelper {

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String> launcher;

    /** @param activity screen that will show the dialogs. call this in its onCreate */
    public NotificationPermissionHelper(@NonNull AppCompatActivity activity) {
        this.activity = activity;
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) showDisabledHint();
                });
    }

    /** asks for the permission if it is needed and not granted yet */
    public void ensurePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (isGranted(activity)) return;

        if (activity.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.notif_permission_title)
                    .setMessage(R.string.notif_permission_message)
                    .setPositiveButton(R.string.notif_action_allow, (dialog, which) ->
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.notif_action_not_now, null)
                    .show();
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** @return true if notifications may be shown (always true before android 13) */
    public static boolean isGranted(@NonNull Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** opens the system screen where the user can turn notifications on for this app */
    public static void openNotificationSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * after two denials android stops showing its dialog and the result arrives immediately as
     * "denied", so the only way back is the settings screen
     */
    private void showDisabledHint() {
        Snackbar.make(activity.findViewById(android.R.id.content),
                        R.string.notif_disabled_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.notif_action_open_settings,
                        v -> openNotificationSettings(activity))
                .show();
    }
}
