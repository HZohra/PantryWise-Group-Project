package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.cp470.groupproject.data.repository.AiRepository;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.data.entity.MealPlan;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * State and logic for the AI recipe screen (MVVM: no Views here).
 * Survives rotation, so a recipe is never generated twice by accident. The recipe on screen can
 * be saved to (or removed from) the favourites with its steps.
 * AiRepository calls back on a background thread, so those updates use postValue().
 */
public class AiRecipeViewModel extends AndroidViewModel {

    static final int DEFAULT_SERVINGS = 2;

    /** Exactly one of these is true at a time, so the screen can't show a recipe and an error together. */
    public static final class UiState {
        public enum Kind { IDLE, LOADING, SUCCESS, ERROR }

        @NonNull public final Kind kind;
        @Nullable public final AiRecipeResponse response;
        @Nullable public final AiError error;

        private UiState(@NonNull Kind kind, @Nullable AiRecipeResponse response, @Nullable AiError error) {
            this.kind = kind;
            this.response = response;
            this.error = error;
        }

        static UiState idle() { return new UiState(Kind.IDLE, null, null); }
        static UiState loading() { return new UiState(Kind.LOADING, null, null); }
        static UiState success(@NonNull AiRecipeResponse r) { return new UiState(Kind.SUCCESS, r, null); }
        static UiState error(@NonNull AiError e) { return new UiState(Kind.ERROR, null, e); }
    }

    /**
     * Whether one recipe is in the favourites. The id says which recipe the answer is about, so
     * after "Try another" the screen never shows the old recipe's heart on the new one.
     */
    public static final class FavoriteState {
        @NonNull public final String recipeId;
        public final boolean saved;

        FavoriteState(@NonNull String recipeId, boolean saved) {
            this.recipeId = recipeId;
            this.saved = saved;
        }
    }

    private final AiRepository repository;
    private final MutableLiveData<UiState> state = new MutableLiveData<>(UiState.idle());
    /** Only the newest request may update the screen. */
    private final AtomicInteger latestRequest = new AtomicInteger();
    private boolean consentGiven;

    /** Local database for the favourites; created on first use so tests without a database still work. */
    @Nullable private PantryRepository pantryRepository;
    /** Favourite state of the recipe on screen; created on first use. */
    @Nullable private LiveData<FavoriteState> favorite;

    public AiRecipeViewModel(@NonNull Application application) {
        this(application, new AiRepository(application), null);
    }

    AiRecipeViewModel(@NonNull Application application, @NonNull AiRepository repository) {
        this(application, repository, null);
    }

    /** @param pantryRepository the favourites storage, or null to create the real one when needed */
    AiRecipeViewModel(@NonNull Application application, @NonNull AiRepository repository,
                      @Nullable PantryRepository pantryRepository) {
        super(application);
        this.repository = repository;
        this.pantryRepository = pantryRepository;
    }

    public LiveData<UiState> getState() { return state; }

    /** US-6: true until the user accepts the dialog on this screen. */
    public boolean needsConsent() { return !consentGiven; }
    public void giveConsent() { consentGiven = true; }

    /** True before the first request (the screen asks for consent or starts generating). */
    public boolean isIdle() {
        UiState current = state.getValue();
        return current == null || current.kind == UiState.Kind.IDLE;
    }

    /** Main thread. Asks the backend for a recipe (also used by "Try another" / "Try again"). */
    public void generate() {
        if (!consentGiven) return; // nothing is sent without consent
        UiState current = state.getValue();
        if (current != null && current.kind == UiState.Kind.LOADING) return;

        final int request = latestRequest.incrementAndGet();
        state.setValue(UiState.loading());
        repository.generateRecipe(null, DEFAULT_SERVINGS, result -> {
            // background thread from here on
            if (request != latestRequest.get()) return;
            AiRecipeResponse data = result.getData();
            if (result.isSuccess() && data != null && data.recipe != null) {
                state.postValue(UiState.success(data));
            } else {
                AiError error = result.getError() != null
                        ? result.getError()
                        : new AiError(AiError.UNEXPECTED_RESPONSE, "Empty response", 200, null);
                state.postValue(UiState.error(error));
            }
        });
    }

    // ---------------------------------------------------------------- favourites

    /** The id of the recipe on screen, or null while loading, after an error or without an id. */
    @Nullable
    public String currentRecipeId() {
        return recipeIdOf(state.getValue());
    }

    /** True when a recipe with an id is on screen, so it can be saved. */
    public boolean canFavorite() {
        return currentRecipeId() != null;
    }

    /**
     * Main thread. Follows the recipe on screen: emits its favourite state from the database, or
     * null while there is no recipe to save.
     */
    @NonNull
    public LiveData<FavoriteState> getFavorite() {
        if (favorite == null) {
            LiveData<String> recipeId = Transformations.distinctUntilChanged(
                    Transformations.map(state, AiRecipeViewModel::recipeIdOf));
            favorite = Transformations.switchMap(recipeId, id -> {
                if (id == null) {
                    return new MutableLiveData<>((FavoriteState) null);
                }
                return Transformations.map(pantry().isFavorite(id),
                        count -> new FavoriteState(id, count != null && count > 0));
            });
        }
        return favorite;
    }

    /**
     * Main thread. Saves the recipe on screen with its steps, or removes it when it is already saved.
     * @return TRUE when it was saved, FALSE when it was removed, null when there is nothing to save
     */
    @Nullable
    public Boolean toggleFavorite() {
        UiState current = state.getValue();
        AiRecipeDto recipe = current == null || current.response == null ? null : current.response.recipe;
        FavoriteRecipe toSave = FavoriteRecipeMapper.fromAi(recipe);
        if (toSave == null) return null;

        FavoriteState known = getFavorite().getValue();
        boolean saved = known != null && known.saved && toSave.recipeId.equals(known.recipeId);
        if (saved) {
            pantry().removeFavoriteRecipe(toSave.recipeId);
            return false;
        }
        pantry().saveFavoriteRecipe(toSave);
        return true;
    }

    /** The trimmed recipe id of a successful state, or null. */
    @Nullable
    static String recipeIdOf(@Nullable UiState state) {
        if (state == null || state.kind != UiState.Kind.SUCCESS || state.response == null
                || state.response.recipe == null || state.response.recipe.recipeId == null) {
            return null;
        }
        String id = state.response.recipe.recipeId.trim();
        return id.isEmpty() ? null : id;
    }


    /**
     * Main thread. Adds the missing ingredients of the recipe on screen to the shopping list.
     * @param callback receives how many were added (background thread); not called when there is no recipe
     */
    public void addMissingToShoppingList(@NonNull PantryRepository.ResultCallback<Integer> callback) {
        UiState current = state.getValue();
        if (current == null || current.response == null) return;
        pantry().addShoppingItemsIfMissing(MissingIngredients.fromAi(current.response.recipe), callback);
    }


    /**
     * Main thread. Plans the recipe on screen for a day and meal (ingredients saved as a snapshot).
     * @return false when there is no recipe with an id on screen
     */
    public boolean planMeal(long planDate, @NonNull String mealType,
                            @NonNull PantryRepository.ResultCallback<Long> callback) {
        UiState current = state.getValue();
        String id = recipeIdOf(current);
        if (id == null) return false;
        AiRecipeDto recipe = current.response.recipe;
        String title = recipe.title == null ? "" : recipe.title.trim();
        pantry().addMealPlan(new MealPlan(planDate, mealType, id, title,
                FavoriteRecipeMapper.aiIngredientLines(recipe)), callback);
        return true;
    }

    @NonNull
    private PantryRepository pantry() {
        if (pantryRepository == null) {
            pantryRepository = new PantryRepository(getApplication());
        }
        return pantryRepository;
    }

    /** Tests only: shows a recipe as if the backend had returned it (no network). Main thread. */
    @VisibleForTesting
    void showRecipeForTest(@NonNull AiRecipeResponse response) {
        state.setValue(UiState.success(response));
    }
}