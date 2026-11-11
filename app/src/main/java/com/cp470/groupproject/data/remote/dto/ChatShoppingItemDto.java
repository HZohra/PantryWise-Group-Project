package com.cp470.groupproject.data.remote.dto;

/** One item the assistant proposes for the shopping list: {"name":"garlic","amount":"2 cloves"}. */
public class ChatShoppingItemDto {
    /** Lowercase English food name, already checked against the user's allergies by the backend. */
    public String name;
    /** Short amount such as "2 cloves", or "". */
    public String amount;

    public ChatShoppingItemDto() { }

    public ChatShoppingItemDto(String name, String amount) {
        this.name = name;
        this.amount = amount;
    }
}