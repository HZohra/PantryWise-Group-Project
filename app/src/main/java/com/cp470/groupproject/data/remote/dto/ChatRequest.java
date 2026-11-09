package com.cp470.groupproject.data.remote.dto;

import java.util.List;

/** Body of POST /api/v1/ai/chat */
public class ChatRequest {
    public String message;
    public List<ChatMessageDto> history;
    public List<InventoryItemDto> inventory;

    public ChatRequest(String message, List<ChatMessageDto> history, List<InventoryItemDto> inventory) {
        this.message = message;
        this.history = history;
        this.inventory = inventory;
    }
}