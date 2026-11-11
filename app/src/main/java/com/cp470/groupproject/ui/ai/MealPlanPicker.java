package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.model.MealType;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * "Plan this meal": a date picker (today or later), then "Which meal?" (Dinner preselected).
 * Shared by Recipe details and the AI recipe screen.
 */
final class MealPlanPicker {

    /** Receives the chosen day (local start of day) and one of the {@link MealType} constants. */
    interface Listener {
        void onPicked(long planDate, @NonNull String mealType);
    }

    private static final String TAG = "ai_meal_plan_date";
    private static final int DINNER_INDEX = 2; // position of DINNER in MealType.ALL

    private MealPlanPicker() { }

    static void show(@NonNull AppCompatActivity activity, @NonNull Listener listener) {
        long today = PlanDates.todayAsUtcDate(System.currentTimeMillis(), TimeZone.getDefault());
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.ai_plan_pick_day)
                .setSelection(today)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setStart(today)
                        .setValidator(DateValidatorPointForward.from(today))
                        .build())
                .build();
        attach(activity, picker, listener);
        picker.show(activity.getSupportFragmentManager(), TAG);
    }

    /** Call in onCreate: after rotation the picker comes back by itself but has lost its listener. */
    @SuppressWarnings("unchecked")
    static void reattach(@NonNull AppCompatActivity activity, @NonNull Listener listener) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment instanceof MaterialDatePicker) {
            attach(activity, (MaterialDatePicker<Long>) fragment, listener);
        }
    }

    private static void attach(@NonNull AppCompatActivity activity, @NonNull MaterialDatePicker<Long> picker,
                               @NonNull Listener listener) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(selection -> askMeal(activity,
                PlanDates.localStartOfDay(selection, TimeZone.getDefault()), listener));
    }

    private static void askMeal(@NonNull AppCompatActivity activity, long planDate, @NonNull Listener listener) {
        String[] labels = new String[MealType.ALL.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = label(activity, MealType.ALL[i]);
        }
        final int[] chosen = {DINNER_INDEX};
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.ai_plan_pick_meal)
                .setSingleChoiceItems(labels, DINNER_INDEX, (dialog, which) -> chosen[0] = which)
                .setPositiveButton(R.string.ai_plan_save,
                        (dialog, which) -> listener.onPicked(planDate, MealType.ALL[chosen[0]]))
                .setNegativeButton(R.string.ai_consent_cancel, null)
                .show();
    }

    /** "Planned for Dinner on Nov 12, 2026" */
    static void showPlanned(@NonNull Context context, long planDate, @NonNull String mealType) {
        String day = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(planDate));
        Toast.makeText(context.getApplicationContext(),
                context.getString(R.string.ai_plan_done, label(context, mealType), day),
                Toast.LENGTH_LONG).show();
    }

    @NonNull
    static String label(@NonNull Context context, @NonNull String mealType) {
        switch (mealType) {
            case MealType.BREAKFAST: return context.getString(R.string.ai_plan_breakfast);
            case MealType.LUNCH: return context.getString(R.string.ai_plan_lunch);
            case MealType.SNACK: return context.getString(R.string.ai_plan_snack);
            default: return context.getString(R.string.ai_plan_dinner);
        }
    }
}