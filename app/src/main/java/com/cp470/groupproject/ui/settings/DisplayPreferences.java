package com.cp470.groupproject.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DisplayPreferences {

    public static int getTextSize(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                "PantryPreferences", Context.MODE_PRIVATE
        );

        Object savedSize = preferences.getAll().get("text_size");

        if (savedSize instanceof Integer) {
            return (int) savedSize;
        }

        preferences.edit().putInt("text_size", 16).apply();
        return 16;
    }

    public static void applyTextSize(Activity activity) {
        int textSize = getTextSize(activity);
        View root = activity.findViewById(android.R.id.content);
        applyToView(root, textSize);
    }

    private static void applyToView(View view, int textSize) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(textSize);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                applyToView(group.getChildAt(i), textSize);
            }
        }
    }
}