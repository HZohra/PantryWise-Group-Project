package com.cp470.groupproject.ui.inventory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.Freshness;
import com.cp470.groupproject.ui.itemdetail.ItemDetailActivity;
import com.cp470.groupproject.util.ExpiryUtils;
import com.cp470.groupproject.ui.settings.DisplayPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private final List<FoodItem> allItems = new ArrayList<>();
    private final List<FoodItem> foodItems = new ArrayList<>();
    private String searchQuery = "";

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);

        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FoodViewHolder holder, int position) {

        FoodItem item = foodItems.get(position);

        int textSize = DisplayPreferences.getTextSize(holder.itemView.getContext());

        holder.foodName.setTextSize(textSize);
        holder.foodQuantity.setTextSize(textSize);
        holder.foodExpiry.setTextSize(textSize);

        holder.foodName.setText(item.name);

        holder.foodQuantity.setText(holder.itemView.getContext()
                .getString(R.string.food_quantity_line, String.valueOf(item.quantity), item.unit));

        Freshness freshness = ExpiryUtils.classify(
                item.expirationDate,
                System.currentTimeMillis(),
                ExpiryUtils.DEFAULT_USE_SOON_DAYS
        );

        switch (freshness) {
            case FRESH:
                holder.foodExpiry.setText(R.string.status_fresh);
                holder.foodExpiry.setTextColor(Color.rgb(46, 125, 50));
                break;

            case USE_SOON:
                holder.foodExpiry.setText(R.string.status_use_soon);
                holder.foodExpiry.setTextColor(Color.rgb(239, 143, 0));
                break;

            case EXPIRED:
                holder.foodExpiry.setText(R.string.status_expired);
                holder.foodExpiry.setTextColor(Color.rgb(198, 40, 40));
                break;

            case NO_DATE:
                holder.foodExpiry.setText(R.string.status_no_date);
                holder.foodExpiry.setTextColor(Color.GRAY);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = ItemDetailActivity.newIntent(context, item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }

    public void setFoodItems(List<FoodItem> items) {
        allItems.clear();

        if (items != null) {
            allItems.addAll(items);
        }

        filter(searchQuery);
    }

    public void filter(String query) {
        searchQuery = query == null ? "" :
                query.trim().toLowerCase(Locale.ROOT);

        foodItems.clear();

        for (FoodItem item : allItems) {
            if (item.name != null &&
                    item.name.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                foodItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {

        TextView foodName;
        TextView foodQuantity;
        TextView foodExpiry;

        FoodViewHolder(@NonNull View itemView) {
            super(itemView);

            foodName = itemView.findViewById(R.id.foodName);
            foodQuantity = itemView.findViewById(R.id.foodQuantity);
            foodExpiry = itemView.findViewById(R.id.foodExpiry);
        }
    }
}