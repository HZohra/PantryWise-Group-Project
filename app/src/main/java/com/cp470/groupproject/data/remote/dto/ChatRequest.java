package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** Body of POST /api/v1/ai/chat */
public class ChatRequest {
    public String message;
    public List<ChatMessageDto> history;
    public List<InventoryItemDto> inventory;
    /** App language tag, e.g. "es-MX". The backend replies in Spanish for "es…", English otherwise. */
    @Nullable public String locale;
    /** Saved user preferences; null = not sent. */
    @Nullable public PreferencesDto preferences;

    public ChatRequest(String message, List<ChatMessageDto> history, List<InventoryItemDto> inventory) {
        this(message, history, inventory, null, null);
    }

    public ChatRequest(String message, List<ChatMessageDto> history, List<InventoryItemDto> inventory,
                       @Nullable String locale, @Nullable PreferencesDto preferences) {
        this.message = message;
        this.history = history;
        this.inventory = inventory;
        this.locale = locale;
        this.preferences = preferences;
    }
}