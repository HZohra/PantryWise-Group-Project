package com.cp470.groupproject.ui.ai;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicLong;

/**
 * One bubble in the AI chat (UI model, immutable).
 * Each message gets a unique {@link #id} so DiffUtil can tell two messages apart
 * even when their text is identical.
 */
public final class ChatMessage {

    /** Who wrote the message. */
    public enum Sender { USER, AI }

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    public final long id;
    @NonNull public final Sender sender;
    @NonNull public final String text;

    private ChatMessage(long id, @NonNull Sender sender, @NonNull String text) {
        this.id = id;
        this.sender = sender;
        this.text = text;
    }

    /** A message typed by the user. */
    @NonNull
    public static ChatMessage fromUser(@NonNull String text) {
        return new ChatMessage(NEXT_ID.getAndIncrement(), Sender.USER, text);
    }

    /** A reply from the assistant. */
    @NonNull
    public static ChatMessage fromAi(@NonNull String text) {
        return new ChatMessage(NEXT_ID.getAndIncrement(), Sender.AI, text);
    }

    public boolean isFromUser() {
        return sender == Sender.USER;
    }

    /** True when both messages would look identical on screen. */
    public boolean hasSameContentAs(@NonNull ChatMessage other) {
        return sender == other.sender && text.equals(other.text);
    }
}