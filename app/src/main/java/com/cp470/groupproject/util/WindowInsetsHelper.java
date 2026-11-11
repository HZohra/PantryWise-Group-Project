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
     * adds the space taken by the system bars to the padding {@code root} already has
     *
     * @param root outermost view of the screen
     */
    public static void applySystemBarPadding(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }
}
