package com.cp470.groupproject.data.remote.dto;

import androidx.annotation.Nullable;

import java.util.List;

/** Body of a successful POST /api/v1/ai/chat: {"reply":"...","shoppingItems":[...]} */
public class ChatResponse {
    public String reply;
    /** Items the assistant proposes for the shopping list; empty (or missing) for a normal answer. */
    @Nullable public List<ChatShoppingItemDto> shoppingItems;
}