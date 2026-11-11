package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.preferences.AiPreferencesStore;
import com.cp470.groupproject.data.remote.AiError;
import com.cp470.groupproject.data.remote.dto.ChatMessageDto;
import com.cp470.groupproject.data.remote.dto.ChatResponse;
import com.cp470.groupproject.data.remote.dto.ChatShoppingItemDto;
import com.cp470.groupproject.data.repository.AiRepository;
import com.cp470.groupproject.data.repository.PantryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State and logic for the AI chat screen (MVVM: no Views here).
 * Survives rotation; AiRepository calls back on a background thread, so those updates use postValue().
 * Items the assistant proposes for the shopping list are only saved when the user taps Add.
 */
public class AiChatViewModel extends AndroidViewModel {

    private final AiRepository repository;
    /** Shopping list storage; created on first use so tests without a database still work. */
    @Nullable private PantryRepository pantryRepository;

    /** Source of truth, guarded by itself. LiveData only ever receives copies. */
    private final List<ChatMessage> conversation = new ArrayList<>();

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<AiError> error = new MutableLiveData<>(null);

    /** Bumped by newChat(), so a late reply to an old conversation is ignored. */
    private final AtomicInteger conversationId = new AtomicInteger();

    private boolean consentGiven;
    @Nullable private volatile String failedMessage;

    public AiChatViewModel(@NonNull Application application) {
        this(application, new AiRepository(application), null);
    }

    AiChatViewModel(@NonNull Application application, @NonNull AiRepository repository) {
        this(application, repository, null);
    }

    /** @param pantryRepository the shopping list storage, or null to create the real one when needed */
    AiChatViewModel(@NonNull Application application, @NonNull AiRepository repository,
                    @Nullable PantryRepository pantryRepository) {
        super(application);
        this.repository = repository;
        this.pantryRepository = pantryRepository;
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<Boolean> isLoading() { return loading; }
    /** Non-null when an error should be shown; call {@link #onErrorShown()} after showing it. */
    public LiveData<AiError> getError() { return error; }

    /** US-6: true until the user accepts the dialog once in this session. */
    public boolean needsConsent() { return !consentGiven; }
    public void giveConsent() { consentGiven = true; }

    /** Main thread. Adds the user's bubble, then asks the backend. */
    public void sendMessage(@NonNull String rawText) {
        String text = rawText.trim();
        if (text.isEmpty() || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        List<ChatMessageDto> history;
        List<ChatMessage> snapshot;
        synchronized (conversation) {
            history = toHistory(conversation); // before adding the new message
            conversation.add(ChatMessage.fromUser(text));
            snapshot = new ArrayList<>(conversation);
        }
        messages.setValue(snapshot);
        loading.setValue(true);
        error.setValue(null);
        failedMessage = null;

        final int requestConversation = conversationId.get();
        repository.sendChat(text, history, AiPreferencesStore.currentDto(getApplication()), result -> {
            // background thread from here on
            if (requestConversation != conversationId.get()) {
                return; // "New chat" was pressed while waiting
            }
            ChatResponse data = result.getData();
            String reply = data == null || data.reply == null ? null : data.reply.trim();
            if (result.isSuccess() && reply != null && !reply.isEmpty()) {
                publish(ChatMessage.fromAi(reply, data.shoppingItems));
            } else {
                failedMessage = text;
                AiError failure = result.getError() != null
                        ? result.getError()
                        : new AiError(AiError.UNEXPECTED_RESPONSE, "Empty reply", 200, null);
                error.postValue(failure);
            }
            loading.postValue(false);
        });
    }

    /** Main thread. Re-sends the last failed message without leaving a duplicate bubble. */
    public void retry() {
        String text = failedMessage;
        if (text == null || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        synchronized (conversation) {
            int last = conversation.size() - 1;
            if (last >= 0 && conversation.get(last).isFromUser()
                    && conversation.get(last).text.equals(text)) {
                conversation.remove(last);
            }
        }
        sendMessage(text);
    }

    /** Main thread. Call after the Snackbar is shown so it doesn't reappear on rotation. */
    public void onErrorShown() {
        error.setValue(null);
    }

    /** Main thread. Clears the session; the next request is sent with no history. */
    public void newChat() {
        conversationId.incrementAndGet();
        synchronized (conversation) {
            conversation.clear();
        }
        messages.setValue(Collections.emptyList());
        loading.setValue(false);
        error.setValue(null);
        failedMessage = null;
    }

    // ---------------------------------------------------------------- proposed shopping items

    /**
     * Main thread. "Add" on the card: saves the proposed items (no duplicates) and marks them added.
     * Does nothing if they were already added or dismissed (a double tap can't add twice).
     * @param onDone receives how many new items were added, on a background thread
     */
    public void addSuggestedItems(long messageId, @NonNull PantryRepository.ResultCallback<Integer> onDone) {
        ChatMessage message = changeItemsState(messageId, ChatMessage.ItemsState.ADDED);
        if (message == null) return;
        List<ShoppingItem> items = new ArrayList<>();
        for (ChatShoppingItemDto item : message.items) {
            items.add(MissingIngredients.toItem((item.amount + " " + item.name).trim(), item.name));
        }
        pantry().addShoppingItemsIfMissing(items, onDone);
    }

    /** Main thread. "No thanks" on the card. */
    public void dismissSuggestedItems(long messageId) {
        changeItemsState(messageId, ChatMessage.ItemsState.DISMISSED);
    }

    /** Replaces the message's items state if it is still PENDING; returns the message, or null. */
    @Nullable
    private ChatMessage changeItemsState(long messageId, @NonNull ChatMessage.ItemsState state) {
        ChatMessage found = null;
        List<ChatMessage> snapshot;
        synchronized (conversation) {
            for (int i = 0; i < conversation.size(); i++) {
                ChatMessage message = conversation.get(i);
                if (message.id == messageId && message.itemsState == ChatMessage.ItemsState.PENDING) {
                    found = message;
                    conversation.set(i, message.withItemsState(state));
                    break;
                }
            }
            if (found == null) return null;
            snapshot = new ArrayList<>(conversation);
        }
        messages.setValue(snapshot);
        return found;
    }

    @NonNull
    private PantryRepository pantry() {
        if (pantryRepository == null) {
            pantryRepository = new PantryRepository(getApplication());
        }
        return pantryRepository;
    }

    /** Tests only: adds a message as if it had arrived (no network). Main thread. */
    @VisibleForTesting
    void addMessageForTest(@NonNull ChatMessage message) {
        List<ChatMessage> snapshot;
        synchronized (conversation) {
            conversation.add(message);
            snapshot = new ArrayList<>(conversation);
        }
        messages.setValue(snapshot);
    }

    /** Background thread. */
    private void publish(@NonNull ChatMessage message) {
        List<ChatMessage> snapshot;
        synchronized (conversation) {
            conversation.add(message);
            snapshot = new ArrayList<>(conversation);
        }
        messages.postValue(snapshot);
    }

    /**
     * Converts the conversation into the backend's history format.
     * A user message that never got an answer (e.g. it failed) is skipped,
     * so the backend never receives two user turns in a row.
     */
    @NonNull
    static List<ChatMessageDto> toHistory(@NonNull List<ChatMessage> messages) {
        List<ChatMessageDto> history = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message.isFromUser()) {
                boolean answered = i + 1 < messages.size() && !messages.get(i + 1).isFromUser();
                if (!answered) continue;
                history.add(new ChatMessageDto(ChatMessageDto.ROLE_USER, message.text));
            } else {
                history.add(new ChatMessageDto(ChatMessageDto.ROLE_ASSISTANT, message.text));
            }
        }
        return history;
    }
}