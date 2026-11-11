package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FavoriteRecipe;

import java.util.Objects;

/**
 * Favourite recipes as cards: photo (or a placeholder), title, "servings · time · source" and a
 * heart to remove it. Edamam photo links expire after about an hour, so Glide falls back to the
 * placeholder then.
 */
public class FavoriteAdapter extends ListAdapter<FavoriteRecipe, FavoriteAdapter.FavoriteViewHolder> {

    /** What the screen does when a card or its heart is tapped. */
    public interface Listener {
        void onOpen(@NonNull FavoriteRecipe recipe);
        void onRemove(@NonNull FavoriteRecipe recipe);
    }

    static final DiffUtil.ItemCallback<FavoriteRecipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<FavoriteRecipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull FavoriteRecipe oldItem, @NonNull FavoriteRecipe newItem) {
            return oldItem.recipeId.equals(newItem.recipeId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FavoriteRecipe oldItem, @NonNull FavoriteRecipe newItem) {
            return oldItem.title.equals(newItem.title)
                    && Objects.equals(oldItem.imageUrl, newItem.imageUrl)
                    && Objects.equals(oldItem.servings, newItem.servings)
                    && Objects.equals(oldItem.totalTimeMinutes, newItem.totalTimeMinutes)
                    && Objects.equals(oldItem.sourceName, newItem.sourceName);
        }
    };

    @NonNull private final Listener listener;

    public FavoriteAdapter(@NonNull Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /** The recipe shown at a position (used by swipe to remove). */
    @NonNull
    public FavoriteRecipe getItemAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public void onViewRecycled(@NonNull FavoriteViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.image).clear(holder.image); // stop loading a photo for a card that scrolled away
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        final ImageView image;
        private final TextView name;
        private final TextView meta;
        private final View remove;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ai_fav_image);
            name = itemView.findViewById(R.id.ai_fav_name);
            meta = itemView.findViewById(R.id.ai_fav_meta);
            remove = itemView.findViewById(R.id.ai_fav_remove);
        }

        void bind(@NonNull FavoriteRecipe recipe, @NonNull Listener listener) {
            Context context = itemView.getContext();
            name.setText(recipe.title);

            image.setContentDescription(context.getString(R.string.ai_recipes_image_description, recipe.title));
            if (recipe.imageUrl != null) {
                Glide.with(image).load(recipe.imageUrl).centerCrop()
                        .placeholder(R.drawable.ic_recipe_placeholder)
                        .error(R.drawable.ic_recipe_placeholder) // expired link or no network
                        .into(image);
            } else {
                Glide.with(image).clear(image);
                image.setImageResource(R.drawable.ic_recipe_placeholder);
            }

            String servings = recipe.servings == null ? null : context.getResources()
                    .getQuantityString(R.plurals.ai_detail_servings, recipe.servings, recipe.servings);
            String minutes = recipe.totalTimeMinutes == null ? null
                    : context.getString(R.string.ai_recipes_minutes, recipe.totalTimeMinutes);
            String source = recipe.isAi() ? context.getString(R.string.ai_fav_ai_label) : recipe.sourceName;
            String metaText = RecipeCardFormatter.joinMeta(servings, minutes, source);
            meta.setText(metaText);
            meta.setVisibility(metaText == null ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(v -> listener.onOpen(recipe));
            remove.setOnClickListener(v -> listener.onRemove(recipe));
        }
    }
}