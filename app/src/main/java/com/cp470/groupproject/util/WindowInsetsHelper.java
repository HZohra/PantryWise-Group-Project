package com.cp470.groupproject.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge helper. since targetSdk 35 android draws the app under the status and navigation
 * bars, so each screen must add the size of those bars as padding or its toolbar would be covered.
 */

public final class WindowInsetsHelper {

    private WindowInsetsHelper() { }

    /**
     * adds the space taken by the system bars as padding of {@code root}
     *
     * @param root outermost view of the screen
     */
    public static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
