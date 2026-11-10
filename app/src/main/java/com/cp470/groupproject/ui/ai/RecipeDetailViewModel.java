package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.dto.MatchDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.SubstituteResponse;
import com.cp470.groupproject.data.repository.RecipeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State and logic for Recipe details (MVVM: no Views here).
 * Holds the recipe across rotation, marks each ingredient line, and runs substitute requests.
 * RecipeRepository calls back on a background thread, so those updates use postValue().
 */
public class RecipeDetailViewModel extends AndroidViewModel {

    /** One ingredient line with its pantry status. */
    public static final class IngredientRow {
        @NonNull public final String line;
        @NonNull public final IngredientMatcher.Status status;
        /** The matched food, e.g. "egg" (sent to "Find substitute"); null when UNKNOWN. */
        @Nullable public final String name;

        IngredientRow(@NonNull String line, @NonNull IngredientMatcher.Status status, @Nullable String name) {
            this.line = line;
            this.status = status;
            this.name = name;
        }
    }

    /** What the substitutes sheet shows. null LiveData value = sheet closed. */
    public static final class SubstituteState {
        @NonNull public final String ingredient;
        public final boolean loading;
        @Nullable public final SubstituteResponse response;
        @Nullable public final AiError error;

        private SubstituteState(@NonNull String ingredient, boolean loading,
                                @Nullable SubstituteResponse response, @Nullable AiError error) {
            this.ingredient = ingredient;
            this.loading = loading;
            this.response = response;
            this.error = error;
        }

        static SubstituteState loading(@NonNull String ingredient) {
            return new SubstituteState(ingredient, true, null, null);
        }

        static SubstituteState done(@NonNull String ingredient, @NonNull SubstituteResponse response) {
            return new SubstituteState(ingredient, false, response, null);
        }

        static SubstituteState failed(@NonNull String ingredient, @NonNull AiError error) {
            return new SubstituteState(ingredient, false, null, error);
        }
    }

    private final RecipeRepository repository;
    private final MutableLiveData<SubstituteState> substitute = new MutableLiveData<>(null);
    /** Only the newest substitute request may update the sheet. */
    private final AtomicInteger latestRequest = new AtomicInteger();

    @Nullable private RecipeDto recipe;
    @NonNull private List<IngredientRow> rows = Collections.emptyList();

    public RecipeDetailViewModel(@NonNull Application application) {
        this(application, new RecipeRepository(application));
    }

    RecipeDetailViewModel(@NonNull Application application, @NonNull RecipeRepository repository) {
        super(application);
        this.repository = repository;
    }

    /**
     * Main thread. Keeps the first valid recipe (later calls, e.g. after rotation, are ignored).
     * @return false if there is no usable recipe, so the screen should close
     */
    public boolean setRecipeIfAbsent(@Nullable RecipeDto candidate) {
        if (recipe != null) return true;
        if (candidate == null || candidate.title == null) return false;
        recipe = candidate;
        rows = buildRows(candidate);
        return true;
    }

    @Nullable public RecipeDto getRecipe() { return recipe; }
    @NonNull public List<IngredientRow> getRows() { return rows; }
    public LiveData<SubstituteState> getSubstitute() { return substitute; }

    /** Main thread. Opens the sheet in "loading" state and asks the backend. */
    public void findSubstitute(@NonNull String ingredient) {
        RecipeDto current = recipe;
        if (current == null) return;
        final int request = latestRequest.incrementAndGet();
        substitute.setValue(SubstituteState.loading(ingredient));

        repository.substitute(current.title, ingredient, current.ingredientLines, null, result -> {
            // background thread from here on
            if (request != latestRequest.get()) return; // closed, or another ingredient was tapped
            SubstituteResponse data = result.getData();
            if (result.isSuccess() && data != null) {
                substitute.postValue(SubstituteState.done(ingredient, data));
            } else {
                AiError error = result.getError() != null
                        ? result.getError()
                        : new AiError(AiError.UNEXPECTED_RESPONSE, "Empty response", 200, null);
                substitute.postValue(SubstituteState.failed(ingredient, error));
            }
        });
    }

    /** Main thread. Closes the sheet; a reply still on its way is ignored. */
    public void closeSubstitutes() {
        latestRequest.incrementAndGet();
        substitute.setValue(null);
    }

    /** One row per ingredient line, marked with the backend's have/missing names. */
    @NonNull
    static List<IngredientRow> buildRows(@NonNull RecipeDto recipe) {
        List<IngredientRow> result = new ArrayList<>();
        if (recipe.ingredientLines == null) return result;
        MatchDto match = recipe.match;
        List<String> have = match == null ? null : match.have;
        List<String> missing = match == null ? null : match.missing;
        for (String line : recipe.ingredientLines) {
            if (line == null || line.trim().isEmpty()) continue;
            IngredientMatcher.Match m = IngredientMatcher.matchLine(line, have, missing);
            result.add(new IngredientRow(line.trim(), m.status, m.name));
        }
        return result;
    }

    /** "- 2 eggs\n- 1 cup milk" … stopping before maxChars, so the chat question fits its limit. */
    @NonNull
    static String buildIngredientList(@Nullable List<String> lines, int maxChars) {
        StringBuilder list = new StringBuilder();
        if (lines == null) return "";
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            String entry = (list.length() == 0 ? "" : "\n") + "- " + line.trim();
            if (list.length() + entry.length() > maxChars) break;
            list.append(entry);
        }
        return list.toString();
    }
}