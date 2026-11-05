package com.cp470.groupproject.ui.mealplanner;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.model.MealType;
import com.cp470.groupproject.util.WeekUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Adapter of the planner: one card per day with its four meal slots. */
public class MealPlannerAdapter extends ListAdapter<DayPlan, MealPlannerAdapter.Holder> {

    /** events of the cards, handled by the activity */
    public interface Listener {
        /**
         * a meal slot was tapped.
         *
         * @param dayStartMs start of the day of the slot
         * @param mealType slot type
         * @param existingOrNull the meal in the slot, or null if it is empty
         */
        void onSlotClicked(long dayStartMs, String mealType, MealPlan existingOrNull);
    }

    private static final DiffUtil.ItemCallback<DayPlan> DIFF = new DiffUtil.ItemCallback<DayPlan>() {
        @Override
        public boolean areItemsTheSame(@NonNull DayPlan a, @NonNull DayPlan b) {
            return a.dayStartMs == b.dayStartMs;
        }

        @Override
        public boolean areContentsTheSame(@NonNull DayPlan a, @NonNull DayPlan b) {
            for (String type : MealType.ALL) {
                if (!sameMeal(a.get(type), b.get(type))) return false;
            }
            return true;
        }
    };

    private static boolean sameMeal(MealPlan a, MealPlan b) {
        if (a == null || b == null) return a == b;
        return a.id == b.id
                && a.recipeTitle.equals(b.recipeTitle)
                && a.completed == b.completed
                && Objects.equals(a.ingredients, b.ingredients);
    }

    private final Listener listener;
    private final ZoneId zone;

    /**
     * @param listener receives the slot clicks
     * @param zone time zone used to write the days and find today
     */
    public MealPlannerAdapter(@NonNull Listener listener, @NonNull ZoneId zone) {
        super(DIFF);
        this.listener = listener;
        this.zone = zone;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DayPlan day = getItem(position);
        Context context = holder.itemView.getContext();

        holder.dayTitle.setText(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
                .format(Instant.ofEpochMilli(day.dayStartMs).atZone(zone)));
        boolean today = day.dayStartMs == WeekUtils.startOfDay(System.currentTimeMillis(), zone);
        holder.dayTitle.setTypeface(null, today ? Typeface.BOLD : Typeface.NORMAL);

        bindSlot(context, holder.breakfast, day, MealType.BREAKFAST, R.string.planner_meal_breakfast);
        bindSlot(context, holder.lunch, day, MealType.LUNCH, R.string.planner_meal_lunch);
        bindSlot(context, holder.dinner, day, MealType.DINNER, R.string.planner_meal_dinner);
        bindSlot(context, holder.snack, day, MealType.SNACK, R.string.planner_meal_snack);
    }

    private void bindSlot(Context context, TextView view, DayPlan day, String mealType, int nameRes) {
        MealPlan meal = day.get(mealType);
        String name = context.getString(nameRes);
        String text;
        if (meal == null) {
            text = context.getString(R.string.planner_slot_format, name,
                    context.getString(R.string.planner_slot_empty));
        } else {
            text = context.getString(R.string.planner_slot_format, name, meal.recipeTitle);
            if (meal.completed) {
                text += " " + context.getString(R.string.planner_cooked_suffix);
            }
        }
        view.setText(text);
        int flags = view.getPaintFlags();
        boolean cooked = meal != null && meal.completed;
        view.setPaintFlags(cooked
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        view.setOnClickListener(v -> listener.onSlotClicked(day.dayStartMs, mealType, meal));
    }

    /** views of one day card */
    static class Holder extends RecyclerView.ViewHolder {
        final TextView dayTitle;
        final TextView breakfast;
        final TextView lunch;
        final TextView dinner;
        final TextView snack;

        Holder(@NonNull View itemView) {
            super(itemView);
            dayTitle = itemView.findViewById(R.id.txtDayTitle);
            breakfast = itemView.findViewById(R.id.slotBreakfast);
            lunch = itemView.findViewById(R.id.slotLunch);
            dinner = itemView.findViewById(R.id.slotDinner);
            snack = itemView.findViewById(R.id.slotSnack);
        }
    }
}
