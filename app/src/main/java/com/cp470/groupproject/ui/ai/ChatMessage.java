package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cp470.groupproject.data.remote.dto.ChatShoppingItemDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One bubble in the AI chat (UI model, immutable).
 * Each message gets a unique {@link #id} so DiffUtil can tell two messages apart
 * even when their text is identical. An assistant reply can carry items proposed for the
 * shopping list; the user adds or dismisses them with the card under the bubble.
 */
public final class ChatMessage {

    /** Who wrote the message. */
    public enum Sender { USER, AI }

    /** What happened to the proposed shopping-list items. */
    public enum ItemsState { NONE, PENDING, ADDED, DISMISSED }

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    public final long id;
    @NonNull public final Sender sender;
    @NonNull public final String text;
    /** Items the assistant proposed (read-only); empty for normal messages. */
    @NonNull public final List<ChatShoppingItemDto> items;
    @NonNull public final ItemsState itemsState;

    private ChatMessage(long id, @NonNull Sender sender, @NonNull String text,
                        @NonNull List<ChatShoppingItemDto> items, @NonNull ItemsState itemsState) {
        this.id = id;
        this.sender = sender;
        this.text = text;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.itemsState = itemsState;
    }

    /** A message typed by the user. */
    @NonNull
    public static ChatMessage fromUser(@NonNull String text) {
        return new ChatMessage(NEXT_ID.getAndIncrement(), Sender.USER, text,
                Collections.emptyList(), ItemsState.NONE);
    }

    /** A reply from the assistant. */
    @NonNull
    public static ChatMessage fromAi(@NonNull String text) {
        return fromAi(text, null);
    }

    /** A reply that may propose shopping-list items (blank names are dropped). */
    @NonNull
    public static ChatMessage fromAi(@NonNull String text, @Nullable List<ChatShoppingItemDto> proposed) {
        List<ChatShoppingItemDto> clean = new ArrayList<>();
        if (proposed != null) {
            for (ChatShoppingItemDto item : proposed) {
                if (item != null && item.name != null && !item.name.trim().isEmpty()) {
                    clean.add(new ChatShoppingItemDto(item.name.trim(),
                            item.amount == null ? "" : item.amount.trim()));
                }
            }
        }
        return new ChatMessage(NEXT_ID.getAndIncrement(), Sender.AI, text, clean,
                clean.isEmpty() ? ItemsState.NONE : ItemsState.PENDING);
    }

    /** The same message (same id) with the items in another state. */
    @NonNull
    public ChatMessage withItemsState(@NonNull ItemsState state) {
        return new ChatMessage(id, sender, text, items, state);
    }

    public boolean isFromUser() {
        return sender == Sender.USER;
    }

    /** "garlic (2 cloves), olive oil" */
    @NonNull
    public String itemsText() {
        StringBuilder out = new StringBuilder();
        for (ChatShoppingItemDto item : items) {
            if (out.length() > 0) out.append(", ");
            out.append(item.name);
            if (!item.amount.isEmpty()) out.append(" (").append(item.amount).append(')');
        }
        return out.toString();
    }

    /** True when both messages would look identical on screen. */
    public boolean hasSameContentAs(@NonNull ChatMessage other) {
        return sender == other.sender && text.equals(other.text)
                && itemsState == other.itemsState && items.size() == other.items.size();
    }
}