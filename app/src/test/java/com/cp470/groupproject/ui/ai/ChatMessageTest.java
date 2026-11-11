package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatMessageTest {

    @Test
    public void factories_setSenderAndText() {
        ChatMessage user = ChatMessage.fromUser("Hi");
        ChatMessage ai = ChatMessage.fromAi("Hello!");

        assertTrue(user.isFromUser());
        assertEquals("Hi", user.text);
        assertFalse(ai.isFromUser());
        assertEquals(ChatMessage.Sender.AI, ai.sender);
    }

    @Test
    public void sameText_stillGetsDifferentIds() {
        ChatMessage first = ChatMessage.fromUser("ok");
        ChatMessage second = ChatMessage.fromUser("ok");

        assertNotEquals(first.id, second.id);
        assertFalse(ChatMessageAdapter.DIFF_CALLBACK.areItemsTheSame(first, second));
        assertTrue(ChatMessageAdapter.DIFF_CALLBACK.areContentsTheSame(first, second));
    }

    @Test
    public void diffCallback_sameMessage_isSameItemAndContent() {
        ChatMessage message = ChatMessage.fromAi("Try an omelette.");

        assertTrue(ChatMessageAdapter.DIFF_CALLBACK.areItemsTheSame(message, message));
        assertTrue(ChatMessageAdapter.DIFF_CALLBACK.areContentsTheSame(message, message));
    }
}