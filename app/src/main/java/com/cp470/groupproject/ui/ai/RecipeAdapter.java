package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;

import java.util.Objects;

/**
 * Shows recipes as cards: photo, title, "You have X of Y ingredients",
 * a "Uses expiring" badge and time · source. Lines with nothing to show are hidden.
 * The Activity only calls {@link #submitList}; DiffUtil works out what changed.
 */
public class RecipeAdapter extends ListAdapter<RecipeDto, RecipeAdapter.RecipeViewHolder> {

    /** Tapping a card (used by Recipe details in 5.7). */
    public interface OnRecipeClickListener {
        void onRecipeClick(@NonNull RecipeDto recipe);
    }

    static final DiffUtil.ItemCallback<RecipeDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<RecipeDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull RecipeDto oldItem, @NonNull RecipeDto newItem) {
            return Objects.equals(oldItem.recipeId, newItem.recipeId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull RecipeDto oldItem, @NonNull RecipeDto newItem) {
            MatchDto a = oldItem.match;
            MatchDto b = newItem.match;
            return Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.image, newItem.image)
                    && Objects.equals(oldItem.totalTimeMinutes, newItem.totalTimeMinutes)
                    && Objects.equals(oldItem.sourceName, newItem.sourceName)
                    && (a == null) == (b == null)
                    && (a == null || (a.haveCount == b.haveCount && a.totalCount == b.totalCount
                    && Objects.equals(RecipeCardFormatter.expiringNames(a), RecipeCardFormatter.expiringNames(b))));
        }
    };

    @Nullable private final OnRecipeClickListener listener;

    public RecipeAdapter(@Nullable OnRecipeClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public void onViewRecycled(@NonNull RecipeViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.image).clear(holder.image); // stop loading a photo for a card that scrolled away
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        private final TextView title;
        private final TextView have;
        private final TextView expiring;
        private final TextView meta;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ai_recipe_image);
            title = itemView.findViewById(R.id.ai_recipe_title);
            have = itemView.findViewById(R.id.ai_recipe_have);
            expiring = itemView.findViewById(R.id.ai_recipe_expiring);
            meta = itemView.findViewById(R.id.ai_recipe_meta);
        }

        void bind(@NonNull RecipeDto recipe, @Nullable OnRecipeClickListener listener) {
            Context context = itemView.getContext();
            String name = recipe.title == null ? "" : recipe.title;

            title.setText(name);

            // Photo: the signed URL expires after about an hour, so it's only shown, never stored
            image.setContentDescription(context.getString(R.string.ai_recipes_image_description, name));
            if (recipe.image != null && !recipe.image.isEmpty()) {
                Glide.with(image).load(recipe.image).centerCrop().into(image);
            } else {
                Glide.with(image).clear(image);
                image.setImageDrawable(null); // the grey background stays
            }

            // "You have X of Y ingredients" (recommend results only)
            MatchDto match = recipe.match;
            if (RecipeCardFormatter.hasMatch(match)) {
                have.setText(context.getResources().getQuantityString(
                        R.plurals.ai_recipes_have_count, match.totalCount, match.haveCount, match.totalCount));
                have.setVisibility(View.VISIBLE);
            } else {
                have.setVisibility(View.GONE);
            }

            // "Uses expiring: spinach, milk"
            String expiringText = RecipeCardFormatter.expiringNames(match);
            if (expiringText != null) {
                expiring.setText(context.getString(R.string.ai_recipes_uses_expiring, expiringText));
                expiring.setVisibility(View.VISIBLE);
            } else {
                expiring.setVisibility(View.GONE);
            }

            // "25 min · BBC Good Food"
            Integer minutes = RecipeCardFormatter.minutes(recipe.totalTimeMinutes);
            String metaText = RecipeCardFormatter.joinMeta(
                    minutes == null ? null : context.getString(R.string.ai_recipes_minutes, minutes),
                    recipe.sourceName);
            if (metaText != null) {
                meta.setText(metaText);
                meta.setVisibility(View.VISIBLE);
            } else {
                meta.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(listener == null ? null : v -> listener.onRecipeClick(recipe));
        }
    }
}