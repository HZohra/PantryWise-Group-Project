package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.cp470.groupproject.data.repository.AiRepository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * State and logic for the AI recipe screen (MVVM: no Views here).
 * Survives rotation, so a recipe is never generated twice by accident.
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

    private final AiRepository repository;
    private final MutableLiveData<UiState> state = new MutableLiveData<>(UiState.idle());
    /** Only the newest request may update the screen. */
    private final AtomicInteger latestRequest = new AtomicInteger();
    private boolean consentGiven;

    public AiRecipeViewModel(@NonNull Application application) {
        this(application, new AiRepository(application));
    }

    AiRecipeViewModel(@NonNull Application application, @NonNull AiRepository repository) {
        super(application);
        this.repository = repository;
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
}