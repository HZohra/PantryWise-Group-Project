package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.ChatShoppingItemDto;

import org.junit.Test;

import java.util.Arrays;

/** Proposed shopping-list items on an assistant message. */
public class ChatMessageItemsTest {

    @Test
    public void normalMessages_haveNoItems() {
        assertEquals(ChatMessage.ItemsState.NONE, ChatMessage.fromUser("hi").itemsState);
        assertEquals(ChatMessage.ItemsState.NONE, ChatMessage.fromAi("hello").itemsState);
        assertEquals(ChatMessage.ItemsState.NONE, ChatMessage.fromAi("hello", null).itemsState);
    }

    @Test
    public void proposedItems_arePending_andBlankNamesAreDropped() {
        ChatMessage message = ChatMessage.fromAi("Confirm below.", Arrays.asList(
                new ChatShoppingItemDto(" garlic ", "2 cloves"),
                new ChatShoppingItemDto("  ", "1"),
                new ChatShoppingItemDto("olive oil", null)));

        assertEquals(ChatMessage.ItemsState.PENDING, message.itemsState);
        assertEquals(2, message.items.size());
        assertEquals("garlic (2 cloves), olive oil", message.itemsText());
    }

    @Test
    public void changingTheState_keepsTheIdAndCountsAsAChange() {
        ChatMessage pending = ChatMessage.fromAi("Confirm below.",
                Arrays.asList(new ChatShoppingItemDto("garlic", "")));

        ChatMessage added = pending.withItemsState(ChatMessage.ItemsState.ADDED);

        assertEquals(pending.id, added.id);
        assertEquals(pending.text, added.text);
        assertFalse(pending.hasSameContentAs(added));
        assertTrue(added.hasSameContentAs(pending.withItemsState(ChatMessage.ItemsState.ADDED)));
    }
}